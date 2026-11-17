package cn.easykits.oa.modules.auth.vo;

import java.time.LocalDateTime;

/**
 * 微信授权链接响应。
 *
 * @param mock  true = mock 模式（前端弹「模拟扫码」确认框而非真实二维码）
 * @param url   微信开放平台授权 URL（mock 时为回调页地址 + state）
 * @param state 一次性防 CSRF 参数（前端回调时原样带回）
 */
public record WechatQrUrlVO(boolean mock, String url, String state) {
}
