package cn.easykits.oa.modules.auth.service;

import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.context.LoginUser;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.config.WechatLoginProperties;
import cn.easykits.oa.entity.SysOauthBinding;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.SysOauthBindingMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.auth.vo.LoginVO;
import cn.easykits.oa.modules.auth.vo.WechatBindingVO;
import cn.easykits.oa.modules.auth.vo.WechatLoginResultVO;
import cn.easykits.oa.modules.auth.vo.WechatQrUrlVO;
import cn.easykits.oa.modules.log.service.LoginLogService;
import cn.easykits.oa.security.AdminAuthorizationService;
import cn.easykits.oa.security.AdminUserDetails;
import cn.easykits.oa.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 个人微信扫码登录：授权链接（state 防 CSRF）→ code 换身份 → 绑定命中判定 →
 * 登录 / 引导绑定（密码确认）→ 个人中心绑定解绑。
 * <p>
 * 一次性凭证走 Redis：state（5 分钟，GETDEL 消费）、bindTicket（10 分钟）；
 * 绑定动作必须经密码验证，绝无仅凭微信身份建立登录态的路径；
 * 自助注册由 {@code oa.wechat.allow-register} 控制（OA 场景默认关闭）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatLoginService {

    public static final String APP_WECHAT_OPEN = "wechat_open";

    private static final String STATE_KEY_PREFIX = "oa:qr:state:";
    private static final String BIND_TICKET_KEY_PREFIX = "oa:qr:bind:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final Duration BIND_TICKET_TTL = Duration.ofMinutes(10);

    private static final String QR_AUTH_URL =
            "https://open.weixin.qq.com/connect/qrconnect?appid={appid}"
                    + "&redirect_uri={redirect}&response_type=code&scope=snsapi_login"
                    + "&state={state}#wechat_redirect";

    private final WechatLoginProperties properties;
    private final WechatApiClient wechatApiClient;
    private final StringRedisTemplate redisTemplate;
    private final SysOauthBindingMapper bindingMapper;
    private final SysUserMapper sysUserMapper;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginGuardService loginGuardService;
    private final LoginLogService loginLogService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 绑定票据中暂存的微信身份 */
    private record WxIdentityPayload(
            String openid, String unionid, String nickname, String avatar) {
    }

    /* ==================== 授权链接 ==================== */

    /**
     * 扫码登录授权链接（免登录态）。
     */
    public WechatQrUrlVO qrLoginUrl() {
        requireEnabled();
        String state = randomToken();
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "login", STATE_TTL);
        return new WechatQrUrlVO(properties.isMock(), buildAuthUrl(state), state);
    }

    /**
     * 个人中心主动绑定的授权链接（登录态）。
     */
    public WechatQrUrlVO qrBindUrl() {
        requireEnabled();
        String state = randomToken();
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "bind", STATE_TTL);
        return new WechatQrUrlVO(properties.isMock(), buildAuthUrl(state), state);
    }

    private String buildAuthUrl(String state) {
        if (properties.isMock()) {
            // mock：无真实二维码，前端直接以 state 调 login，code 传 mock
            return properties.getRedirectUri() + "?state=" + state;
        }
        if (!StringUtils.hasText(properties.getAppid())
                || !StringUtils.hasText(properties.getRedirectUri())) {
            throw new BusinessException("微信登录配置不完整（appid/redirect-uri），请联系管理员");
        }
        return QR_AUTH_URL
                .replace("{appid}", properties.getAppid())
                .replace("{redirect}", urlEncode(properties.getRedirectUri()))
                .replace("{state}", state);
    }

    /* ==================== 扫码登录 ==================== */

    /**
     * 扫码回调：code + state 换登录态或引导绑定。
     */
    public WechatLoginResultVO login(String code, String state) {
        requireEnabled();
        consumeState(state, "login");
        WechatApiClient.WxIdentity identity = wechatApiClient.exchangeByCode(code);

        SysOauthBinding binding = findBinding(identity);
        if (binding != null) {
            LoginUser loginUser = adminAuthorizationService.resolveById(binding.getUserId())
                    .orElseThrow(() -> new BusinessException(ResultCode.ACCOUNT_DISABLED,
                            "绑定账号已被停用或删除，请联系管理员"));
            LoginVO login = issueLogin(loginUser, "微信扫码登录");
            return new WechatLoginResultVO(WechatLoginResultVO.RESULT_LOGIN, login, null, null);
        }

        if (properties.isAllowRegister()) {
            // 预留：OA 场景默认关闭自助注册，开启后在此创建账号并绑定
            throw new BusinessException("自助注册暂未开放");
        }
        String ticket = randomToken();
        try {
            redisTemplate.opsForValue().set(BIND_TICKET_KEY_PREFIX + ticket,
                    objectMapper.writeValueAsString(new WxIdentityPayload(
                            identity.openid(), identity.unionid(),
                            identity.nickname(), identity.avatar())),
                    BIND_TICKET_TTL);
        } catch (Exception e) {
            throw new BusinessException("微信身份暂存失败，请重新扫码");
        }
        return new WechatLoginResultVO(WechatLoginResultVO.RESULT_NEED_BIND, null,
                ticket, identity.nickname());
    }

    /* ==================== 绑定引导（密码确认） ==================== */

    /**
     * 未绑定用户扫码后：账号密码确认绑定并直接登录。
     * 密码错误计入与密码登录共享的防爆破计数。
     */
    public LoginVO bind(String bindTicket, String username, String password) {
        requireEnabled();
        WxIdentityPayload identity = consumeBindTicket(bindTicket);

        loginGuardService.checkLocked(username);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            loginGuardService.recordFailure(username);
            loginLogService.record(username, null, false, "账号已停用（微信绑定）");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            int remain = loginGuardService.recordFailure(username);
            loginLogService.record(username, null, false, "账号或密码错误（微信绑定）");
            log.warn("微信绑定失败: username={}, reason={}", username, e.getMessage());
            throw new BusinessException(remain > 0 && remain < 3
                    ? "账号或密码错误，还可尝试 " + remain + " 次" : "账号或密码错误");
        }
        AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
        LoginUser loginUser = new LoginUser(details.getId(), details.getUsername(),
                details.getNickname(), details.getRoles());

        saveBinding(details.getId(), toIdentity(identity));
        loginGuardService.recordSuccess(username);
        LoginVO login = issueLogin(loginUser, "微信扫码绑定成功");
        return login;
    }

    /* ==================== 个人中心绑定 / 解绑 ==================== */

    /**
     * 已登录用户确认绑定（回调页携带 code + state）。
     */
    public WechatBindingVO bindConfirm(String code, String state) {
        requireEnabled();
        Long userId = UserContext.require().userId();
        consumeState(state, "bind");
        WechatApiClient.WxIdentity identity = wechatApiClient.exchangeByCode(code);
        saveBinding(userId, identity);
        return bindingInfo(userId);
    }

    /**
     * 解绑当前账号的微信。
     */
    public WechatBindingVO unbind() {
        Long userId = UserContext.require().userId();
        SysOauthBinding binding = findByUserId(userId);
        if (binding == null) {
            return new WechatBindingVO(false, null, null, null);
        }
        bindingMapper.deleteById(binding.getId());
        return new WechatBindingVO(false, null, null, null);
    }

    /**
     * 当前账号绑定信息。
     */
    public WechatBindingVO bindingInfo(Long userId) {
        SysOauthBinding binding = findByUserId(userId);
        return binding == null
                ? new WechatBindingVO(false, null, null, null)
                : new WechatBindingVO(true, binding.getWxNickname(),
                binding.getWxAvatar(), binding.getBoundAt());
    }

    /* ==================== 私有方法 ==================== */

    private void saveBinding(Long userId, WechatApiClient.WxIdentity identity) {
        SysOauthBinding existing = findBinding(identity);
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                return;
            }
            throw new BusinessException("该微信已绑定其他账号（"
                    + maskUserId(existing.getUserId()) + "），请先解绑后再试");
        }
        SysOauthBinding binding = new SysOauthBinding();
        binding.setUserId(userId);
        binding.setAppType(APP_WECHAT_OPEN);
        binding.setOpenid(identity.openid());
        binding.setUnionid(identity.unionid());
        binding.setWxNickname(identity.nickname());
        binding.setWxAvatar(identity.avatar());
        binding.setBoundAt(LocalDateTime.now());
        try {
            bindingMapper.insert(binding);
        } catch (DuplicateKeyException e) {
            // 并发绑定同微信：唯一索引兜底
            throw new BusinessException("该微信已绑定其他账号，请先解绑后再试");
        }
    }

    private WechatApiClient.WxIdentity toIdentity(WxIdentityPayload payload) {
        return new WechatApiClient.WxIdentity(payload.openid(), payload.unionid(),
                payload.nickname(), payload.avatar());
    }

    /**
     * 绑定查找：unionid 命中优先，其次 openid。
     */
    private SysOauthBinding findBinding(WechatApiClient.WxIdentity identity) {
        if (StringUtils.hasText(identity.unionid())) {
            SysOauthBinding byUnionid = bindingMapper.selectOne(
                    Wrappers.<SysOauthBinding>lambdaQuery()
                            .eq(SysOauthBinding::getAppType, APP_WECHAT_OPEN)
                            .eq(SysOauthBinding::getUnionid, identity.unionid())
                            .last("limit 1"));
            if (byUnionid != null) {
                return byUnionid;
            }
        }
        return bindingMapper.selectOne(Wrappers.<SysOauthBinding>lambdaQuery()
                .eq(SysOauthBinding::getAppType, APP_WECHAT_OPEN)
                .eq(SysOauthBinding::getOpenid, identity.openid())
                .last("limit 1"));
    }

    private SysOauthBinding findByUserId(Long userId) {
        return bindingMapper.selectOne(Wrappers.<SysOauthBinding>lambdaQuery()
                .eq(SysOauthBinding::getAppType, APP_WECHAT_OPEN)
                .eq(SysOauthBinding::getUserId, userId)
                .last("limit 1"));
    }

    /**
     * 消费 state：GETDEL 原子取删，场景不符视为重放/串用。
     */
    private void consumeState(String state, String expectedScene) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException("缺少扫码状态参数");
        }
        String scene = redisTemplate.opsForValue().getAndDelete(STATE_KEY_PREFIX + state);
        if (scene == null) {
            throw new BusinessException("扫码状态已过期，请重新扫码");
        }
        if (!Objects.equals(scene, expectedScene)) {
            throw new BusinessException("扫码状态不匹配，请重新扫码");
        }
    }

    private WxIdentityPayload consumeBindTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException("缺少绑定票据");
        }
        String payload = redisTemplate.opsForValue().getAndDelete(BIND_TICKET_KEY_PREFIX + ticket);
        if (payload == null) {
            throw new BusinessException("绑定会话已过期，请重新扫码");
        }
        try {
            return objectMapper.readValue(payload, WxIdentityPayload.class);
        } catch (Exception e) {
            throw new BusinessException("绑定票据损坏，请重新扫码");
        }
    }

    /**
     * 签发登录态：JWT + 更新最近登录时间 + 登录日志。
     */
    private LoginVO issueLogin(LoginUser loginUser, String logMessage) {
        String token = jwtTokenProvider.createToken(loginUser);
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, loginUser.userId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));
        loginLogService.record(loginUser.username(), loginUser.userId(), true, logMessage);
        return new LoginVO(token, token, "Bearer", jwtTokenProvider.getExpireSeconds(), loginUser);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "微信登录未开启");
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String maskUserId(Long userId) {
        String s = String.valueOf(userId);
        return s.length() <= 2 ? "**" : s.charAt(0) + "***";
    }
}
