package cn.easykits.oa.modules.office.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.OfficeNotice;
import cn.easykits.oa.entity.OfficeNoticeRead;
import cn.easykits.oa.entity.OfficeNoticeScope;
import cn.easykits.oa.entity.SysDept;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.OfficeNoticeMapper;
import cn.easykits.oa.mapper.OfficeNoticeReadMapper;
import cn.easykits.oa.mapper.OfficeNoticeScopeMapper;
import cn.easykits.oa.mapper.SysDeptMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.office.dto.NoticeForm;
import cn.easykits.oa.modules.office.vo.NoticePortalDetailVO;
import cn.easykits.oa.modules.office.vo.NoticePortalVO;
import cn.easykits.oa.modules.office.vo.NoticeReadVO;
import cn.easykits.oa.modules.office.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公告通知：管理端维护 + 员工端（公告中心）查看与已读统计。
 * 投放范围存 office_notice_scope 关联表，已读回执一人一条（唯一键）。
 */
@Service
@RequiredArgsConstructor
public class NoticeService extends ServiceImpl<OfficeNoticeMapper, OfficeNotice> {

    public static final Integer STATUS_DRAFT = 0;
    public static final Integer STATUS_PUBLISHED = 1;
    public static final Integer STATUS_WITHDRAWN = 2;

    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_DEPT = "dept";

    private final OfficeNoticeScopeMapper scopeMapper;
    private final OfficeNoticeReadMapper readMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /* ==================== 管理端 ==================== */

    public PageResult<NoticeVO> pageQuery(long pageNum, long pageSize,
                                          String keyword, Integer status, String category) {
        Page<OfficeNotice> page = page(Page.of(pageNum, pageSize),
                Wrappers.<OfficeNotice>lambdaQuery()
                        .like(StringUtils.hasText(keyword), OfficeNotice::getTitle, keyword)
                        .eq(status != null, OfficeNotice::getStatus, status)
                        .eq(StringUtils.hasText(category), OfficeNotice::getCategory, category)
                        .orderByDesc(OfficeNotice::getTop)
                        .orderByDesc(OfficeNotice::getId));
        return PageResult.of(page, this::toVO);
    }

    public NoticeVO detail(Long id) {
        OfficeNotice notice = getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在");
        }
        return toVO(notice);
    }

    @Transactional(rollbackFor = Exception.class)
    public OfficeNotice create(NoticeForm form, boolean publish) {
        validateScope(form);
        OfficeNotice notice = new OfficeNotice();
        applyForm(notice, form);
        notice.setStatus(publish ? STATUS_PUBLISHED : STATUS_DRAFT);
        notice.setPublishAt(publish ? LocalDateTime.now() : null);
        save(notice);
        saveScopes(notice.getId(), form);
        return notice;
    }

    @Transactional(rollbackFor = Exception.class)
    public OfficeNotice update(Long id, NoticeForm form) {
        OfficeNotice notice = getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在");
        }
        // 已发布的内容调整走「撤回 → 编辑 → 重新发布」，避免在线内容被静默篡改
        if (Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException("已发布的公告需先撤回再编辑");
        }
        validateScope(form);
        applyForm(notice, form);
        notice.setPublishAt(null);
        if (!updateById(notice)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        scopeMapper.delete(Wrappers.<OfficeNoticeScope>lambdaQuery()
                .eq(OfficeNoticeScope::getNoticeId, id));
        saveScopes(id, form);
        return notice;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        OfficeNotice notice = requireById(id);
        if (!Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            notice.setStatus(STATUS_PUBLISHED);
            notice.setPublishAt(LocalDateTime.now());
            if (!updateById(notice)) {
                throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        OfficeNotice notice = requireById(id);
        if (Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            notice.setStatus(STATUS_WITHDRAWN);
            if (!updateById(notice)) {
                throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
            }
        }
    }

    public void toggleTop(Long id) {
        OfficeNotice notice = requireById(id);
        notice.setTop(Objects.equals(notice.getTop(), 1) ? 0 : 1);
        if (!updateById(notice)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        removeById(id);
        scopeMapper.delete(Wrappers.<OfficeNoticeScope>lambdaQuery()
                .eq(OfficeNoticeScope::getNoticeId, id));
        readMapper.delete(Wrappers.<OfficeNoticeRead>lambdaQuery()
                .eq(OfficeNoticeRead::getNoticeId, id));
    }

    /**
     * 已读明细：已读人（姓名、部门）+ 时间。
     */
    public List<NoticeReadVO> readList(Long id) {
        requireById(id);
        List<OfficeNoticeRead> reads = readMapper.selectList(
                Wrappers.<OfficeNoticeRead>lambdaQuery()
                        .eq(OfficeNoticeRead::getNoticeId, id)
                        .orderByDesc(OfficeNoticeRead::getReadAt));
        if (reads.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = reads.stream().map(OfficeNoticeRead::getUserId).distinct().toList();
        Map<Long, SysUser> users = sysUserMapper.selectList(
                        Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds)).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, String> deptNames = sysDeptMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        return reads.stream().map(read -> {
            SysUser user = users.get(read.getUserId());
            return new NoticeReadVO(read.getUserId(),
                    user == null ? String.valueOf(read.getUserId()) : user.getNickname(),
                    user == null || user.getDeptId() == null ? null : deptNames.get(user.getDeptId()),
                    read.getReadAt());
        }).toList();
    }

    /**
     * 已读统计：已读数 / 范围内总人数。
     */
    public record ReadStat(long readCount, long totalUsers) {
    }

    public ReadStat readStat(Long id) {
        OfficeNotice notice = requireById(id);
        long readCount = readMapper.selectCount(Wrappers.<OfficeNoticeRead>lambdaQuery()
                .eq(OfficeNoticeRead::getNoticeId, id));
        return new ReadStat(readCount, countAudience(notice));
    }

    /* ==================== 员工端（公告中心） ==================== */

    /**
     * 当前用户可见的已发布公告：置顶优先，其次按发布时间倒序。
     */
    public PageResult<NoticePortalVO> portalPage(long pageNum, long pageSize, String category) {
        Long userId = requireUserId();
        SysUser user = sysUserMapper.selectById(userId);
        // 指定部门范围：先查该部门命中的公告 ID，再并入「全员」公告
        List<Long> deptNoticeIds = user == null || user.getDeptId() == null ? List.of()
                : scopeMapper.selectList(Wrappers.<OfficeNoticeScope>lambdaQuery()
                        .eq(OfficeNoticeScope::getDeptId, user.getDeptId())).stream()
                .map(OfficeNoticeScope::getNoticeId).distinct().toList();
        Page<OfficeNotice> page = page(Page.of(pageNum, pageSize),
                Wrappers.<OfficeNotice>lambdaQuery()
                        .eq(OfficeNotice::getStatus, STATUS_PUBLISHED)
                        .and(w -> w
                                .eq(OfficeNotice::getScopeType, SCOPE_ALL)
                                .or(w2 -> w2.eq(OfficeNotice::getScopeType, SCOPE_DEPT)
                                        .in(!deptNoticeIds.isEmpty(), OfficeNotice::getId, deptNoticeIds)))
                        .eq(StringUtils.hasText(category), OfficeNotice::getCategory, category)
                        .and(w -> w.isNull(OfficeNotice::getExpireAt)
                                .or(w2 -> w2.gt(OfficeNotice::getExpireAt, LocalDateTime.now())))
                        .orderByDesc(OfficeNotice::getTop)
                        .orderByDesc(OfficeNotice::getPublishAt));

        List<Long> noticeIds = page.getRecords().stream().map(OfficeNotice::getId).toList();
        Map<Long, Long> readNoticeIds = noticeIds.isEmpty() ? Map.of()
                : readMapper.selectList(Wrappers.<OfficeNoticeRead>lambdaQuery()
                        .eq(OfficeNoticeRead::getUserId, userId)
                        .in(OfficeNoticeRead::getNoticeId, noticeIds)).stream()
                .collect(Collectors.toMap(OfficeNoticeRead::getNoticeId, r -> r.getId()));
        return PageResult.of(page, notice -> new NoticePortalVO(
                notice.getId(), notice.getTitle(), notice.getCategory(), notice.getTop(),
                notice.getPublishAt(), notice.getExpireAt(), readNoticeIds.containsKey(notice.getId())));
    }

    /**
     * 员工端详情：校验可见范围，查看即落已读回执（幂等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public NoticePortalDetailVO portalDetail(Long id) {
        OfficeNotice notice = getById(id);
        if (notice == null || !Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在或已撤回");
        }
        SysUser user = sysUserMapper.selectById(requireUserId());
        if (!visibleTo(notice, user)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该公告不在你的可见范围内");
        }
        markRead(id, requireUserId());
        String publisher = notice.getCreatedBy() == null ? null
                : sysUserMapper.selectById(notice.getCreatedBy()).getNickname();
        return new NoticePortalDetailVO(notice.getId(), notice.getTitle(), notice.getCategory(),
                notice.getContent(), notice.getPublishAt(), notice.getExpireAt(), publisher,
                readMapper.selectCount(Wrappers.<OfficeNoticeRead>lambdaQuery()
                        .eq(OfficeNoticeRead::getNoticeId, id)));
    }

    /* ==================== 私有方法 ==================== */

    private OfficeNotice requireById(Long id) {
        OfficeNotice notice = getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "公告不存在");
        }
        return notice;
    }

    private Long requireUserId() {
        return cn.easykits.oa.common.context.UserContext.require().userId();
    }

    private void applyForm(OfficeNotice notice, NoticeForm form) {
        notice.setTitle(form.title());
        notice.setCategory(StringUtils.hasText(form.category()) ? form.category() : "notice");
        notice.setContent(form.content());
        notice.setScopeType(form.scopeType());
        notice.setTop(form.top() != null && form.top() == 1 ? 1 : 0);
        notice.setExpireAt(form.expireAt());
    }

    private void saveScopes(Long noticeId, NoticeForm form) {
        if (!SCOPE_DEPT.equals(form.scopeType()) || form.deptIds() == null) {
            return;
        }
        for (Long deptId : form.deptIds().stream().distinct().toList()) {
            OfficeNoticeScope scope = new OfficeNoticeScope();
            scope.setNoticeId(noticeId);
            scope.setDeptId(deptId);
            scopeMapper.insert(scope);
        }
    }

    private void validateScope(NoticeForm form) {
        if (!SCOPE_ALL.equals(form.scopeType()) && !SCOPE_DEPT.equals(form.scopeType())) {
            throw new BusinessException("投放范围不合法，仅支持 all / dept");
        }
        if (SCOPE_DEPT.equals(form.scopeType())
                && (form.deptIds() == null || form.deptIds().isEmpty())) {
            throw new BusinessException("指定部门范围时至少选择一个部门");
        }
    }

    /**
     * 范围内总人数：全员 = 全部启用员工；部门范围 = 这些部门下的启用员工数。
     */
    private long countAudience(OfficeNotice notice) {
        if (SCOPE_DEPT.equals(notice.getScopeType())) {
            List<Long> deptIds = scopeMapper.selectList(Wrappers.<OfficeNoticeScope>lambdaQuery()
                            .eq(OfficeNoticeScope::getNoticeId, notice.getId())).stream()
                    .map(OfficeNoticeScope::getDeptId).distinct().toList();
            if (deptIds.isEmpty()) {
                return 0;
            }
            return sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .in(SysUser::getDeptId, deptIds)
                    .eq(SysUser::getStatus, 1));
        }
        return sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getStatus, 1));
    }

    private boolean visibleTo(OfficeNotice notice, SysUser user) {
        if (SCOPE_ALL.equals(notice.getScopeType())) {
            return true;
        }
        if (user == null || user.getDeptId() == null) {
            return false;
        }
        return scopeMapper.selectCount(Wrappers.<OfficeNoticeScope>lambdaQuery()
                .eq(OfficeNoticeScope::getNoticeId, notice.getId())
                .eq(OfficeNoticeScope::getDeptId, user.getDeptId())) > 0;
    }

    private void markRead(Long noticeId, Long userId) {
        boolean exists = readMapper.selectCount(Wrappers.<OfficeNoticeRead>lambdaQuery()
                .eq(OfficeNoticeRead::getNoticeId, noticeId)
                .eq(OfficeNoticeRead::getUserId, userId)) > 0;
        if (exists) {
            return;
        }
        OfficeNoticeRead read = new OfficeNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadAt(LocalDateTime.now());
        readMapper.insert(read);
    }

    private NoticeVO toVO(OfficeNotice notice) {
        List<Long> deptIds = scopeMapper.selectList(Wrappers.<OfficeNoticeScope>lambdaQuery()
                        .eq(OfficeNoticeScope::getNoticeId, notice.getId())).stream()
                .map(OfficeNoticeScope::getDeptId).toList();
        String publisher = notice.getCreatedBy() == null ? null
                : sysUserMapper.selectById(notice.getCreatedBy()).getNickname();
        Long readCount = readMapper.selectCount(Wrappers.<OfficeNoticeRead>lambdaQuery()
                .eq(OfficeNoticeRead::getNoticeId, notice.getId()));
        return new NoticeVO(notice.getId(), notice.getTitle(), notice.getCategory(), notice.getContent(),
                notice.getScopeType(), deptIds, notice.getStatus(), notice.getTop(),
                notice.getPublishAt(), notice.getExpireAt(), publisher, readCount, notice.getCreatedAt());
    }
}
