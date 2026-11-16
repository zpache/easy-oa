package cn.easykits.oa.modules.attendance.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.entity.AttLeaveType;
import cn.easykits.oa.modules.attendance.dto.BalanceAdjustForm;
import cn.easykits.oa.modules.attendance.dto.LeaveTypeForm;
import cn.easykits.oa.modules.attendance.service.AttLeaveBalanceService;
import cn.easykits.oa.modules.attendance.service.AttLeaveTypeService;
import cn.easykits.oa.modules.attendance.vo.LeaveBalanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 假期类型与余额接口。
 */
@Tag(name = "考勤-假期")
@RestController
@RequestMapping("/attendance/leave")
@RequiredArgsConstructor
public class AttLeaveController {

    private final AttLeaveTypeService leaveTypeService;
    private final AttLeaveBalanceService leaveBalanceService;

    @Operation(summary = "假期类型列表（管理端）")
    @GetMapping("/type")
    @PreAuthorize("hasAuthority('attendance:leavetype:list')")
    public Result<List<AttLeaveType>> types() {
        return Result.ok(leaveTypeService.listAll());
    }

    @Operation(summary = "启用中的假期类型（员工端下拉）")
    @GetMapping("/type/enabled")
    public Result<List<AttLeaveType>> enabledTypes() {
        return Result.ok(leaveTypeService.listEnabled());
    }

    @Operation(summary = "新增假期类型")
    @PostMapping("/type")
    @PreAuthorize("hasAuthority('attendance:leavetype:create')")
    public Result<Long> createType(@Valid @RequestBody LeaveTypeForm form) {
        return Result.ok(leaveTypeService.create(form).getId());
    }

    @Operation(summary = "编辑假期类型")
    @PutMapping("/type/{id}")
    @PreAuthorize("hasAuthority('attendance:leavetype:update')")
    public Result<Void> updateType(@PathVariable Long id, @Valid @RequestBody LeaveTypeForm form) {
        leaveTypeService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除假期类型（有余额引用时不可删）")
    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasAuthority('attendance:leavetype:delete')")
    public Result<Void> deleteType(@PathVariable Long id) {
        leaveTypeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "我的假期余额")
    @GetMapping("/balance/my")
    public Result<List<LeaveBalanceVO>> myBalances() {
        return Result.ok(leaveBalanceService.myList(UserContext.userId()));
    }

    @Operation(summary = "员工假期余额（管理端）")
    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('attendance:balance:adjust')")
    public Result<PageResult<LeaveBalanceVO>> balances(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long typeId) {
        return Result.ok(leaveBalanceService.adminPage(pageNum, pageSize, userId, typeId));
    }

    @Operation(summary = "设定员工假期总额度")
    @PostMapping("/balance/adjust")
    @PreAuthorize("hasAuthority('attendance:balance:adjust')")
    public Result<LeaveBalanceVO> adjust(@Valid @RequestBody BalanceAdjustForm form) {
        return Result.ok(leaveBalanceService.adjust(form));
    }
}
