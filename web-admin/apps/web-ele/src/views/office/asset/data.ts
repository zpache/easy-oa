import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OfficeAssetApi } from '#/api/office/asset';

import { usePermission } from '#/composables/use-permission';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

export const ASSET_STATUS = {
  inStock: 0,
  inUse: 1,
  scrapped: 3,
  lost: 4,
} as const;

export const STATUS_TEXT: Record<number, string> = {
  0: '在库',
  1: '在用',
  3: '报废',
  4: '丢失',
};

export const CATEGORY_TEXT: Record<string, string> = {
  appliance: '电器',
  computer: '电脑设备',
  device: '外设/办公设备',
  furniture: '家具',
  other: '其他',
};

export const ACTION_TEXT: Record<string, string> = {
  assign: '领用',
  lost: '丢失登记',
  return: '归还',
  scrap: '报废',
  transfer: '转移',
};

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: '按编号/名称/型号搜索' },
      fieldName: 'keyword',
      label: '关键词',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: Object.entries(CATEGORY_TEXT).map(([value, label]) => ({
          label,
          value,
        })),
      },
      fieldName: 'category',
      label: '分类',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: Object.entries(STATUS_TEXT).map(([value, label]) => ({
          label,
          value: Number(value),
        })),
      },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/**
 * 资产表格列
 */
export function useColumns(
  onActionClick: OnActionClickFn<OfficeAssetApi.Asset>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canUpdate = can(OFFICE_PERMISSIONS.assetUpdate);
  const canAssign = can(OFFICE_PERMISSIONS.assetAssign);
  const canScrap = can(OFFICE_PERMISSIONS.assetScrap);
  const canDelete = can(OFFICE_PERMISSIONS.assetDelete);
  const showOperation = canUpdate || canAssign || canScrap || canDelete;

  return [
    {
      field: 'assetNo',
      minWidth: 150,
      title: '资产编号',
    },
    {
      field: 'name',
      minWidth: 140,
      title: '名称',
    },
    {
      field: 'category',
      formatter: ({ row }) => CATEGORY_TEXT[row.category] ?? row.category,
      title: '分类',
      width: 110,
    },
    {
      field: 'brandModel',
      formatter: ({ row }) => row.brandModel ?? '-',
      minWidth: 130,
      title: '品牌型号',
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'status',
      formatter: ({ row }) => STATUS_TEXT[row.status] ?? row.status,
      title: '状态',
      width: 80,
    },
    {
      field: 'holderName',
      formatter: ({ row }) => row.holderName ?? '-',
      title: '使用人',
      width: 100,
    },
    {
      field: 'location',
      formatter: ({ row }) => row.location ?? '-',
      title: '存放位置',
      width: 120,
    },
    {
      field: 'purchaseDate',
      formatter: ({ row }) => row.purchaseDate ?? '-',
      title: '购置日期',
      width: 110,
    },
    {
      field: 'price',
      formatter: ({ row }) =>
        row.price == null ? '-' : `¥${Number(row.price).toFixed(2)}`,
      title: '价格',
      width: 100,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '资产',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'assign',
            show: (row: OfficeAssetApi.Asset) =>
              canAssign && row.status === ASSET_STATUS.inStock,
            text: '领用',
          },
          {
            code: 'transfer',
            show: (row: OfficeAssetApi.Asset) =>
              canAssign && row.status === ASSET_STATUS.inUse,
            text: '转移',
          },
          {
            code: 'return',
            show: (row: OfficeAssetApi.Asset) =>
              canAssign && row.status === ASSET_STATUS.inUse,
            text: '归还',
          },
          {
            code: 'scrap',
            show: (row: OfficeAssetApi.Asset) =>
              canScrap && row.status !== ASSET_STATUS.scrapped,
            text: '报废',
          },
          {
            code: 'lost',
            show: (row: OfficeAssetApi.Asset) =>
              canScrap &&
              (row.status === ASSET_STATUS.inStock ||
                row.status === ASSET_STATUS.inUse),
            text: '丢失',
          },
          { code: 'records', show: true, text: '流转记录' },
          { code: 'edit', show: (row: OfficeAssetApi.Asset) => canUpdate && row.status !== ASSET_STATUS.scrapped },
          { code: 'delete', show: (row: OfficeAssetApi.Asset) => canDelete && row.status !== ASSET_STATUS.inUse },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      visible: showOperation,
      width: 300,
    },
  ];
}
