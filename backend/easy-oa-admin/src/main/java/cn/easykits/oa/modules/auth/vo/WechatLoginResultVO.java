package cn.easykits.oa.modules.auth.vo;

/**
 * 微信扫码登录结果。
 *
 * @param result     LOGIN 已绑定并登录成功 / NEED_BIND 未绑定需引导绑定
 * @param login      result=LOGIN 时的登录态（结构与密码登录一致）
 * @param bindTicket result=NEED_BIND 时的一次性绑定票据（10 分钟有效）
 * @param wxNickname result=NEED_BIND 时的微信昵称（引导页展示）
 */
public record WechatLoginResultVO(
        String result,
        LoginVO login,
        String bindTicket,
        String wxNickname) {

    public static final String RESULT_LOGIN = "LOGIN";
    public static final String RESULT_NEED_BIND = "NEED_BIND";
}
