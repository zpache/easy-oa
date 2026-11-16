package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 每日打卡记录：一个员工一个工作日一条（唯一键 uk_user_date）。
 * 上下班状态在打卡瞬间判定并落库，补卡通过后回写为「补卡」。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_checkin")
public class AttCheckin extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工 ID */
    private Long userId;

    /** 工作日 */
    private LocalDate workDate;

    /** 当日生效班次 ID */
    private Long shiftId;

    /** 上班打卡时间 */
    private LocalTime clockIn;

    /** 上班状态：0 正常，1 迟到，2 补卡 */
    private Integer clockInStatus;

    /** 下班打卡时间 */
    private LocalTime clockOut;

    /** 下班状态：0 正常，1 早退，2 补卡 */
    private Integer clockOutStatus;
}
