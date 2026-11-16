package cn.easykits.oa.common.permission;

import java.util.Set;

/**
 * 管理端内置权限码。
 * <p>
 * 菜单 / 按钮的授权关系仍以 sys_menu.auth_code + sys_role_menu 为准；这里集中登记代码中
 * 实际校验的权限码，用于超级管理员兜底和前端按钮展示。新增后端校验时必须同步登记，
 * 否则非 ADMIN 角色即使授权也无法通过 authority 校验，ADMIN 也不会默认获得该码。
 * <p>
 * 各模块权限码分段：system:xxx 系统管理；后续审批中心 approval:xxx、
 * 行政 office:xxx、考勤 attendance:xxx、日程 schedule:xxx 等随模块落地登记。
 */
public final class PermissionCodes {

    public static final Set<String> ALL = Set.of(
            // 工作台
            "dashboard:workbench",

            // 系统管理
            "system:user:list", "system:user:create", "system:user:update",
            "system:user:delete", "system:user:status", "system:user:reset-password",
            "system:role:list", "system:role:create", "system:role:update",
            "system:role:delete", "system:role:status",
            "system:menu:list", "system:menu:create", "system:menu:update", "system:menu:delete",
            "system:dept:list", "system:dept:create", "system:dept:update", "system:dept:delete",
            "system:operlog:list", "system:loginlog:list",

            // 行政办公
            "office:notice:list", "office:notice:create", "office:notice:update",
            "office:notice:delete", "office:notice:publish", "office:notice:withdraw",
            "office:notice:top", "office:notice:portal",
            "office:room:list", "office:room:create", "office:room:update", "office:room:delete",
            "office:booking:list", "office:booking:create", "office:booking:cancel"
    );

    private PermissionCodes() {
    }
}
