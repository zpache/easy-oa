package cn.easykits.oa.modules.doc.vo;

import java.time.LocalDateTime;

/**
 * 文档列表条目视图对象（不含正文，详情单独拉取）。
 */
public record DocVO(
        Long id,
        Long folderId,
        String folderName,
        String title,
        Integer status,
        String creatorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
