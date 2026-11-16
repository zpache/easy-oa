package cn.easykits.oa.modules.doc.controller;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.doc.dto.DocForm;
import cn.easykits.oa.modules.doc.service.DocDocumentService;
import cn.easykits.oa.modules.doc.vo.DocDetailVO;
import cn.easykits.oa.modules.doc.vo.DocVO;
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

/**
 * 文档接口：读操作登录即可（内部按 ACL 过滤），写操作需权限码。
 */
@Tag(name = "文档")
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
public class DocController {

    private final DocDocumentService docService;

    @Operation(summary = "文档分页（目录内列表 + 全文检索）")
    @GetMapping("/list")
    public Result<PageResult<DocVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(docService.page(pageNum, pageSize, folderId, keyword, status));
    }

    @Operation(summary = "文档详情（含正文）")
    @GetMapping("/{id}")
    public Result<DocDetailVO> detail(@PathVariable Long id) {
        return Result.ok(docService.detail(id));
    }

    @Operation(summary = "新建文档（默认草稿，publish=true 直接发布）")
    @PostMapping
    @PreAuthorize("hasAuthority('doc:doc:create')")
    public Result<Long> create(@Valid @RequestBody DocForm form,
                               @RequestParam(defaultValue = "false") boolean publish) {
        return Result.ok(docService.create(form, publish).getId());
    }

    @Operation(summary = "编辑文档（仅创建人 / 管理员）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:doc:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DocForm form) {
        docService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "发布文档")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('doc:doc:publish')")
    public Result<Void> publish(@PathVariable Long id) {
        docService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "删除文档（仅创建人 / 管理员）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:doc:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        docService.delete(id);
        return Result.ok();
    }
}
