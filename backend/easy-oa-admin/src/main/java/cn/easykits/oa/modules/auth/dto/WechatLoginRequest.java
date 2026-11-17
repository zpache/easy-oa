package cn.easykits.oa.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信扫码回调登录请求。
 */
public record WechatLoginRequest(
        @NotBlank(message = "缺少授权码") String code,
        @NotBlank(message = "缺少扫码状态") String state
) {
}
