package cn.easykits.oa.modules.office.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.office.dto.AssetActionForm;
import cn.easykits.oa.modules.office.dto.AssetForm;
import cn.easykits.oa.modules.office.service.AssetService;
import cn.easykits.oa.modules.office.vo.AssetRecordVO;
import cn.easykits.oa.modules.office.vo.AssetVO;
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
 * 行政办公 - 资产管理。
 */
@Tag(name = "行政-资产管理")
@RestController
@RequestMapping("/office/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @Operation(summary = "分页查询资产台账")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('office:asset:list')")
    public Result<PageResult<AssetVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(assetService.pageQuery(pageNum, pageSize, keyword, category, status));
    }

    @Operation(summary = "新增资产（编号留空自动生成，默认在库）")
    @PostMapping
    @PreAuthorize("hasAuthority('office:asset:create')")
    public Result<AssetVO> create(@Valid @RequestBody AssetForm form) {
        return Result.ok(assetService.create(form));
    }

    @Operation(summary = "编辑资产基础信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('office:asset:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AssetForm form) {
        assetService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除资产（在用不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('office:asset:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "领用")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('office:asset:assign')")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssetActionForm form) {
        assetService.assign(id, form);
        return Result.ok();
    }

    @Operation(summary = "归还")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('office:asset:assign')")
    public Result<Void> giveBack(@PathVariable Long id,
                                 @RequestBody(required = false) AssetActionForm form) {
        assetService.giveBack(id, form == null ? null : form.remark());
        return Result.ok();
    }

    @Operation(summary = "转移使用人")
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('office:asset:assign')")
    public Result<Void> transfer(@PathVariable Long id, @Valid @RequestBody AssetActionForm form) {
        assetService.transfer(id, form);
        return Result.ok();
    }

    @Operation(summary = "报废")
    @PutMapping("/{id}/scrap")
    @PreAuthorize("hasAuthority('office:asset:scrap')")
    public Result<Void> scrap(@PathVariable Long id,
                              @RequestBody(required = false) AssetActionForm form) {
        assetService.scrap(id, form == null ? null : form.remark());
        return Result.ok();
    }

    @Operation(summary = "丢失登记")
    @PutMapping("/{id}/lost")
    @PreAuthorize("hasAuthority('office:asset:scrap')")
    public Result<Void> markLost(@PathVariable Long id,
                                 @RequestBody(required = false) AssetActionForm form) {
        assetService.markLost(id, form == null ? null : form.remark());
        return Result.ok();
    }

    @Operation(summary = "流转记录")
    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('office:asset:list')")
    public Result<List<AssetRecordVO>> records(@PathVariable Long id) {
        return Result.ok(assetService.records(id));
    }
}
