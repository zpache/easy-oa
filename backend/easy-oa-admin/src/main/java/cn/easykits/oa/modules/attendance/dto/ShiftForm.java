package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 班次新增/编辑表单。
 */
public record ShiftForm(
        @NotBlank(message = "班次名称不能为空")
        @Size(max = 30, message = "班次名称过长")
        String name,

        @NotBlank(message = "上班时间不能为空")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "上班时间格式应为 HH:mm")
        String startTime,

        @NotBlank(message = "下班时间不能为空")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "下班时间格式应为 HH:mm")
        String endTime,

        @Min(value = 0, message = "弹性分钟数不能为负")
        @Max(value = 240, message = "弹性分钟数过大")
        Integer flexibleMinutes,

        @Min(0) @Max(1)
        Integer isDefault,

        @Min(0) @Max(1)
        Integer status) {
}
