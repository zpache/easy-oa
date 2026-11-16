package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档目录（知识库目录树）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_folder")
public class DocFolder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父目录，0 根 */
    private Long pid;

    private String name;

    private Integer sort;

    private String description;
}
