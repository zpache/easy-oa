package cn.easykits.oa.modules.attendance.vo;

import java.math.BigDecimal;

/**
 * 假期余额视图对象。
 */
public record LeaveBalanceVO(
        Long userId,
        String userName,
        String deptName,
        Long typeId,
        String typeName,
        BigDecimal totalQuota,
        BigDecimal usedQuota,
        BigDecimal remainQuota) {
}
