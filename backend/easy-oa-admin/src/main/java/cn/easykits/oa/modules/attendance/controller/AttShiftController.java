package cn.easykits.oa.modules.attendance.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.entity.AttShift;
import cn.easykits.oa.modules.attendance.dto.BindForm;
import cn.easykits.oa.modules.attendance.dto.ShiftForm;
import cn.easykits.oa.modules.attendance.service.AttShiftService;
import cn.easykits.oa.modules.attendance.vo.ShiftVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 考勤班次管理接口。
 */
@Tag(name = "考勤-班次管理")
@RestController
@RequestMapping("/attendance/shift")
@RequiredArgsConstructor
public class AttShiftController {

    private final AttShiftService shiftService;

    @Operation(summary = "班次列表")
    @GetMapping
    @PreAuthorize("hasAuthority('attendance:shift:list')")
    public Result<List<ShiftVO>> list() {
        return Result.ok(shiftService.listAll());
    }

    @Operation(summary = "新增班次")
    @PostMapping
    @PreAuthorize("hasAuthority('attendance:shift:create')")
    public Result<Long> create(@Valid @RequestBody ShiftForm form) {
        return Result.ok(shiftService.create(form).getId());
    }

    @Operation(summary = "编辑班次")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance:shift:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShiftForm form) {
        shiftService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除班次（绑定中的班次不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('attendance:shift:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        shiftService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "绑定/解绑员工班次")
    @PostMapping("/bind")
    @PreAuthorize("hasAuthority('attendance:shift:bind')")
    public Result<Void> bind(@Valid @RequestBody BindForm form) {
        shiftService.bind(form);
        return Result.ok();
    }
}
