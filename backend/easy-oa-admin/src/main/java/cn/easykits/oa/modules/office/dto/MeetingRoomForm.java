package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 会议室新增 / 编辑表单。
 */
public record MeetingRoomForm(

        @NotBlank(message = "会议室名称不能为空")
        @Size(max = 50, message = "会议室名称不能超过 50 个字符")
        String name,

        @Size(max = 100, message = "位置不能超过 100 个字符")
        String location,

        @Min(value = 1, message = "容纳人数至少为 1")
        Integer capacity,

        @Size(max = 255, message = "设备说明不能超过 255 个字符")
        String equipment,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
