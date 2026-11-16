package cn.easykits.oa.modules.attendance.vo;

import cn.easykits.oa.entity.AttShift;

import java.time.LocalTime;

/**
 * 班次视图对象。
 */
public record ShiftVO(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        Integer flexibleMinutes,
        Integer isDefault,
        Integer status,
        Integer boundCount) {

    public static ShiftVO of(AttShift shift, Integer boundCount) {
        return new ShiftVO(shift.getId(), shift.getName(), shift.getStartTime(),
                shift.getEndTime(), shift.getFlexibleMinutes(), shift.getIsDefault(),
                shift.getStatus(), boundCount);
    }
}
