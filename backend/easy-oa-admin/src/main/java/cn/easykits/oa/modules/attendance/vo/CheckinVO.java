package cn.easykits.oa.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 打卡记录视图对象（我的记录 / 管理端全员记录共用）。
 */
public record CheckinVO(
        Long id,
        Long userId,
        String userName,
        String deptName,
        LocalDate workDate,
        Long shiftId,
        String shiftName,
        LocalTime clockIn,
        Integer clockInStatus,
        LocalTime clockOut,
        Integer clockOutStatus) {
}
