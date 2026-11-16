package cn.easykits.oa.modules.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 目录新增编辑表单。
 */
public record FolderForm(
        Long pid,
        @NotBlank(message = "目录名不能为空")
        @Size(max = 50, message = "目录名最长 50 字")
        String name,
        Integer sort,
        String description) {
}
