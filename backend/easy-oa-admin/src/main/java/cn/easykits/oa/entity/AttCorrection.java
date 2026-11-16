package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请：员工对漏卡的一日补登申请，由持有 attendance:correction:audit
 * 权限的负责人审批；通过后回写打卡记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_correction")
public class AttCorrection extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人 ID */
    private Long userId;

    /** 补卡对应的日期 */
    private LocalDate workDate;

    /** 补哪次卡：1 上班卡，2 下班卡 */
    private Integer type;

    /** 申请原因 */
    private String reason;

    /** 状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人 ID */
    private Long approverId;

    /** 审批备注 */
    private String approveRemark;

    /** 审批时间 */
    private LocalDateTime approveAt;
}
