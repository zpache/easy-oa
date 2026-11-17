package cn.easykits.oa.modules.auth.vo;

import java.time.LocalDateTime;

/**
 * 当前账号的微信绑定信息。
 */
public record WechatBindingVO(
        boolean bound,
        String wxNickname,
        String wxAvatar,
        LocalDateTime boundAt) {
}
