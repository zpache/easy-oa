package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 会议室 VO。
 */
public record MeetingRoomVO(
        Long id,
        String name,
        String location,
        Integer capacity,
        String equipment,
        Integer status,
        String remark
) {
}
