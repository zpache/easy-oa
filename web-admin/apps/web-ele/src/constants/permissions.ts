/** 管理端权限码：必须与后端 @PreAuthorize / sys_menu.auth_code 保持一致 */

export const DASHBOARD_PERMISSIONS = {
  workbench: 'dashboard:workbench',
} as const;

export const OFFICE_PERMISSIONS = {
  bookingCancel: 'office:booking:cancel',
  bookingCreate: 'office:booking:create',
  bookingList: 'office:booking:list',
  noticeCreate: 'office:notice:create',
  noticeDelete: 'office:notice:delete',
  noticeList: 'office:notice:list',
  noticePortal: 'office:notice:portal',
  noticePublish: 'office:notice:publish',
  noticeTop: 'office:notice:top',
  noticeUpdate: 'office:notice:update',
  noticeWithdraw: 'office:notice:withdraw',
  roomCreate: 'office:room:create',
  roomDelete: 'office:room:delete',
  roomList: 'office:room:list',
  roomUpdate: 'office:room:update',
} as const;

export const SYSTEM_PERMISSIONS = {
  deptCreate: 'system:dept:create',
  deptDelete: 'system:dept:delete',
  deptList: 'system:dept:list',
  deptUpdate: 'system:dept:update',
  loginLogList: 'system:loginlog:list',
  menuCreate: 'system:menu:create',
  menuDelete: 'system:menu:delete',
  menuList: 'system:menu:list',
  menuUpdate: 'system:menu:update',
  operLogList: 'system:operlog:list',
  roleCreate: 'system:role:create',
  roleDelete: 'system:role:delete',
  roleList: 'system:role:list',
  roleStatus: 'system:role:status',
  roleUpdate: 'system:role:update',
  userCreate: 'system:user:create',
  userDelete: 'system:user:delete',
  userList: 'system:user:list',
  userResetPassword: 'system:user:reset-password',
  userStatus: 'system:user:status',
  userUpdate: 'system:user:update',
} as const;

// 预留分段：审批中心 approval:*、行政 office:*、考勤 attendance:*、
// 日程 schedule:*、文档 doc:*，随各阶段模块落地登记。
