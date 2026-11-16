package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产新增/编辑表单。
 * 编号可留空：后端按 ZC-yyyyMMdd-序号 自动生成；存量/特殊编号可手动维护。
 */
public record AssetForm(
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
