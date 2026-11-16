package cn.easykits.oa.modules.doc.controller;

import cn.easykits.oa.common.api.Result;
import cn.easykits.oa.modules.doc.dto.AclForm;
import cn.easykits.oa.modules.doc.dto.FolderForm;
import cn.easykits.oa.modules.doc.service.DocFolderService;
import cn.easykits.oa.modules.doc.vo.FolderVO;
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
 * 文档目录接口：树查询登录即可（内部按 ACL 过滤），写操作需权限码。
 */
@Tag(name = "文档-目录")
@RestController
@RequestMapping("/doc/folders")
@RequiredArgsConstructor
public class DocFolderController {

    private final DocFolderService folderService;

    @Operation(summary = "目录树（按当前用户 ACL 过滤）")
    @GetMapping
    public Result<List<FolderVO>> tree() {
        return Result.ok(folderService.tree());
    }

    @Operation(summary = "读取目录 ACL 配置")
    @GetMapping("/{id}/acl")
    @PreAuthorize("hasAuthority('doc:folder:acl')")
    public Result<List<AclForm.AclItem>> listAcl(@PathVariable Long id) {
        return Result.ok(folderService.listAcl(id));
    }

    @Operation(summary = "新增目录")
    @PostMapping
    @PreAuthorize("hasAuthority('doc:folder:create')")
    public Result<Long> create(@Valid @RequestBody FolderForm form) {
        return Result.ok(folderService.create(form).getId());
    }

    @Operation(summary = "编辑目录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:folder:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FolderForm form) {
        folderService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除目录（须先清空子目录与文档）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doc:folder:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "配置目录阅读权限（整组替换，空数组 = 全员可见）")
    @PutMapping("/{id}/acl")
    @PreAuthorize("hasAuthority('doc:folder:acl')")
    public Result<Void> setAcl(@PathVariable Long id, @Valid @RequestBody AclForm form) {
        folderService.setAcl(id, form);
        return Result.ok();
    }
}
