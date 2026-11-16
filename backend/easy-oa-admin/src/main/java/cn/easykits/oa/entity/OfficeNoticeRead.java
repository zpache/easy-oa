package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告已读回执（一人一条，唯一键 notice_id + user_id）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_notice_read")
public class OfficeNoticeRead extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    private LocalDateTime readAt;
}
