package cn.easykits.oa.modules.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文档新增编辑表单。
 */
public record DocForm(
        @NotNull(message = "所属目录不能为空")
        Long folderId,
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题最长 100 字")
        String title,
        @NotBlank(message = "正文不能为空")
        String content) {
}
