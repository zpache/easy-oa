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
        LocalDateTime createdAt,
        /**
         * 展示状态（由时间推导，不落库）：
         * cancelled 已取消 / ended 已结束 / ongoing 进行中 / upcoming 待开始（已预约）。
         */
        String state
) {
}
