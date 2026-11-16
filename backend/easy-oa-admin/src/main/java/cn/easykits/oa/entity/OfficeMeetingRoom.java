package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室档案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_meeting_room")
public class OfficeMeetingRoom extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 位置，如 3F-东侧 */
    private String location;

    /** 容纳人数 */
    private Integer capacity;

    /** 设备说明，如 投屏,白板,视频会议 */
    private String equipment;

    /** 1 可预约，0 停用 */
    private Integer status;

    private String remark;
}
