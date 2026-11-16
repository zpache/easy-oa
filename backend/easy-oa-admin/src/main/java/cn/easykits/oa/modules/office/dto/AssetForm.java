package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产新增/编辑表单。
 */
public record AssetForm(
        @NotBlank(message = "资产编号不能为空")
        @Size(max = 40, message = "资产编号过长")
        String assetNo,

        @NotBlank(message = "资产名称不能为空")
        @Size(max = 100, message = "资产名称过长")
        String name,

        @NotBlank(message = "分类不能为空")
        String category,

        String brandModel,

        LocalDate purchaseDate,

        BigDecimal price,

        String location,

        String remark
) {
}
