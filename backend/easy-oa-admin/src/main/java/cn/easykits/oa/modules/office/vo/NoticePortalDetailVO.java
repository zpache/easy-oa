package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 公告中心详情：正文 + 已读数。
 */
public record NoticePortalDetailVO(
        Long id,
        String title,
        String category,
        String content,
        LocalDateTime publishAt,
        LocalDateTime expireAt,
        String publisher,
        Long readCount
) {
}
