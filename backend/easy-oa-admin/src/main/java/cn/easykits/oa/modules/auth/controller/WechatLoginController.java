package cn.easykits.oa.modules.auth.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.auth.dto.WechatBindRequest;
import cn.easykits.oa.modules.auth.dto.WechatLoginRequest;
import cn.easykits.oa.modules.auth.service.WechatLoginService;
import cn.easykits.oa.modules.auth.vo.LoginVO;
import cn.easykits.oa.modules.auth.vo.WechatLoginResultVO;
import cn.easykits.oa.modules.auth.vo.WechatQrUrlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信扫码登录（免登录态端点：授权链接、扫码登录、引导绑定）。
 * 个人中心的绑定/解绑在 BindingController（需登录态）。
 */
@Tag(name = "系统-微信扫码登录")
@RestController
@RequestMapping("/auth/wechat")
@RequiredArgsConstructor
public class WechatLoginController {

    private final WechatLoginService wechatLoginService;

    @Operation(summary = "获取扫码登录授权链接（mock 模式返回 mock 标记）")
    @SecurityRequirements
    @PostMapping("/qr-login-url")
    public Result<WechatQrUrlVO> qrLoginUrl() {
        return Result.ok(wechatLoginService.qrLoginUrl());
    }

    @Operation(summary = "扫码回调登录（已绑定直接登录，未绑定返回 NEED_BIND + bindTicket）")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<WechatLoginResultVO> login(@Valid @RequestBody WechatLoginRequest request) {
        return Result.ok(wechatLoginService.login(request.code(), request.state()));
    }

    @Operation(summary = "绑定引导：账号密码确认绑定并直接登录")
    @SecurityRequirements
    @PostMapping("/bind")
    public Result<LoginVO> bind(@Valid @RequestBody WechatBindRequest request) {
        return Result.ok(wechatLoginService.bind(
                request.bindTicket(), request.username(), request.password()));
    }
}
