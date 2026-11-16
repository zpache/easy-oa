package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 公告中心（员工视角）列表项：不带正文，点开详情才记录已读。
 */
public record NoticePortalVO(
        Long id,
        String title,
        String category,
        Integer top,
        LocalDateTime publishAt,
        LocalDateTime expireAt,
        Boolean read
) {
}
