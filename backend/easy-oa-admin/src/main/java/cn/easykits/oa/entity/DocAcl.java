package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 目录阅读权限：user 指定人 / role 角色（存角色 ID）/ dept 部门。
 * 目录未配置 ACL 时对所有人可见；配置后仅命中主体可见，且子目录继承父目录限制。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_acl")
public class DocAcl extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long folderId;

    /** user / role / dept */
    private String subjectType;

    private Long subjectId;
}
