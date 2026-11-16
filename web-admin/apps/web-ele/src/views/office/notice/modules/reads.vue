<script lang="ts" setup>
import type { OfficeNoticeApi } from '#/api/office/notice';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  ElAlert,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { getNoticeReadStat, getNoticeReads } from '#/api/office/notice';

const notice = ref<null | OfficeNoticeApi.Notice>(null);
const stat = ref<OfficeNoticeApi.ReadStat>({ readCount: 0, totalUsers: 0 });
const reads = ref<OfficeNoticeApi.NoticeRead[]>([]);
const loading = ref(false);

const [Modal, modalApi] = useVbenModal({
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    notice.value = modalApi.getData<OfficeNoticeApi.Notice>();
    loading.value = true;
    try {
      const [statResult, readList] = await Promise.all([
        getNoticeReadStat(notice.value!.id),
        getNoticeReads(notice.value!.id),
      ]);
      stat.value = statResult;
      reads.value = readList;
    } finally {
      loading.value = false;
    }
  },
});

onMounted(() => {});
</script>

<template>
  <Modal :title="`已读情况 - ${notice?.title ?? ''}`" class="w-[720px]">
    <ElAlert
      class="mb-3"
      :closable="false"
      show-icon
      :title="`已读 ${stat.readCount} / 范围内共 ${stat.totalUsers} 人（未读 ${Math.max(stat.totalUsers - stat.readCount, 0)} 人）`"
      type="info"
    />
    <ElTable v-loading="loading" :data="reads" max-height="400">
      <ElTableColumn label="姓名" prop="nickname" width="140" />
      <ElTableColumn label="部门" prop="deptName" min-width="140" />
      <ElTableColumn label="阅读时间" prop="readAt" width="180" />
    </ElTable>
  </Modal>
</template>
