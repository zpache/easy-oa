package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 假期类型：年假/事假/病假等，额度单位统一为天。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_leave_type")
public class AttLeaveType extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型名称，如「年假」 */
    private String name;

    /** 说明 */
    private String remark;

    /** 默认额度（天/年）：新建员工余额时的参考值 */
    private java.math.BigDecimal defaultQuota;

    /** 状态：0 停用，1 启用 */
    private Integer status;
}
