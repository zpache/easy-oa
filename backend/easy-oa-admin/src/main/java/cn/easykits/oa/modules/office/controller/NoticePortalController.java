package cn.easykits.oa.modules.office.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.office.service.NoticeService;
import cn.easykits.oa.modules.office.vo.NoticePortalDetailVO;
import cn.easykits.oa.modules.office.vo.NoticePortalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行政办公 - 公告中心（员工端，所有登录用户可查看范围内公告）。
 */
@Tag(name = "行政-公告中心")
@RestController
@RequestMapping("/office/notice/portal")
@RequiredArgsConstructor
public class NoticePortalController {

    private final NoticeService noticeService;

    @Operation(summary = "分页查询我可见的公告")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('office:notice:portal')")
    public Result<PageResult<NoticePortalVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String category) {
        return Result.ok(noticeService.portalPage(pageNum, pageSize, category));
    }

    @Operation(summary = "我的未读公告数")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('office:notice:portal')")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.portalUnreadCount());
    }

    @Operation(summary = "公告详情（查看即记录已读）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('office:notice:portal')")
    public Result<NoticePortalDetailVO> detail(@PathVariable Long id) {
        return Result.ok(noticeService.portalDetail(id));
    }
}
