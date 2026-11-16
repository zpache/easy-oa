package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告管理列表 / 详情 VO。
 */
public record NoticeVO(
        Long id,
        String title,
        String category,
        String content,
        String scopeType,
        List<Long> deptIds,
        Integer status,
        Integer top,
        LocalDateTime publishAt,
        LocalDateTime expireAt,
        String publisher,
        Long readCount,
        LocalDateTime createdAt
) {
}
