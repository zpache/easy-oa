package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 资产流转表单：领用 / 转移（指定新使用人），归还 / 报废 / 丢失（仅备注）。
 */
public record AssetActionForm(
        @NotNull(message = "使用人不能为空")
        Long userId,

        String remark
) {
}
