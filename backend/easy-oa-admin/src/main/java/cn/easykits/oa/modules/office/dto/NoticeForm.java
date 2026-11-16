package cn.easykits.oa.modules.office.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告新增 / 编辑表单。
 *
 * @param title     标题
 * @param category  分类：notice 通知 / policy 制度 / activity 活动
 * @param content   正文（富文本 HTML）
 * @param scopeType 投放范围：all 全员 / dept 指定部门
 * @param deptIds   指定部门 ID 列表（scopeType = dept 时必填）
 * @param top       是否置顶：1 是，0 否
 * @param expireAt  有效期截止时间，空表示长期有效
 */
public record NoticeForm(

        @NotBlank(message = "公告标题不能为空")
        @Size(max = 100, message = "公告标题不能超过 100 个字符")
        String title,

        @Size(max = 20, message = "分类不合法")
        String category,

        @NotBlank(message = "公告内容不能为空")
        String content,

        @NotBlank(message = "投放范围不能为空")
        String scopeType,

        List<Long> deptIds,

        Integer top,

        LocalDateTime expireAt
) {
}
