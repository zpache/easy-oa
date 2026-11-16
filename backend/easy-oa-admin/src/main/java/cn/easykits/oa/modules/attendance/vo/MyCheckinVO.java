package cn.easykits.oa.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 我的今日考勤：用于打卡页首屏渲染。
 */
public record MyCheckinVO(
        LocalDate workDate,
        Long shiftId,
        String shiftName,
        LocalTime shiftStart,
        LocalTime shiftEnd,
        Integer flexibleMinutes,
        LocalTime clockIn,
        Integer clockInStatus,
        LocalTime clockOut,
        Integer clockOutStatus,
        boolean clockedIn,
        boolean clockedOut) {
}
