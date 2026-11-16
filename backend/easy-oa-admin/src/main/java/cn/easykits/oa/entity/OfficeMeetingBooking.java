package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议室预约单。
 * <p>
 * 冲突判定：同一会议室、status = 1（未取消）、时间段 [start_at, end_at) 有重叠即冲突。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_meeting_booking")
public class OfficeMeetingBooking extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    /** 会议主题 */
    private String title;

    /** 预约人（发起者）用户 ID */
    private Long userId;

    /** 参与人说明，如 「产品组全员 + 客户 2 人」 */
    private String participants;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** 1 已预约，0 已取消 */
    private Integer status;
}
