package cn.easykits.oa.modules.office.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产台账行。
 */
public record AssetVO(
        Long id,
        String assetNo,
        String name,
        String category,
        String brandModel,
        LocalDate purchaseDate,
        BigDecimal price,
        String location,
        Integer status,
        Long holderUserId,
        String holderName,
        String remark,
        LocalDateTime createdAt
) {
}
