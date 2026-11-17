package cn.easykits.oa.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信绑定引导请求：一次性票据 + 账号密码确认。
 */
public record WechatBindRequest(
        @NotBlank(message = "缺少绑定票据") String bindTicket,
        @NotBlank(message = "请输入账号") String username,
        @NotBlank(message = "请输入密码") String password
) {
}
