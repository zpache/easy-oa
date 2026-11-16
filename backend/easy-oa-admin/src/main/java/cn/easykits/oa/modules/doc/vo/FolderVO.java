package cn.easykits.oa.modules.doc.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 目录树节点视图对象。
 */
public record FolderVO(
        Long id,
        Long pid,
        String name,
        Integer sort,
        String description,
        int docCount,
        List<FolderVO> children) {
}
