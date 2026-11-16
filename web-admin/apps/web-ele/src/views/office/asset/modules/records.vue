<script lang="ts" setup>
import type { OfficeAssetApi } from '#/api/office/asset';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElEmpty, ElTable, ElTableColumn, ElTag } from 'element-plus';

import { getAssetRecords } from '#/api/office/asset';

import { ACTION_TEXT, STATUS_TEXT } from '../data';

const asset = ref<null | OfficeAssetApi.Asset>(null);
const records = ref<OfficeAssetApi.Record[]>([]);
const loading = ref(false);

const ACTION_TAG: Record<string, 'danger' | 'info' | 'success' | 'warning'> = {
  assign: 'success',
  lost: 'danger',
  return: 'info',
  scrap: 'danger',
  transfer: 'warning',
};

async function load() {
  if (!asset.value?.id) {
    return;
  }
  loading.value = true;
  try {
    records.value = (await getAssetRecords(asset.value.id)) ?? [];
  } finally {
    loading.value = false;
  }
}

const [Modal, modalApi] = useVbenModal({
  onOpenChange(isOpen) {
    if (!isOpen) {
      records.value = [];
      return;
    }
    asset.value = modalApi.getData<OfficeAssetApi.Asset>();
    load();
  },
});
</script>

<template>
  <Modal :title="`流转记录：${asset?.name ?? ''}`" class="w-[760px]">
    <ElTable v-loading="loading" :data="records" size="small">
      <ElTableColumn label="动作" width="100">
        <template #default="{ row }">
          <ElTag :type="ACTION_TAG[row.action] ?? 'info'" size="small">
            {{ ACTION_TEXT[row.action] ?? row.action }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="涉及人" width="120">
        <template #default="{ row }">
          {{ row.targetUserName ?? '-' }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="流转后状态" width="100">
        <template #default="{ row }">
          {{ STATUS_TEXT[row.assetStatus] ?? row.assetStatus ?? '-' }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作人" prop="operatorName" width="120" />
      <ElTableColumn label="备注" prop="remark" min-width="180" />
      <ElTableColumn label="时间" prop="createdAt" width="170" />
    </ElTable>
    <ElEmpty v-if="!loading && records.length === 0" description="暂无流转记录" :image-size="80" />
  </Modal>
</template>
