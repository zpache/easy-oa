package cn.easykits.oa.modules.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 补卡申请表单。
 */
public record CorrectionApplyForm(
        @NotNull(message = "补卡日期不能为空")
        LocalDate workDate,

        @NotNull(message = "请选择补上班卡还是下班卡")
        @Min(1) @Max(2)
        Integer type,

        @NotBlank(message = "申请原因不能为空")
        @Size(max = 200, message = "申请原因过长")
        String reason) {
}
