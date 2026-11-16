package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 假期余额调整表单：直接设定总额度（天）。
 */
public record BalanceAdjustForm(
        @NotNull(message = "员工不能为空")
        Long userId,

        @NotNull(message = "假期类型不能为空")
        Long typeId,

        @NotNull(message = "总额度不能为空")
        @DecimalMin(value = "0", message = "总额度不能为负")
        BigDecimal totalQuota) {
}
