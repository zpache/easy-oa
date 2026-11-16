package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 考勤班次：上下班时间 + 弹性分钟数。
 * is_default = 1 的班次作为未单独绑定员工的默认班次（全表仅允许一条默认）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_shift")
public class AttShift extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班次名称，如「标准班」 */
    private String name;

    /** 上班时间 HH:mm */
    private LocalTime startTime;

    /** 下班时间 HH:mm */
    private LocalTime endTime;

    /** 弹性分钟数：上班晚于 start + flexible 记迟到，下班早于 end - flexible 记早退 */
    private Integer flexibleMinutes;

    /** 是否默认班次：0 否，1 是 */
    private Integer isDefault;

    /** 状态：0 停用，1 启用 */
    private Integer status;
}
