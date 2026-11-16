<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OfficeNoticeApi } from '#/api/office/notice';

import { Page, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteNotice,
  getNoticePage,
  publishNotice,
  toggleNoticeTop,
  withdrawNotice,
} from '#/api/office/notice';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import Reads from './modules/reads.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [ReadsModal, readsModalApi] = useVbenModal({
  connectedComponent: Reads,
  destroyOnClose: true,
});

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onEdit(row: OfficeNoticeApi.Notice) {
  formDrawerApi.setData(row).open();
}

function onReads(row: OfficeNoticeApi.Notice) {
  readsModalApi.setData(row).open();
}

async function onPublish(row: OfficeNoticeApi.Notice) {
  await ElMessageBox.confirm(`确认发布公告「${row.title}」吗？`, '发布公告', {
    type: 'warning',
  });
  await publishNotice(row.id);
  ElMessage.success('已发布');
  gridApi.query();
}

async function onWithdraw(row: OfficeNoticeApi.Notice) {
  await ElMessageBox.confirm(
    `确认撤回公告「${row.title}」吗？撤回后员工端不再展示，可编辑后重新发布。`,
    '撤回公告',
    { type: 'warning' },
  );
  await withdrawNotice(row.id);
  ElMessage.success('已撤回');
  gridApi.query();
}

async function onToggleTop(row: OfficeNoticeApi.Notice) {
  await toggleNoticeTop(row.id);
  ElMessage.success(row.top === 1 ? '已取消置顶' : '已置顶');
  gridApi.query();
}

async function onDelete(row: OfficeNoticeApi.Notice) {
  await ElMessageBox.confirm(
    `确认删除公告「${row.title}」吗？删除后已读记录一并清除。`,
    '删除公告',
    { type: 'warning' },
  );
  await deleteNotice(row.id);
  ElMessage.success('删除成功');
  gridApi.query();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<OfficeNoticeApi.Notice>) {
  switch (code) {
    case 'delete': {
      onDelete(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEdit(row);
      break;
    }
    case 'publish': {
      onPublish(row).catch(() => {});
      break;
    }
    case 'reads': {
      onReads(row);
      break;
    }
    case 'top': {
      onToggleTop(row).catch(() => {});
      break;
    }
    case 'withdraw': {
      onWithdraw(row).catch(() => {});
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
          return await getNoticePage({
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
    <FormDrawer @success="gridApi.query()" />
    <ReadsModal />
    <Grid table-title="公告管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OFFICE_PERMISSIONS.noticeCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增公告
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
