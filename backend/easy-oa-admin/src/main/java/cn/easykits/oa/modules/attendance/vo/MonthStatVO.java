package cn.easykits.oa.modules.attendance.vo;

import java.time.LocalDate;
import java.util.Map;

/**
 * 我的月度考勤统计：日历渲染用。
 */
public record MonthStatVO(
        String yearMonth,
        Map<LocalDate, DayStat> days,
        int lateCount,
        int earlyCount,
        int normalCount,
        int missingCount) {

    /**
     * 单日状态：clockInStatus / clockOutStatus（null 表示当日无打卡记录）。
     */
    public record DayStat(Integer clockInStatus, Integer clockOutStatus) {
    }
}
