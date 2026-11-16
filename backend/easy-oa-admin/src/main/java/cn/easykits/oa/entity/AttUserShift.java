package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工班次绑定：一个员工一条绑定记录，未绑定的员工使用默认班次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_user_shift")
public class AttUserShift extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工 ID（唯一） */
    private Long userId;

    /** 班次 ID */
    private Long shiftId;
}
