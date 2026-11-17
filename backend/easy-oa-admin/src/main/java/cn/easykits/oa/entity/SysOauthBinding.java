package cn.easykits.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 第三方身份绑定：微信扫码登录的身份 ↔ OA 账号映射。
 * <p>
 * unionid 为主判据（同主体跨应用稳定），openid 兜底；
 * 唯一索引保证一个微信身份只能绑定一个账号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oauth_binding")
public class SysOauthBinding extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** wechat_open 个人微信（预留 wecom_qr 企业微信） */
    private String appType;

    private String openid;

    private String unionid;

    /** 微信昵称（展示用） */
    private String wxNickname;

    private String wxAvatar;

    private LocalDateTime boundAt;
}
