package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告通知。
 * <p>
 * 状态机：0 草稿 → 1 已发布 → 2 已撤回；草稿与已撤回均可重新发布。
 * 投放范围：scope_type = all 全员 / dept 指定部门（范围存 office_notice_scope 关联表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_notice")
public class OfficeNotice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 分类：notice 通知 / policy 制度 / activity 活动 */
    private String category;

    /** 正文，富文本 HTML */
    private String content;

    /** 投放范围：all 全员，dept 指定部门 */
    private String scopeType;

    /** 0 草稿，1 已发布，2 已撤回 */
    private Integer status;

    /** 1 置顶，0 普通 */
    private Integer top;

    private LocalDateTime publishAt;

    /** 有效期截止时间，空表示长期有效 */
    private LocalDateTime expireAt;
}
