package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 会议室预约表单。
 */
public record MeetingBookingForm(

        @NotNull(message = "会议室不能为空")
        Long roomId,

        @NotBlank(message = "会议主题不能为空")
        @Size(max = 100, message = "会议主题不能超过 100 个字符")
        String title,

        @Size(max = 255, message = "参与人说明不能超过 255 个字符")
        String participants,

        @NotNull(message = "开始时间不能为空")
        @Future(message = "开始时间必须晚于当前时间")
        LocalDateTime startAt,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endAt
) {
}
