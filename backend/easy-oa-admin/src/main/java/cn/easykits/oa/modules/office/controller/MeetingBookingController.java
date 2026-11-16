package cn.easykits.oa.modules.office.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.entity.OfficeMeetingBooking;
import cn.easykits.oa.modules.office.dto.MeetingBookingForm;
import cn.easykits.oa.modules.office.service.MeetingBookingService;
import cn.easykits.oa.modules.office.vo.MeetingBookingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 行政办公 - 会议室预约。
 */
@Tag(name = "行政-会议室预约")
@RestController
@RequestMapping("/office/meeting/booking")
@RequiredArgsConstructor
public class MeetingBookingController {

    private final MeetingBookingService meetingBookingService;

    @Operation(summary = "分页查询预约单")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('office:booking:list')")
    public Result<PageResult<MeetingBookingVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {
        return Result.ok(meetingBookingService.pageQuery(pageNum, pageSize, roomId, startDate, endDate, status, userId));
    }

    @Operation(summary = "创建预约")
    @PostMapping
    @PreAuthorize("hasAuthority('office:booking:create')")
    public Result<OfficeMeetingBooking> create(@Valid @RequestBody MeetingBookingForm form) {
        return Result.ok(meetingBookingService.create(form));
    }

    @Operation(summary = "取消预约（本人或持有 office:booking:cancel 权限）")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('office:booking:cancel') or hasAuthority('office:booking:create')")
    public Result<Void> cancel(@PathVariable Long id) {
        meetingBookingService.cancel(id);
        return Result.ok();
    }
}
