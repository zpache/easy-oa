<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { SystemMenuApi } from '#/api/system/menu';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { ElButton, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteMenu,
  getMenuList,
  moveMenu,
  updateMenuOrder,
} from '#/api/system/menu';
import { SYSTEM_PERMISSIONS } from '#/constants/permissions';
import { $t } from '#/locales';

import { useColumns } from './data';
import Form from './modules/form.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getMenuList();
          return { items: list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      zoom: true,
    },
    treeConfig: {
      parentField: 'pid',
      rowField: 'id',
      transform: false,
    },
  } as VxeTableGridOptions,
});

async function onActionClick({
  code,
  row,
}: OnActionClickParams<SystemMenuApi.SystemMenu>) {
  switch (code) {
    case 'append': {
      formDrawerApi.setData({ pid: row.id }).open();
      break;
    }
    case 'delete': {
      onDelete(row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(row).open();
      break;
    }
    case 'moveDown':
    case 'moveUp': {
      try {
        await moveMenu(row.id, code === 'moveUp' ? 'up' : 'down');
        gridApi.query();
      } catch {
        // 边界（已在最前/最后）由后端返回提示，请求拦截器统一弹出
      }
      break;
    }
  }
}

/**
 * 内联修改排序值：数字越小越靠前，保存后刷新整棵树。
 */
async function onOrderChange(
  row: SystemMenuApi.SystemMenu,
  value: number | undefined,
) {
  const order = value ?? 0;
  if (order === (row.meta?.order ?? 0)) {
    return;
  }
  try {
    await updateMenuOrder(row.id, order);
    ElMessage.success('排序已更新');
  } finally {
    gridApi.query();
  }
}

async function onDelete(row: SystemMenuApi.SystemMenu) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [
      row.meta?.title || row.name,
    ]),
    $t('ui.actionTitle.delete', [$t('system.menu.name')]),
    { type: 'warning' },
  );
  await deleteMenu(row.id);
  ElMessage.success('删除成功');
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('system.menu.title')">
      <template #toolbar-tools>
        <ElButton
          v-access:code="SYSTEM_PERMISSIONS.menuCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('system.menu.name')]) }}
        </ElButton>
      </template>
      <template #title="{ row }">
        <div class="flex w-full items-center gap-2">
          <div class="size-5 shrink-0">
            <IconifyIcon
              v-if="row.type === 'button'"
              class="size-full"
              icon="carbon:security"
            />
            <IconifyIcon
              v-else-if="row.meta?.icon"
              class="size-full"
              :icon="row.meta.icon"
            />
          </div>
          <span class="flex-auto">{{ row.meta?.title }}</span>
        </div>
      </template>
      <template #order="{ row }">
        <ElInputNumber
          :model-value="row.meta?.order ?? 0"
          :min="0"
          :step="10"
          controls-position="right"
          size="small"
          style="width: 90px"
          @change="(val: any) => onOrderChange(row, val)"
        />
      </template>
    </Grid>
  </Page>
</template>
