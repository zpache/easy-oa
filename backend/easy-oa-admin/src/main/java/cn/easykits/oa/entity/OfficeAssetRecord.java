package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产流转记录：领用/归还/转移/报废/丢失，一人一条，只增不改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_asset_record")
public class OfficeAssetRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    /** 动作：assign 领用 / return 归还 / transfer 转移 / scrap 报废 / lost 丢失 */
    private String action;

    /** 操作对象用户：领用人 / 归还人（动作发起的使用人）/ 转移后的新使用人 */
    private Long targetUserId;

    /** 流转后的资产状态快照 */
    private Integer assetStatus;

    private String remark;
}
