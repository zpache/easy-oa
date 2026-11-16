<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteLeaveType,
  getBalances,
  getLeaveTypes,
} from '#/api/attendance/attendance';
import { ATTENDANCE_PERMISSIONS } from '#/constants/permissions';

import TypeFormDrawer from './modules/type-form.vue';
import AdjustModal from './modules/adjust.vue';

const [TypeFormDrawerComp, typeFormDrawerApi] = useVbenDrawer({
  connectedComponent: TypeFormDrawer,
  destroyOnClose: true,
});

const [AdjustModalComp, adjustModalApi] = useVbenModal({
  connectedComponent: AdjustModal,
  destroyOnClose: true,
});

/* -------- 假期类型 -------- */
const types = ref<any[]>([]);
const typesLoading = ref(false);

async function loadTypes() {
  typesLoading.value = true;
  try {
    types.value = await getLeaveTypes();
  } finally {
    typesLoading.value = false;
  }
}

function onCreateType() {
  typeFormDrawerApi.setData(null).open();
}

function onEditType(row: any) {
  typeFormDrawerApi.setData(row).open();
}

async function onDeleteType(row: any) {
  await ElMessageBox.confirm(
    `确认删除假期类型「${row.name}」吗？有余额引用时不可删除。`,
    '删除假期类型',
    { type: 'warning' },
  );
  await deleteLeaveType(row.id);
  ElMessage.success('删除成功');
  loadTypes();
}

/* -------- 员工余额 -------- */
function onAdjust() {
  adjustModalApi.setData(null).open();
}

const [BalanceGrid, balanceGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'userName', title: '员工', minWidth: 110 },
      {
        field: 'deptName',
        formatter: ({ row }) => row.deptName ?? '-',
        minWidth: 120,
        title: '部门',
      },
      { field: 'typeName', title: '假期类型', width: 120 },
      { field: 'totalQuota', title: '总额度（天）', width: 120 },
      { field: 'usedQuota', title: '已用（天）', width: 110 },
      {
        field: 'remainQuota',
        formatter: ({ row }) => `${row.remainQuota}`,
        title: '剩余（天）',
        width: 110,
      },
    ],
    height: 420,
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          return await getBalances({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
          });
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
  } as VxeTableGridOptions,
});

onMounted(() => {
  loadTypes();
});
</script>

<template>
  <Page>
    <ElCard shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">假期类型</span>
          <ElButton
            v-access:code="ATTENDANCE_PERMISSIONS.leaveTypeCreate"
            type="primary"
            @click="onCreateType"
          >
            <Plus class="size-5" />
            新增类型
          </ElButton>
        </div>
      </template>
      <ElTable v-loading="typesLoading" :data="types" row-key="id">
        <ElTableColumn label="名称" prop="name" width="140" />
        <ElTableColumn label="说明" prop="remark" min-width="180" />
        <ElTableColumn label="默认额度（天/年）" prop="defaultQuota" width="160" />
        <ElTableColumn label="状态" width="90">
          <template #default="{ row }">
            <ElTag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn width="150">
          <template #default="{ row }">
            <ElButton
              v-access:code="ATTENDANCE_PERMISSIONS.leaveTypeUpdate"
              link
              type="primary"
              @click="onEditType(row)"
            >
              编辑
            </ElButton>
            <ElButton
              v-access:code="ATTENDANCE_PERMISSIONS.leaveTypeDelete"
              link
              type="danger"
              @click="onDeleteType(row)"
            >
              删除
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElCard class="mt-4" shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">员工假期余额</span>
          <ElButton
            v-access:code="ATTENDANCE_PERMISSIONS.balanceAdjust"
            type="primary"
            @click="onAdjust"
          >
            设定额度
          </ElButton>
        </div>
      </template>
      <BalanceGrid />
    </ElCard>

    <TypeFormDrawerComp @success="loadTypes()" />
    <AdjustModalComp @success="balanceGridApi.query()" />
  </Page>
</template>
