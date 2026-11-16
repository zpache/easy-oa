<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { IconifyIcon } from '@vben/icons';
import { useAccessStore } from '@vben/stores';

import { ElBadge, ElButton, ElTooltip } from 'element-plus';

import { getPortalUnreadCount } from '#/api/office/notice';

/**
 * 顶栏公告铃铛：轮询 + 路由切换时刷新未读数，点击进入公告中心。
 * 仅授予了 office:notice:portal 权限的用户可见。
 */
const router = useRouter();
const route = useRoute();
const accessStore = useAccessStore();

const canNotice = computed(() =>
  (accessStore.accessCodes ?? []).includes('office:notice:portal'),
);
const unreadCount = ref(0);
let timer: undefined | ReturnType<typeof setInterval>;

async function refresh() {
  if (!canNotice.value) {
    return;
  }
  try {
    unreadCount.value = Number((await getPortalUnreadCount()) ?? 0);
  } catch {
    // 静默失败，不打扰用户
  }
}

watch(
  () => route.fullPath,
  (path, prev) => {
    // 进入公告中心（详情已读）或离开时都刷新一次
    if (prev?.startsWith('/office/notice-center') || path.startsWith('/office/notice-center')) {
      refresh();
    }
  },
);

onMounted(() => {
  refresh();
  timer = setInterval(refresh, 60_000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});

function toNoticeCenter() {
  router.push('/office/notice-center');
}
</script>

<template>
  <ElTooltip v-if="canNotice" content="公告通知" placement="bottom">
    <ElBadge
      :hidden="unreadCount === 0"
      :max="99"
      :value="unreadCount"
      class="mx-1"
    >
      <ElButton circle text @click="toNoticeCenter">
        <span class="relative">
          <IconifyIcon class="size-5" icon="lucide:bell" />
        </span>
      </ElButton>
    </ElBadge>
  </ElTooltip>
</template>
