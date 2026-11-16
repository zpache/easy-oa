package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 员工班次绑定表单：shiftId 为空表示解除绑定（回落到默认班次）。
 */
public record BindForm(
        @NotNull(message = "员工不能为空")
        Long userId,

        Long shiftId) {
}
