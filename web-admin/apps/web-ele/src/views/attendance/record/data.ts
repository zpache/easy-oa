import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { getUserList } from '#/api/system/user';

export const IN_STATUS_TEXT: Record<number, string> = {
  0: '正常',
  1: '迟到',
  2: '补卡',
};
export const OUT_STATUS_TEXT: Record<number, string> = {
  0: '正常',
  1: '早退',
  2: '补卡',
};
export const STATUS_TAG: Record<number, string> = {
  0: 'success',
  1: 'danger',
  2: 'warning',
};

/** 查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: async () => {
          const result = await getUserList({
            pageNum: 1,
            pageSize: 500,
            status: 1,
          });
          return (result.items ?? []).map((u: any) => ({
            label: u.nickname ?? u.username,
            value: u.id,
          }));
        },
        class: 'w-full',
        filterable: true,
        placeholder: '全部员工',
        showSearch: true,
      },
      fieldName: 'userId',
      label: '员工',
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
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: Object.entries(IN_STATUS_TEXT).map(([value, label]) => ({
          label: `上班-${label}`,
          value: Number(value),
        })),
        placeholder: '上班状态',
      },
      fieldName: 'inStatus',
      label: '上班状态',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: Object.entries(OUT_STATUS_TEXT).map(([value, label]) => ({
          label: `下班-${label}`,
          value: Number(value),
        })),
        placeholder: '下班状态',
      },
      fieldName: 'outStatus',
      label: '下班状态',
    },
  ];
}

/** 考勤记录列 */
export function useColumns(): VxeTableGridColumns {
  return [
    { field: 'workDate', title: '日期', width: 120 },
    { field: 'userName', title: '员工', minWidth: 100 },
    {
      field: 'deptName',
      formatter: ({ row }) => row.deptName ?? '-',
      minWidth: 120,
      title: '部门',
    },
    {
      field: 'shiftName',
      formatter: ({ row }) => row.shiftName ?? '-',
      title: '班次',
      width: 100,
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'clockIn',
      formatter: ({ row }) =>
        row.clockIn
          ? `${row.clockIn.slice(0, 5)} ${IN_STATUS_TEXT[row.clockInStatus ?? 0]}`
          : '缺卡',
      title: '上班',
      width: 130,
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'clockOut',
      formatter: ({ row }) =>
        row.clockOut
          ? `${row.clockOut.slice(0, 5)} ${OUT_STATUS_TEXT[row.clockOutStatus ?? 0]}`
          : '缺卡',
      title: '下班',
      width: 130,
    },
  ];
}
