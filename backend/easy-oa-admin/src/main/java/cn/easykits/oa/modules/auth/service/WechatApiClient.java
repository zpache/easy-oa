package cn.easykits.oa.modules.auth.service;

import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.config.WechatLoginProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 微信开放平台 API 客户端（网站应用 sns 系列）。
 * <p>
 * code 一次性，失败不重试（重试必然 40163 已使用）；超时 3s/5s；
 * mock 模式（仅 dev）跳过真实调用，返回固定测试身份，便于未申请 AppID 前联调全流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatApiClient {

    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid={appid}&secret={secret}"
                    + "&code={code}&grant_type=authorization_code";
    private static final String USER_INFO_URL =
            "https://api.weixin.qq.com/sns/userinfo?access_token={token}&openid={openid}";

    private final WechatLoginProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 微信身份：openid（应用内）+ unionid（同主体，主判据）+ 昵称头像。
     */
    public record WxIdentity(String openid, String unionid, String nickname, String avatar) {
    }

    /**
     * 用授权 code 换取微信身份。
     */
    public WxIdentity exchangeByCode(String code) {
        if (properties.isMock()) {
            log.info("[wechat-mock] 跳过微信 API，返回固定测试身份");
            return new WxIdentity("mock-openid-001", "mock-unionid-001", "微信测试用户", null);
        }
        RestClient client = RestClient.builder()
                .requestFactory(simpleFactory())
                .build();
        String body = client.get()
                .uri(ACCESS_TOKEN_URL, properties.getAppid(), properties.getSecret(), code)
                .retrieve()
                .body(String.class);
        JsonNode node = parse(body);
        if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
            throw new BusinessException(mapWxError(node.get("errcode").asInt()));
        }
        String openid = node.path("openid").asText(null);
        String accessToken = node.path("access_token").asText(null);
        if (!StringUtils.hasText(openid) || !StringUtils.hasText(accessToken)) {
            throw new BusinessException("微信授权失败，请重新扫码");
        }
        // unionid 在开放平台绑定主体后返回；缺失时以 openid 兜底
        String unionid = node.path("unionid").asText(null);
        String nickname = null;
        String avatar = null;
        try {
            JsonNode info = parse(client.get()
                    .uri(USER_INFO_URL, accessToken, openid)
                    .retrieve()
                    .body(String.class));
            if (!info.hasNonNull("errcode")) {
                nickname = info.path("nickname").asText(null);
                avatar = info.path("headimgurl").asText(null);
            }
        } catch (Exception e) {
            log.warn("[wechat] 拉取用户信息失败（不阻断登录）: {}", e.getMessage());
        }
        return new WxIdentity(openid, unionid, nickname, avatar);
    }

    private SimpleClientHttpRequestFactory simpleFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            throw new BusinessException("微信接口响应异常，请稍后再试");
        }
    }

    private String mapWxError(int errcode) {
        return switch (errcode) {
            // code 已被使用 / 已过期 / 非法
            case 40029, 42003, 40163 -> "微信授权已失效，请重新扫码";
            case 40125, 41004, 40001 -> "微信应用配置错误，请联系管理员";
            default -> "微信登录失败（" + errcode + "），请稍后再试";
        };
    }
}
