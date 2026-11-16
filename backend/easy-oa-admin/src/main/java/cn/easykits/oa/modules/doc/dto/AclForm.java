package cn.easykits.oa.modules.doc.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 目录阅读权限配置：整组替换提交。
 *
 * @param items 权限项列表（空列表 = 清空 ACL，即全员可见）
 */
public record AclForm(@NotEmpty(message = "权限项不能为空，清空请传空数组") List<AclItem> items) {

    /**
     * @param subjectType user 指定人 / role 角色 / dept 部门
     * @param subjectId   主体 ID（角色存角色 ID）
     */
    public record AclItem(String subjectType, Long subjectId) {
    }
}
