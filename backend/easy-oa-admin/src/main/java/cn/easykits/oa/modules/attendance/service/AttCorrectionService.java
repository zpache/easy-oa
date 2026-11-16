package cn.easykits.oa.modules.attendance.service;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.AttCorrection;
import cn.easykits.oa.entity.SysDept;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.AttCorrectionMapper;
import cn.easykits.oa.mapper.SysDeptMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.attendance.dto.CorrectionApplyForm;
import cn.easykits.oa.modules.attendance.dto.CorrectionAuditForm;
import cn.easykits.oa.modules.attendance.vo.CorrectionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.easykits.oa.modules.attendance.service.AttStatus.*;

/**
 * 补卡申请与审批。
 */
@Service
@RequiredArgsConstructor
public class AttCorrectionService extends ServiceImpl<AttCorrectionMapper, AttCorrection> {

    private final AttCheckinService checkinService;
    private final AttShiftService shiftService;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    @Transactional(rollbackFor = Exception.class)
    public AttCorrection apply(Long userId, CorrectionApplyForm form) {
        if (form.workDate().isAfter(LocalDate.now())) {
            throw new BusinessException("补卡日期不能是未来的日期");
        }
        Long dup = count(Wrappers.<AttCorrection>lambdaQuery()
                .eq(AttCorrection::getUserId, userId)
                .eq(AttCorrection::getWorkDate, form.workDate())
                .eq(AttCorrection::getType, form.type())
                .ne(AttCorrection::getStatus, AUDIT_REJECTED));
        if (dup > 0) {
            throw new BusinessException("该日期此类型的补卡申请已存在");
        }
        AttCorrection correction = new AttCorrection();
        correction.setUserId(userId);
        correction.setWorkDate(form.workDate());
        correction.setType(form.type());
        correction.setReason(form.reason());
        correction.setStatus(AUDIT_PENDING);
        save(correction);
        return correction;
    }

    public PageResult<CorrectionVO> myPage(Long userId, long pageNum, long pageSize, Integer status) {
        return toVOPage(page(Page.of(pageNum, pageSize), Wrappers.<AttCorrection>lambdaQuery()
                .eq(AttCorrection::getUserId, userId)
                .eq(status != null, AttCorrection::getStatus, status)
                .orderByDesc(AttCorrection::getWorkDate)
                .orderByDesc(AttCorrection::getType)));
    }

    /**
     * 管理端审批列表：可按申请人、状态过滤。
     */
    public PageResult<CorrectionVO> adminPage(long pageNum, long pageSize, Long userId,
                                              Integer status, String month) {
        return toVOPage(page(Page.of(pageNum, pageSize), Wrappers.<AttCorrection>lambdaQuery()
                .eq(userId != null, AttCorrection::getUserId, userId)
                .eq(status != null, AttCorrection::getStatus, status)
                .apply(month != null && !month.isBlank(),
                        "DATE_FORMAT(work_date, '%Y-%m') = {0}", month)
                .orderByAsc(AttCorrection::getStatus)
                .orderByDesc(AttCorrection::getWorkDate)));
    }

    /**
     * 审批：通过则回写打卡记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, CorrectionAuditForm form, Long approverId) {
        AttCorrection correction = getById(id);
        if (correction == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "补卡申请不存在");
        }
        if (!Integer.valueOf(AUDIT_PENDING).equals(correction.getStatus())) {
            throw new BusinessException("该申请已审批过，请勿重复操作");
        }
        correction.setStatus(form.status());
        correction.setApproverId(approverId);
        correction.setApproveRemark(form.remark());
        correction.setApproveAt(java.time.LocalDateTime.now());
        updateById(correction);
        if (Integer.valueOf(AUDIT_APPROVED).equals(form.status())) {
            checkinService.applyCorrection(correction.getUserId(), correction.getWorkDate(),
                    correction.getType(), shiftService.resolveShiftForUser(correction.getUserId()));
        }
    }

    // ------------------------------ 内部 ------------------------------

    private PageResult<CorrectionVO> toVOPage(Page<AttCorrection> page) {
        List<Long> userIds = page.getRecords().stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getUserId(), c.getApproverId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, String> deptNames = users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList().isEmpty()
                ? Map.of()
                : sysDeptMapper.selectByIds(users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        return PageResult.of(page, c -> {
            SysUser user = users.get(c.getUserId());
            SysUser approver = c.getApproverId() != null ? users.get(c.getApproverId()) : null;
            return new CorrectionVO(c.getId(), c.getUserId(),
                    user != null ? user.getNickname() : null,
                    user != null ? deptNames.get(user.getDeptId()) : null,
                    c.getWorkDate(), c.getType(), c.getReason(), c.getStatus(),
                    approver != null ? approver.getNickname() : null,
                    c.getApproveRemark(), c.getApproveAt(), c.getCreatedAt());
        });
    }
}
