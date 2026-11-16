package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告投放范围（scope_type = dept 时，每个可见部门一行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_notice_scope")
public class OfficeNoticeScope extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long deptId;
}
