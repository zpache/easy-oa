package cn.easykits.oa.modules.schedule.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.schedule.dto.EventForm;
import cn.easykits.oa.modules.schedule.service.SchEventService;
import cn.easykits.oa.modules.schedule.vo.EventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 日程/待办接口（个人操作，登录即可，无需权限码）。
 */
@Tag(name = "日程")
@RestController
@RequestMapping("/schedule/events")
@RequiredArgsConstructor
public class SchEventController {

    private final SchEventService eventService;

    @Operation(summary = "区间查询（我的 + 部门共享，重复日程已展开）")
    @GetMapping
    public Result<List<EventVO>> list(@RequestParam String start,
                                      @RequestParam String end,
                                      @RequestParam(required = false) Integer type) {
        return Result.ok(eventService.listRange(start, end, type));
    }

    @Operation(summary = "今日日程/待办（工作台）")
    @GetMapping("/today")
    public Result<List<EventVO>> today() {
        return Result.ok(eventService.listToday());
    }

    @Operation(summary = "新建日程/待办")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody EventForm form) {
        return Result.ok(eventService.create(form).getId());
    }

    @Operation(summary = "编辑日程/待办（仅创建人）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EventForm form) {
        eventService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除日程/待办（仅创建人）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "待办完成/取消完成（仅创建人）")
    @PutMapping("/{id}/done")
    public Result<Void> toggleDone(@PathVariable Long id) {
        eventService.toggleDone(id);
        return Result.ok();
    }
}
