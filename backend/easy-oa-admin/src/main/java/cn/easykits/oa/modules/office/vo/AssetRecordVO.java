package cn.easykits.oa.modules.office.vo;

import java.time.LocalDateTime;

/**
 * 资产流转记录。
 */
public record AssetRecordVO(
        Long id,
        String action,
        Integer assetStatus,
        Long targetUserId,
        String targetUserName,
        String operatorName,
        String remark,
        LocalDateTime createdAt
) {
}
