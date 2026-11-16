package cn.easykits.oa.modules.doc.service;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.DocDocument;
import cn.easykits.oa.entity.DocFolder;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.DocDocumentMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.doc.dto.DocForm;
import cn.easykits.oa.modules.doc.vo.DocDetailVO;
import cn.easykits.oa.modules.doc.vo.DocVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文档：目录内列表 / 富文本编辑 / 发布 / 标题与正文关键字检索。
 * <p>
 * 可见性：目录 ACL 由 DocFolderService 判定；草稿仅创建人与管理员可见。
 * 全文检索用 LIKE 命中标题或正文（Phase 5 轻量实现，量级上来后再接搜索引擎）。
 */
@Service
@RequiredArgsConstructor
public class DocDocumentService extends ServiceImpl<DocDocumentMapper, DocDocument> {

    public static final Integer STATUS_DRAFT = 0;
    public static final Integer STATUS_PUBLISHED = 1;

    private final DocFolderService folderService;
    private final SysUserMapper sysUserMapper;

    /* ==================== 查询 ==================== */

    public PageResult<DocVO> page(long pageNum, long pageSize,
                                  Long folderId, String keyword, Integer status) {
        if (folderId != null && !folderService.readable(folderId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该目录不在你的可见范围内");
        }
        IPage<DocDocument> page = page(Page.of(pageNum, pageSize),
                Wrappers.<DocDocument>lambdaQuery()
                        .eq(folderId != null, DocDocument::getFolderId, folderId)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(DocDocument::getTitle, keyword)
                                .or()
                                .like(DocDocument::getContent, keyword))
                        // 草稿过滤：非创建人且非管理员看不到草稿
                        .and(!isPrivileged(), w -> w
                                .eq(DocDocument::getStatus, STATUS_PUBLISHED)
                                .or(w2 -> w2.eq(DocDocument::getCreatedBy, UserContext.userId())))
                        .eq(status != null, DocDocument::getStatus, status)
                        .orderByDesc(DocDocument::getUpdatedAt));

        Map<Long, String> folderNames = folderService.list(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(DocFolder::getId, DocFolder::getName, (a, b) -> a));
        return PageResult.of(page, doc -> toVO(doc, folderNames));
    }

    public DocDetailVO detail(Long id) {
        DocDocument doc = requireById(id);
        if (!folderService.readable(doc.getFolderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该文档不在你的可见范围内");
        }
        if (Objects.equals(doc.getStatus(), STATUS_DRAFT) && !isPrivileged()
                && !Objects.equals(doc.getCreatedBy(), UserContext.userId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "草稿仅创建人可见");
        }
        return toDetailVO(doc);
    }

    /* ==================== 写操作 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public DocDocument create(DocForm form, boolean publish) {
        if (!folderService.readable(form.folderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该目录不在你的可见范围内");
        }
        DocDocument doc = new DocDocument();
        applyForm(doc, form);
        doc.setStatus(publish ? STATUS_PUBLISHED : STATUS_DRAFT);
        save(doc);
        return doc;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DocForm form) {
        DocDocument doc = requireById(id);
        if (!isPrivileged() && !Objects.equals(doc.getCreatedBy(), UserContext.userId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己创建的文档");
        }
        if (!folderService.readable(form.folderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "目标目录不在你的可见范围内");
        }
        applyForm(doc, form);
        if (!updateById(doc)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        DocDocument doc = requireById(id);
        if (!Objects.equals(doc.getStatus(), STATUS_PUBLISHED)) {
            doc.setStatus(STATUS_PUBLISHED);
            if (!updateById(doc)) {
                throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DocDocument doc = requireById(id);
        if (!isPrivileged() && !Objects.equals(doc.getCreatedBy(), UserContext.userId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己创建的文档");
        }
        removeById(id);
    }

    /* ==================== 私有方法 ==================== */

    private DocDocument requireById(Long id) {
        DocDocument doc = getById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在");
        }
        return doc;
    }

    /** 管理员可跨人管理文档 */
    private boolean isPrivileged() {
        return UserContext.require().roles() != null
                && UserContext.require().roles().contains("ADMIN");
    }

    private void applyForm(DocDocument doc, DocForm form) {
        doc.setFolderId(form.folderId());
        doc.setTitle(form.title().trim());
        doc.setContent(form.content());
    }

    private DocVO toVO(DocDocument doc, Map<Long, String> folderNames) {
        return new DocVO(doc.getId(), doc.getFolderId(),
                folderNames.get(doc.getFolderId()), doc.getTitle(), doc.getStatus(),
                creatorName(doc.getCreatedBy()), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    private DocDetailVO toDetailVO(DocDocument doc) {
        DocFolder folder = folderService.getById(doc.getFolderId());
        return new DocDetailVO(doc.getId(), doc.getFolderId(),
                folder == null ? null : folder.getName(),
                doc.getTitle(), doc.getContent(), doc.getStatus(),
                creatorName(doc.getCreatedBy()), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    private String creatorName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }
}
