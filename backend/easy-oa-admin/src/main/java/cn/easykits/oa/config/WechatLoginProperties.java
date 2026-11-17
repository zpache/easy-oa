package cn.easykits.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信扫码登录配置（oa.wechat.*）。
 * <p>
 * enabled=false 时登录页不展示微信入口，后端拒绝全部微信端点；
 * mock=true（仅 dev）跳过微信 API，以固定测试身份走全流程，便于联调；
 * appid/secret 生产环境通过环境变量 OA_WECHAT_APPID / OA_WECHAT_SECRET 注入，不入库不入仓。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oa.wechat")
public class WechatLoginProperties {

    /** 总开关：false 时全部微信端点返回未开启 */
    private boolean enabled = false;

    /** mock 模式：跳过微信 API，用固定测试身份联调（仅本地/开发环境开启） */
    private boolean mock = false;

    /** 微信开放平台网站应用 AppID */
    private String appid = "";

    /** 网站应用 AppSecret */
    private String secret = "";

    /** 授权回调页地址（前端路由，由后端拼接进授权 URL，前端不可传） */
    private String redirectUri = "";

    /** 是否允许扫码自助注册新账号（OA 场景默认关闭，账号由管理员创建） */
    private boolean allowRegister = false;
}
