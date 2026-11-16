package cn.easykits.oa.modules.office.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.entity.OfficeMeetingRoom;
import cn.easykits.oa.modules.office.dto.MeetingRoomForm;
import cn.easykits.oa.modules.office.service.MeetingRoomService;
import cn.easykits.oa.modules.office.vo.MeetingRoomVO;
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
 * 行政办公 - 会议室档案。
 */
@Tag(name = "行政-会议室管理")
@RestController
@RequestMapping("/office/meeting/room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @Operation(summary = "会议室列表")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('office:room:list', 'office:booking:create')")
    public Result<List<MeetingRoomVO>> list() {
        return Result.ok(meetingRoomService.listAll());
    }

    @Operation(summary = "新增会议室")
    @PostMapping
    @PreAuthorize("hasAuthority('office:room:create')")
    public Result<OfficeMeetingRoom> create(@Valid @RequestBody MeetingRoomForm form) {
        return Result.ok(meetingRoomService.create(form));
    }

    @Operation(summary = "编辑会议室")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('office:room:update')")
    public Result<OfficeMeetingRoom> update(@PathVariable Long id, @Valid @RequestBody MeetingRoomForm form) {
        return Result.ok(meetingRoomService.update(id, form));
    }

    @Operation(summary = "删除会议室")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('office:room:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        meetingRoomService.delete(id);
        return Result.ok();
    }
}
