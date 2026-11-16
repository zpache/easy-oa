import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';

import { usePermission } from '#/composables/use-permission';
import { ATTENDANCE_PERMISSIONS } from '#/constants/permissions';

export const AUDIT_STATUS_TEXT: Record<number, string> = {
  0: '待审批',
  1: '已通过',
  2: '已驳回',
};
export const AUDIT_TAG: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
};
export const TYPE_TEXT: Record<number, string> = {
  1: '补上班卡',
  2: '补下班卡',
};

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: Object.entries(AUDIT_STATUS_TEXT).map(([value, label]) => ({
          label,
          value: Number(value),
        })),
        placeholder: '全部状态',
      },
      defaultValue: 0,
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        clearable: true,
        placeholder: '全部月份',
        type: 'month',
        valueFormat: 'YYYY-MM',
      },
      fieldName: 'month',
      label: '月份',
    },
  ];
}

/** 补卡审批列 */
export function useColumns(
  onActionClick: OnActionClickFn<any>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canAudit = can(ATTENDANCE_PERMISSIONS.correctionAudit);

  return [
    { field: 'workDate', title: '补卡日期', width: 120 },
    { field: 'userName', title: '申请人', minWidth: 100 },
    {
      field: 'deptName',
      formatter: ({ row }) => row.deptName ?? '-',
      minWidth: 110,
      title: '部门',
    },
    {
      field: 'type',
      formatter: ({ row }) => TYPE_TEXT[row.type] ?? row.type,
      title: '类型',
      width: 100,
    },
    { field: 'reason', minWidth: 160, title: '申请原因' },
    {
      cellRender: { name: 'CellTag' },
      field: 'status',
      formatter: ({ row }) => AUDIT_STATUS_TEXT[row.status] ?? row.status,
      title: '状态',
      width: 90,
    },
    {
      field: 'approverName',
      formatter: ({ row }) => row.approverName ?? '-',
      title: '审批人',
      width: 100,
    },
    {
      field: 'approveRemark',
      formatter: ({ row }) => row.approveRemark ?? '-',
      minWidth: 120,
      title: '审批备注',
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'workDate',
          nameTitle: '补卡',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'approve',
            show: (row: any) => canAudit && row.status === 0,
            text: '通过',
          },
          {
            code: 'reject',
            show: (row: any) => canAudit && row.status === 0,
            text: '驳回',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      visible: canAudit,
      width: 140,
    },
  ];
}
