package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 公告已读明细行。
 */
public record NoticeReadVO(
        Long userId,
        String nickname,
        String deptName,
        LocalDateTime readAt
) {
}
