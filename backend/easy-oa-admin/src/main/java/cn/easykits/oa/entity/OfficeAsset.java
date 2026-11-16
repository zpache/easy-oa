package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产/办公用品台账。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_asset")
public class OfficeAsset extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产编号，全局唯一，如 ZC-20261116-001 */
    private String assetNo;

    private String name;

    /** 分类：computer / device / furniture / appliance / other */
    private String category;

    /** 品牌型号 */
    private String brandModel;

    private LocalDate purchaseDate;

    private BigDecimal price;

    /** 存放位置，如 3F-工位区 */
    private String location;

    /** 0 在库，1 在用，3 报废，4 丢失 */
    private Integer status;

    /** 当前使用人（在用时非空） */
    private Long holderUserId;

    private String remark;
}
