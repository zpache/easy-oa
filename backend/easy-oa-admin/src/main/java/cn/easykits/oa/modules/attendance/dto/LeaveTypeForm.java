package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 假期类型新增/编辑表单。
 */
public record LeaveTypeForm(
        @NotBlank(message = "假期名称不能为空")
        @Size(max = 30, message = "假期名称过长")
        String name,

        @Size(max = 100, message = "说明过长")
        String remark,

        @NotNull(message = "默认额度不能为空")
        @DecimalMin(value = "0", message = "默认额度不能为负")
        BigDecimal defaultQuota,

        @Min(0) @Max(1)
        Integer status) {
}
