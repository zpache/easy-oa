package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 会议室预约单 VO。
 */
public record MeetingBookingVO(
        Long id,
        Long roomId,
        String roomName,
        String title,
        Long userId,
        String userName,
        String participants,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer status,
        LocalDateTime createdAt
) {
}
