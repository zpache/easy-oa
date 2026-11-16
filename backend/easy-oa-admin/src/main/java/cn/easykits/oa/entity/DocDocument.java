package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档：所属目录 + 富文本正文。
 * 状态机：0 草稿 → 1 已发布；草稿仅创建人与管理员可见。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_document")
public class DocDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long folderId;

    private String title;

    /** 正文，富文本 HTML */
    private String content;

    /** 0 草稿 1 已发布 */
    private Integer status;
}
