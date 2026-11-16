package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 员工假期余额：员工 + 假期类型 一条，唯一键 uk_user_type。
 * 已用额度来源：当前阶段管理员登记；后续请假走 easy-bpm 审批通过后自动核销。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_leave_balance")
public class AttLeaveBalance extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工 ID */
    private Long userId;

    /** 假期类型 ID */
    private Long typeId;

    /** 总额度（天） */
    private BigDecimal totalQuota;

    /** 已用额度（天） */
    private BigDecimal usedQuota;
}
