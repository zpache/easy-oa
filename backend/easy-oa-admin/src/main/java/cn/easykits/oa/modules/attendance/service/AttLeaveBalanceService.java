package cn.easykits.oa.modules.attendance.service;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.AttLeaveBalance;
import cn.easykits.oa.entity.AttLeaveType;
import cn.easykits.oa.entity.SysDept;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.AttLeaveBalanceMapper;
import cn.easykits.oa.mapper.AttLeaveTypeMapper;
import cn.easykits.oa.mapper.SysDeptMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.attendance.dto.BalanceAdjustForm;
import cn.easykits.oa.modules.attendance.vo.LeaveBalanceVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工假期余额：管理端设定总额度，员工端查看本人余额。
 * 已用额度当前由管理员登记；后续请假经 easy-bpm 审批通过后自动核销。
 */
@Service
@RequiredArgsConstructor
public class AttLeaveBalanceService extends ServiceImpl<AttLeaveBalanceMapper, AttLeaveBalance> {

    private final AttLeaveTypeMapper leaveTypeMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /**
     * 我的余额：仅返回启用类型的记录。
     */
    public List<LeaveBalanceVO> myList(Long userId) {
        List<AttLeaveBalance> balances = list(Wrappers.<AttLeaveBalance>lambdaQuery()
                .eq(AttLeaveBalance::getUserId, userId));
        List<Long> typeIds = balances.stream().map(AttLeaveBalance::getTypeId).distinct().toList();
        Map<Long, AttLeaveType> types = typeIds.isEmpty() ? Map.of()
                : leaveTypeMapper.selectByIds(typeIds).stream()
                .filter(t -> Integer.valueOf(1).equals(t.getStatus()))
                .collect(Collectors.toMap(AttLeaveType::getId, Function.identity()));
        SysUser user = sysUserMapper.selectById(userId);
        return balances.stream()
                .filter(b -> types.containsKey(b.getTypeId()))
                .map(b -> toVO(b, types.get(b.getTypeId()), user, Map.of()))
                .toList();
    }

    /**
     * 管理端余额列表：可按员工、类型过滤。
     */
    public PageResult<LeaveBalanceVO> adminPage(long pageNum, long pageSize, Long userId, Long typeId) {
        Page<AttLeaveBalance> page = page(Page.of(pageNum, pageSize),
                Wrappers.<AttLeaveBalance>lambdaQuery()
                        .eq(userId != null, AttLeaveBalance::getUserId, userId)
                        .eq(typeId != null, AttLeaveBalance::getTypeId, typeId)
                        .orderByAsc(AttLeaveBalance::getUserId)
                        .orderByAsc(AttLeaveBalance::getTypeId));
        List<Long> userIds = page.getRecords().stream()
                .map(AttLeaveBalance::getUserId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, String> deptNames = users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList().isEmpty()
                ? Map.of()
                : sysDeptMapper.selectByIds(users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        Map<Long, AttLeaveType> types = page.getRecords().isEmpty() ? Map.of()
                : leaveTypeMapper.selectByIds(page.getRecords().stream()
                .map(AttLeaveBalance::getTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(AttLeaveType::getId, Function.identity()));
        return PageResult.of(page, b ->
                toVO(b, types.get(b.getTypeId()), users.get(b.getUserId()), deptNames));
    }

    /**
     * 设定员工某假期类型的总额度：不存在则按类型默认额度初始化已用为 0，存在则覆盖总额度。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveBalanceVO adjust(BalanceAdjustForm form) {
        AttLeaveType type = leaveTypeMapper.selectById(form.typeId());
        if (type == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "假期类型不存在");
        }
        AttLeaveBalance balance = getOne(Wrappers.<AttLeaveBalance>lambdaQuery()
                .eq(AttLeaveBalance::getUserId, form.userId())
                .eq(AttLeaveBalance::getTypeId, form.typeId())
                .last("limit 1"));
        if (balance == null) {
            balance = new AttLeaveBalance();
            balance.setUserId(form.userId());
            balance.setTypeId(form.typeId());
            balance.setUsedQuota(java.math.BigDecimal.ZERO);
        }
        if (balance.getUsedQuota() != null
                && balance.getUsedQuota().compareTo(form.totalQuota()) > 0) {
            throw new BusinessException("总额度不能小于已用额度（"
                    + balance.getUsedQuota() + " 天）");
        }
        balance.setTotalQuota(form.totalQuota());
        saveOrUpdate(balance);
        return toVO(balance, type, sysUserMapper.selectById(form.userId()), Map.of());
    }

    private LeaveBalanceVO toVO(AttLeaveBalance b, AttLeaveType type, SysUser user,
                                Map<Long, String> deptNames) {
        java.math.BigDecimal remain = b.getTotalQuota() == null ? java.math.BigDecimal.ZERO
                : b.getTotalQuota().subtract(
                b.getUsedQuota() == null ? java.math.BigDecimal.ZERO : b.getUsedQuota());
        return new LeaveBalanceVO(b.getUserId(),
                user != null ? user.getNickname() : null,
                user != null ? deptNames.get(user.getDeptId()) : null,
                b.getTypeId(), type != null ? type.getName() : null,
                b.getTotalQuota(), b.getUsedQuota(), remain);
    }
}
