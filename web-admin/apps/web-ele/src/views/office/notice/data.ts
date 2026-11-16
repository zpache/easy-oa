import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OfficeNoticeApi } from '#/api/office/notice';

import { usePermission } from '#/composables/use-permission';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

export const NOTICE_STATUS = {
  draft: 0,
  published: 1,
  withdrawn: 2,
} as const;

const STATUS_TEXT: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已撤回',
};

const CATEGORY_TEXT: Record<string, string> = {
  activity: '活动',
  notice: '通知',
  policy: '制度',
};

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: '按标题搜索' },
      fieldName: 'keyword',
      label: '标题',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '草稿', value: 0 },
          { label: '已发布', value: 1 },
          { label: '已撤回', value: 2 },
        ],
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '通知', value: 'notice' },
          { label: '制度', value: 'policy' },
          { label: '活动', value: 'activity' },
        ],
      },
      fieldName: 'category',
      label: '分类',
    },
  ];
}

/**
 * 公告表格列
 */
export function useColumns(
  onActionClick: OnActionClickFn<OfficeNoticeApi.Notice>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(OFFICE_PERMISSIONS.noticeUpdate);
  const canPublish = can(OFFICE_PERMISSIONS.noticePublish);
  const canWithdraw = can(OFFICE_PERMISSIONS.noticeWithdraw);
  const canTop = can(OFFICE_PERMISSIONS.noticeTop);
  const canDelete = can(OFFICE_PERMISSIONS.noticeDelete);
  const showOperation = canUpdate || canPublish || canWithdraw || canDelete || canTop;

  return [
    {
      field: 'title',
      minWidth: 220,
      title: '标题',
    },
    {
      field: 'category',
      formatter: ({ row }) => CATEGORY_TEXT[row.category] ?? row.category,
      title: '分类',
      width: 80,
    },
    {
      field: 'scopeType',
      formatter: ({ row }) =>
        row.scopeType === 'dept'
          ? `指定部门 (${(row.deptIds ?? []).length} 个)`
          : '全员',
      minWidth: 120,
      title: '投放范围',
    },
    {
      cellRender: {
        name: 'CellTag',
      },
      field: 'top',
      formatter: ({ row }) => (row.top === 1 ? '置顶' : '普通'),
      title: '置顶',
      width: 80,
    },
    {
      cellRender: {
        name: 'CellTag',
      },
      field: 'status',
      formatter: ({ row }) => STATUS_TEXT[row.status] ?? row.status,
      title: '状态',
      width: 90,
    },
    {
      field: 'readCount',
      formatter: ({ row }) => `${row.readCount ?? 0} 次已读`,
      title: '已读',
      width: 100,
    },
    {
      field: 'publisher',
      title: '发布人',
      width: 100,
    },
    {
      field: 'publishAt',
      title: '发布时间',
      width: 170,
    },
    {
      field: 'expireAt',
      formatter: ({ row }) => row.expireAt ?? '长期',
      title: '有效期至',
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'title',
          nameTitle: '公告',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'publish',
            show: (row: OfficeNoticeApi.Notice) =>
              canPublish && row.status !== 1,
            text: '发布',
          },
          {
            code: 'withdraw',
            show: (row: OfficeNoticeApi.Notice) =>
              canWithdraw && row.status === 1,
            text: '撤回',
          },
          {
            code: 'top',
            show: canTop,
            text: (row: OfficeNoticeApi.Notice) =>
              row.top === 1 ? '取消置顶' : '置顶',
          },
          { code: 'edit', show: (row: OfficeNoticeApi.Notice) => canUpdate && row.status !== 1 },
          { code: 'reads', show: true, text: '已读情况' },
          { code: 'delete', show: canDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      visible: showOperation,
      width: 260,
    },
  ];
}
