package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日程 / 待办。
 * <p>
 * 归属人 user_id；scope = personal 私人 / dept 部门共享（dept 时冗余创建人部门便于查询）。
 * 重复规则 repeat_type：none / daily / weekly / monthly，配合 repeat_until 截止；
 * 查询时由服务层在时间区间内展开为虚拟日程。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sch_event")
public class SchEvent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属人（创建人） */
    private Long userId;

    private String title;

    /** 1 日程 2 待办 */
    private Integer type;

    /** personal 私人 / dept 部门共享 */
    private String scope;

    /** 部门共享时冗余创建人部门 */
    private Long deptId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 1 全天 */
    private Integer allDay;

    /** none daily weekly monthly */
    private String repeatType;

    private LocalDate repeatUntil;

    /** 提前提醒分钟数 */
    private Integer remindMinutes;

    private String location;

    private String color;

    private String remark;

    /** 待办完成标记 */
    private Integer done;

    private LocalDateTime doneAt;
}
