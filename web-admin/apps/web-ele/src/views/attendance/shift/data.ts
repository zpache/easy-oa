import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { usePermission } from '#/composables/use-permission';
import { ATTENDANCE_PERMISSIONS } from '#/constants/permissions';

/** 班次表格列 */
export function useColumns(
  onActionClick: (params: OnActionClickParams) => void,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(ATTENDANCE_PERMISSIONS.shiftUpdate);
  const canDelete = can(ATTENDANCE_PERMISSIONS.shiftDelete);
  const canBind = can(ATTENDANCE_PERMISSIONS.shiftBind);

  return [
    { field: 'name', title: '班次名称', minWidth: 120 },
    {
      field: 'startTime',
      formatter: ({ row }) => row.startTime?.slice(0, 5) ?? '-',
      title: '上班时间',
      width: 100,
    },
    {
      field: 'endTime',
      formatter: ({ row }) => row.endTime?.slice(0, 5) ?? '-',
      title: '下班时间',
      width: 100,
    },
    {
      field: 'flexibleMinutes',
      formatter: ({ row }) => `${row.flexibleMinutes ?? 0} 分钟`,
      title: '弹性',
      width: 90,
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'isDefault',
      formatter: ({ row }) => (row.isDefault === 1 ? '默认班次' : '-'),
      title: '默认',
      width: 100,
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'status',
      formatter: ({ row }) => (row.status === 1 ? '启用' : '停用'),
      title: '状态',
      width: 80,
    },
    {
      field: 'boundCount',
      formatter: ({ row }) => `${row.boundCount ?? 0} 人`,
      title: '绑定人数',
      width: 100,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '班次',
          onClick: onActionClick as any,
        },
        name: 'CellOperation',
        options: [
          { code: 'bind', show: () => canBind, text: '绑定员工' },
          { code: 'edit', show: () => canUpdate },
          { code: 'delete', show: () => canDelete },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      visible: canUpdate || canDelete || canBind,
      width: 220,
    },
  ];
}

interface OnActionClickParams {
  code: string;
  row: any;
}
