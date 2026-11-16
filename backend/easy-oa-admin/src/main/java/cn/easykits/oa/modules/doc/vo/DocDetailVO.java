package cn.easykits.oa.modules.doc.vo;

import java.time.LocalDateTime;

/**
 * 文档详情视图对象（含正文）。
 */
public record DocDetailVO(
        Long id,
        Long folderId,
        String folderName,
        String title,
        String content,
        Integer status,
        String creatorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
