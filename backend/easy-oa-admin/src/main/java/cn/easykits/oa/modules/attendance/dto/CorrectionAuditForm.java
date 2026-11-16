package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 补卡审批表单。
 */
public record CorrectionAuditForm(
        @NotNull(message = "审批结果不能为空")
        @Min(1) @Max(2)
        Integer status,

        @Size(max = 200, message = "审批备注过长")
        String remark) {
}
