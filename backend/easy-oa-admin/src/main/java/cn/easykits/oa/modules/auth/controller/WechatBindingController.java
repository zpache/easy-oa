package cn.easykits.oa.modules.auth.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.modules.auth.service.WechatLoginService;
import cn.easykits.oa.modules.auth.vo.WechatBindingVO;
import cn.easykits.oa.modules.auth.vo.WechatQrUrlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心的微信绑定管理（登录态）。
 */
@Tag(name = "系统-微信绑定")
@RestController
@RequestMapping("/auth/wechat/binding")
@RequiredArgsConstructor
public class WechatBindingController {

    private final WechatLoginService wechatLoginService;

    @Operation(summary = "我的微信绑定信息")
    @GetMapping
    public Result<WechatBindingVO> info() {
        return Result.ok(wechatLoginService.bindingInfo(UserContext.require().userId()));
    }

    @Operation(summary = "获取绑定用授权链接（个人中心主动绑定）")
    @PostMapping("/qr-bind-url")
    public Result<WechatQrUrlVO> qrBindUrl() {
        return Result.ok(wechatLoginService.qrBindUrl());
    }

    @Operation(summary = "绑定确认（回调页携带 code + state）")
    @PostMapping("/confirm")
    public Result<WechatBindingVO> confirm(@Valid @RequestBody ConfirmRequest request) {
        return Result.ok(wechatLoginService.bindConfirm(request.code(), request.state()));
    }

    @Operation(summary = "解绑微信")
    @DeleteMapping
    public Result<WechatBindingVO> unbind() {
        return Result.ok(wechatLoginService.unbind());
    }

    public record ConfirmRequest(@jakarta.validation.constraints.NotBlank(message = "缺少授权码") String code,
                                 @jakarta.validation.constraints.NotBlank(message = "缺少扫码状态") String state) {
    }
}
