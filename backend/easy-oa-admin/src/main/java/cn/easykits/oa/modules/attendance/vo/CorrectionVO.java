package cn.easykits.oa.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请视图对象。
 */
public record CorrectionVO(
        Long id,
        Long userId,
        String userName,
        String deptName,
        LocalDate workDate,
        Integer type,
        String reason,
        Integer status,
        String approverName,
        String approveRemark,
        LocalDateTime approveAt,
        LocalDateTime createdAt) {
}
