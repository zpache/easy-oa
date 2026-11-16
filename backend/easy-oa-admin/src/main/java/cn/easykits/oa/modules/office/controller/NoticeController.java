package cn.easykits.oa.modules.office.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.entity.OfficeNotice;
import cn.easykits.oa.modules.office.dto.NoticeForm;
import cn.easykits.oa.modules.office.service.NoticeService;
import cn.easykits.oa.modules.office.vo.NoticeReadVO;
import cn.easykits.oa.modules.office.vo.NoticeVO;
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
 * 行政办公 - 公告通知管理。
 */
@Tag(name = "行政-公告通知")
@RestController
@RequestMapping("/office/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "分页查询公告")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('office:notice:list')")
    public Result<PageResult<NoticeVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String category) {
        return Result.ok(noticeService.pageQuery(pageNum, pageSize, keyword, status, category));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('office:notice:list')")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.ok(noticeService.detail(id));
    }

    @Operation(summary = "新增公告（draft=false 直接发布）")
    @PostMapping
    @PreAuthorize("hasAuthority('office:notice:create')")
    public Result<OfficeNotice> create(@Valid @RequestBody NoticeForm form,
                                       @RequestParam(defaultValue = "false") boolean publish) {
        return Result.ok(noticeService.create(form, publish));
    }

    @Operation(summary = "编辑公告（仅草稿/已撤回）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('office:notice:update')")
    public Result<OfficeNotice> update(@PathVariable Long id, @Valid @RequestBody NoticeForm form) {
        return Result.ok(noticeService.update(id, form));
    }

    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('office:notice:publish')")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "撤回公告")
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('office:notice:withdraw')")
    public Result<Void> withdraw(@PathVariable Long id) {
        noticeService.withdraw(id);
        return Result.ok();
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/{id}/top")
    @PreAuthorize("hasAuthority('office:notice:top')")
    public Result<Void> toggleTop(@PathVariable Long id) {
        noticeService.toggleTop(id);
        return Result.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('office:notice:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "已读明细")
    @GetMapping("/{id}/reads")
    @PreAuthorize("hasAuthority('office:notice:list')")
    public Result<List<NoticeReadVO>> reads(@PathVariable Long id) {
        return Result.ok(noticeService.readList(id));
    }

    @Operation(summary = "已读统计（已读数/范围内人数）")
    @GetMapping("/{id}/read-stat")
    @PreAuthorize("hasAuthority('office:notice:list')")
    public Result<NoticeService.ReadStat> readStat(@PathVariable Long id) {
        return Result.ok(noticeService.readStat(id));
    }
}
