package cn.easykits.oa.modules.doc.service;

import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.context.LoginUser;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.DocAcl;
import cn.easykits.oa.entity.DocDocument;
import cn.easykits.oa.entity.DocFolder;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.entity.SysUserRole;
import cn.easykits.oa.mapper.DocAclMapper;
import cn.easykits.oa.mapper.DocDocumentMapper;
import cn.easykits.oa.mapper.DocFolderMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.mapper.SysUserRoleMapper;
import cn.easykits.oa.modules.doc.dto.AclForm;
import cn.easykits.oa.modules.doc.dto.FolderForm;
import cn.easykits.oa.modules.doc.vo.FolderVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文档目录树 + 阅读权限（ACL）。
 * <p>
 * 阅读权限模型：目录未配置 ACL = 全员可见；配置后仅 user/role/dept 命中主体可见，
 * 且 ACL 沿目录链向下继承（父目录不可见则子目录不可见）。管理员绕过 ACL。
 */
@Service
@RequiredArgsConstructor
public class DocFolderService extends ServiceImpl<DocFolderMapper, DocFolder> {

    private final DocAclMapper aclMapper;
    private final DocDocumentMapper documentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper userRoleMapper;

    /* ==================== 查询 ==================== */

    /**
     * 当前用户可见的目录树（已按 ACL 过滤，含各目录文档计数）。
     */
    public List<FolderVO> tree() {
        List<DocFolder> all = list(Wrappers.<DocFolder>lambdaQuery()
                .orderByAsc(DocFolder::getSort)
                .orderByAsc(DocFolder::getId));
        Set<Long> visible = new HashSet<>();
        for (DocFolder folder : all) {
            if (readable(folder, all)) {
                visible.add(folder.getId());
            }
        }
        // 父目录即使自身 ACL 不命中，子目录命中时也要保留父节点（树结构完整性）
        Set<Long> withAncestors = new HashSet<>(visible);
        Map<Long, DocFolder> byId = all.stream()
                .collect(java.util.stream.Collectors.toMap(DocFolder::getId, f -> f));
        for (Long id : visible) {
            DocFolder cur = byId.get(id);
            while (cur != null && cur.getPid() != null && cur.getPid() != 0) {
                withAncestors.add(cur.getPid());
                cur = byId.get(cur.getPid());
            }
        }

        Map<Long, Long> docCounts = new HashMap<>();
        for (DocDocument doc : documentMapper.selectList(Wrappers.<DocDocument>lambdaQuery()
                .eq(DocDocument::getStatus, 1))) {
            docCounts.merge(doc.getFolderId(), 1L, Long::sum);
        }

        Map<Long, List<FolderVO>> childrenMap = new HashMap<>();
        List<FolderVO> roots = new ArrayList<>();
        for (DocFolder folder : all) {
            if (!withAncestors.contains(folder.getId())) {
                continue;
            }
            childrenMap.computeIfAbsent(folder.getPid(), k -> new ArrayList<>())
                    .add(toVO(folder, docCounts.getOrDefault(folder.getId(), 0L).intValue()));
        }
        childrenMap.values().forEach(list ->
                list.sort(Comparator.comparing(FolderVO::sort).thenComparing(FolderVO::id)));
        for (FolderVO vo : childrenMap.getOrDefault(0L, List.of())) {
            roots.add(fillChildren(vo, childrenMap));
        }
        return roots;
    }

    /**
     * 读取某目录的 ACL 配置（管理端编辑用，需 doc:folder:acl 权限，控制器已校验）。
     */
    public List<AclForm.AclItem> listAcl(Long folderId) {
        requireById(folderId);
        return aclMapper.selectList(Wrappers.<DocAcl>lambdaQuery()
                        .eq(DocAcl::getFolderId, folderId)
                        .orderByAsc(DocAcl::getSubjectType)
                        .orderByAsc(DocAcl::getSubjectId)).stream()
                .map(acl -> new AclForm.AclItem(acl.getSubjectType(), acl.getSubjectId()))
                .toList();
    }

    /* ==================== 写操作 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public DocFolder create(FolderForm form) {
        if (form.pid() != null && form.pid() != 0) {
            requireById(form.pid());
        }
        DocFolder folder = new DocFolder();
        applyForm(folder, form);
        save(folder);
        return folder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FolderForm form) {
        DocFolder folder = requireById(id);
        if (form.pid() != null && form.pid() != 0) {
            requireById(form.pid());
            if (form.pid().equals(id) || isDescendant(form.pid(), id)) {
                throw new BusinessException("不能把目录移动到它自己或它的子目录下");
            }
        }
        applyForm(folder, form);
        if (!updateById(folder)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        Long childCount = count(Wrappers.<DocFolder>lambdaQuery().eq(DocFolder::getPid, id));
        if (childCount > 0) {
            throw new BusinessException("该目录下还有 " + childCount + " 个子目录，请先删除子目录");
        }
        Long docCount = documentMapper.selectCount(Wrappers.<DocDocument>lambdaQuery()
                .eq(DocDocument::getFolderId, id));
        if (docCount != null && docCount > 0) {
            throw new BusinessException("该目录下还有 " + docCount + " 篇文档，请先移走或删除文档");
        }
        aclMapper.delete(Wrappers.<DocAcl>lambdaQuery().eq(DocAcl::getFolderId, id));
        removeById(id);
    }

    /**
     * 整组替换目录 ACL；空数组 = 清空 = 全员可见。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setAcl(Long folderId, AclForm form) {
        requireById(folderId);
        Set<String> validTypes = Set.of("user", "role", "dept");
        for (AclForm.AclItem item : form.items()) {
            if (!validTypes.contains(item.subjectType()) || item.subjectId() == null) {
                throw new BusinessException("权限项不合法：subjectType 仅支持 user / role / dept");
            }
        }
        aclMapper.delete(Wrappers.<DocAcl>lambdaQuery().eq(DocAcl::getFolderId, folderId));
        for (AclForm.AclItem item : form.items().stream()
                .distinct()
                .toList()) {
            DocAcl acl = new DocAcl();
            acl.setFolderId(folderId);
            acl.setSubjectType(item.subjectType());
            acl.setSubjectId(item.subjectId());
            aclMapper.insert(acl);
        }
    }

    /* ==================== ACL 判定 ==================== */

    /**
     * 当前用户能否读某目录：管理员绕过；沿父链逐级校验（任一级不命中即不可见）。
     * 目录无 ACL 记录视为全员可见。
     */
    public boolean readable(DocFolder folder, List<DocFolder> allFolders) {
        UserSnapshot snapshot = snapshot();
        if (snapshot.admin()) {
            return true;
        }
        Map<Long, DocFolder> byId = allFolders.stream()
                .collect(java.util.stream.Collectors.toMap(DocFolder::getId, f -> f));
        DocFolder cur = folder;
        while (cur != null) {
            if (!aclMatch(cur.getId(), snapshot)) {
                return false;
            }
            cur = cur.getPid() == null || cur.getPid() == 0 ? null : byId.get(cur.getPid());
        }
        return true;
    }

    /**
     * 当前用户能否读某目录（单目录便捷重载）。
     */
    public boolean readable(Long folderId) {
        DocFolder folder = getById(folderId);
        if (folder == null) {
            return false;
        }
        List<DocFolder> all = list(Wrappers.emptyWrapper());
        return readable(folder, all);
    }

    private boolean aclMatch(Long folderId, UserSnapshot snapshot) {
        List<DocAcl> acls = aclMapper.selectList(Wrappers.<DocAcl>lambdaQuery()
                .eq(DocAcl::getFolderId, folderId));
        if (acls.isEmpty()) {
            return true;
        }
        for (DocAcl acl : acls) {
            switch (acl.getSubjectType()) {
                case "user" -> {
                    if (acl.getSubjectId().equals(snapshot.userId())) {
                        return true;
                    }
                }
                case "dept" -> {
                    if (snapshot.deptId() != null && acl.getSubjectId().equals(snapshot.deptId())) {
                        return true;
                    }
                }
                case "role" -> {
                    if (snapshot.roleIds().contains(acl.getSubjectId())) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }
        return false;
    }

    private record UserSnapshot(Long userId, Long deptId, Set<Long> roleIds, boolean admin) {
    }

    private UserSnapshot snapshot() {
        LoginUser user = UserContext.require();
        boolean admin = user.roles() != null && user.roles().contains("ADMIN");
        SysUser me = sysUserMapper.selectById(user.userId());
        Set<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, user.userId())).stream()
                .map(SysUserRole::getRoleId)
                .collect(java.util.stream.Collectors.toSet());
        return new UserSnapshot(user.userId(), me == null ? null : me.getDeptId(),
                roleIds, admin);
    }

    /* ==================== 私有方法 ==================== */

    private FolderVO fillChildren(FolderVO node, Map<Long, List<FolderVO>> childrenMap) {
        List<FolderVO> children = childrenMap.getOrDefault(node.id(), List.of());
        return new FolderVO(node.id(), node.pid(), node.name(), node.sort(),
                node.description(), node.docCount(),
                children.stream().map(c -> fillChildren(c, childrenMap)).toList());
    }

    private FolderVO toVO(DocFolder folder, int docCount) {
        return new FolderVO(folder.getId(), folder.getPid(), folder.getName(),
                folder.getSort(), folder.getDescription(), docCount, List.of());
    }

    private DocFolder requireById(Long id) {
        DocFolder folder = getById(id);
        if (folder == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "目录不存在");
        }
        return folder;
    }

    /**
     * 判断 target 是否为 ancestorId 的子孙（含相等的判断由调用方处理）。
     */
    private boolean isDescendant(Long target, Long ancestorId) {
        DocFolder cur = getById(target);
        int depth = 0;
        while (cur != null && cur.getPid() != null && cur.getPid() != 0 && depth < 50) {
            if (cur.getPid().equals(ancestorId)) {
                return true;
            }
            cur = getById(cur.getPid());
            depth++;
        }
        return false;
    }

    private void applyForm(DocFolder folder, FolderForm form) {
        folder.setPid(form.pid() == null ? 0 : form.pid());
        folder.setName(form.name().trim());
        folder.setSort(form.sort() == null ? 0 : form.sort());
        folder.setDescription(StringUtils.hasText(form.description())
                ? form.description() : null);
    }
}
