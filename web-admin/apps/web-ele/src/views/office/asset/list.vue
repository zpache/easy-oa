<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OfficeAssetApi } from '#/api/office/asset';

import { Page, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteAsset,
  getAssetPage,
  lostAsset,
  returnAsset,
  scrapAsset,
} from '#/api/office/asset';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import ActionModal from './modules/action.vue';
import FormDrawer from './modules/form.vue';
import RecordsModal from './modules/records.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [ActionModalComp, actionModalApi] = useVbenModal({
  connectedComponent: ActionModal,
  destroyOnClose: true,
});

const [RecordsModalComp, recordsModalApi] = useVbenModal({
  connectedComponent: RecordsModal,
  destroyOnClose: true,
});

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onEdit(row: OfficeAssetApi.Asset) {
  formDrawerApi.setData(row).open();
}

function onAssign(row: OfficeAssetApi.Asset) {
  actionModalApi.setData({ asset: row, type: 'assign' }).open();
}

function onTransfer(row: OfficeAssetApi.Asset) {
  actionModalApi.setData({ asset: row, type: 'transfer' }).open();
}

function onRecords(row: OfficeAssetApi.Asset) {
  recordsModalApi.setData(row).open();
}

async function onReturn(row: OfficeAssetApi.Asset) {
  await ElMessageBox.confirm(
    `确认归还资产「${row.name}」（使用人：${row.holderName ?? '-'}）吗？`,
    '归还资产',
    { type: 'warning' },
  );
  await returnAsset(row.id);
  ElMessage.success('已归还');
  gridApi.query();
}

async function onScrap(row: OfficeAssetApi.Asset) {
  await ElMessageBox.confirm(
    `确认报废资产「${row.name}」吗？报废后不可恢复、不可再编辑或领用。`,
    '报废资产',
    { type: 'warning' },
  );
  await scrapAsset(row.id);
  ElMessage.success('已报废');
  gridApi.query();
}

async function onLost(row: OfficeAssetApi.Asset) {
  await ElMessageBox.confirm(
    `确认将资产「${row.name}」登记为丢失吗？`,
    '丢失登记',
    { type: 'warning' },
  );
  await lostAsset(row.id);
  ElMessage.success('已登记丢失');
  gridApi.query();
}

async function onDelete(row: OfficeAssetApi.Asset) {
  await ElMessageBox.confirm(
    `确认删除资产「${row.name}」吗？流转记录将一并清除。`,
    '删除资产',
    { type: 'warning' },
  );
  await deleteAsset(row.id);
  ElMessage.success('删除成功');
  gridApi.query();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<OfficeAssetApi.Asset>) {
  switch (code) {
    case 'assign': {
      onAssign(row);
      break;
    }
    case 'delete': {
      onDelete(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEdit(row);
      break;
    }
    case 'lost': {
      onLost(row).catch(() => {});
      break;
    }
    case 'records': {
      onRecords(row);
      break;
    }
    case 'return': {
      onReturn(row).catch(() => {});
      break;
    }
    case 'scrap': {
      onScrap(row).catch(() => {});
      break;
    }
    case 'transfer': {
      onTransfer(row);
      break;
    }
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getAssetPage({
            category: formValues?.category,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions,
});
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <ActionModalComp @success="gridApi.query()" />
    <RecordsModalComp />
    <Grid table-title="资产管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OFFICE_PERMISSIONS.assetCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增资产
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
