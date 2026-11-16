package cn.easykits.oa.modules.attendance.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.modules.attendance.service.AttCheckinService;
import cn.easykits.oa.modules.attendance.vo.CheckinVO;
import cn.easykits.oa.modules.attendance.vo.MonthStatVO;
import cn.easykits.oa.modules.attendance.vo.MyCheckinVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打卡与考勤记录接口。
 */
@Tag(name = "考勤-打卡")
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttCheckinController {

    private final AttCheckinService checkinService;

    @Operation(summary = "打卡（自动判定上班/下班）")
    @PostMapping("/clock")
    public Result<MyCheckinVO> clock() {
        return Result.ok(checkinService.clock(UserContext.userId()));
    }

    @Operation(summary = "我的今日考勤状态")
    @GetMapping("/today")
    public Result<MyCheckinVO> today() {
        return Result.ok(checkinService.today(UserContext.userId()));
    }

    @Operation(summary = "我的月度统计（日历 + 迟到/早退/缺卡计数）")
    @GetMapping("/month")
    public Result<MonthStatVO> month(@RequestParam(required = false) String yearMonth) {
        return Result.ok(checkinService.monthStat(UserContext.userId(), yearMonth));
    }

    @Operation(summary = "我的打卡记录")
    @GetMapping("/my-records")
    public Result<PageResult<CheckinVO>> myRecords(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String month) {
        return Result.ok(checkinService.myPage(UserContext.userId(), pageNum, pageSize, month));
    }

    @Operation(summary = "全员考勤记录（管理端）")
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('attendance:record:list')")
    public Result<PageResult<CheckinVO>> records(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer inStatus,
            @RequestParam(required = false) Integer outStatus) {
        return Result.ok(checkinService.adminPage(pageNum, pageSize, userId, month, inStatus, outStatus));
    }
}
