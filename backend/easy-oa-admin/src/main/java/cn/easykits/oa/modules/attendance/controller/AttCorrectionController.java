package cn.easykits.oa.modules.attendance.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.modules.attendance.dto.CorrectionApplyForm;
import cn.easykits.oa.modules.attendance.dto.CorrectionAuditForm;
import cn.easykits.oa.modules.attendance.service.AttCorrectionService;
import cn.easykits.oa.modules.attendance.vo.CorrectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补卡申请与审批接口。
 */
@Tag(name = "考勤-补卡")
@RestController
@RequestMapping("/attendance/correction")
@RequiredArgsConstructor
public class AttCorrectionController {

    private final AttCorrectionService correctionService;

    @Operation(summary = "提交补卡申请")
    @PostMapping
    public Result<Long> apply(@Valid @RequestBody CorrectionApplyForm form) {
        return Result.ok(correctionService.apply(UserContext.userId(), form).getId());
    }

    @Operation(summary = "我的补卡申请")
    @GetMapping("/my")
    public Result<PageResult<CorrectionVO>> my(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.ok(correctionService.myPage(UserContext.userId(), pageNum, pageSize, status));
    }

    @Operation(summary = "补卡审批列表（管理端）")
    @GetMapping
    @PreAuthorize("hasAuthority('attendance:correction:audit')")
    public Result<PageResult<CorrectionVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String month) {
        return Result.ok(correctionService.adminPage(pageNum, pageSize, userId, status, month));
    }

    @Operation(summary = "补卡审批（通过/驳回）")
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('attendance:correction:audit')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody CorrectionAuditForm form) {
        correctionService.audit(id, form, UserContext.userId());
        return Result.ok();
    }
}
