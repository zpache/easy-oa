package cn.easykits.oa.modules.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日程/待办新增编辑表单。
 *
 * @param title         标题
 * @param type          1 日程 2 待办
 * @param scope         personal 私人 / dept 部门共享
 * @param startTime     开始时间
 * @param endTime       结束时间
 * @param allDay        1 全天
 * @param repeatType    none / daily / weekly / monthly
 * @param repeatUntil   重复截止日期
 * @param remindMinutes 提前提醒分钟数
 * @param location      地点
 * @param color         日历展示色
 * @param remark        备注
 */
public record EventForm(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题最长 100 字")
        String title,
        @NotNull(message = "类型不能为空")
        Integer type,
        String scope,
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,
        Integer allDay,
        String repeatType,
        LocalDate repeatUntil,
        Integer remindMinutes,
        String location,
        String color,
        String remark) {
}
