<script lang="ts" setup>
import type { OfficeNoticeApi } from '#/api/office/notice';

import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  ElButton,
  ElCard,
  ElCol,
  ElEmpty,
  ElRow,
  ElTag,
} from 'element-plus';

import { getPortalNoticePage } from '#/api/office/notice';

const router = useRouter();
const userStore = useUserStore();
const accessStore = useAccessStore();

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const realName = computed(() => userStore.userInfo?.realName || '同事');

const quickEntries = [
  {
    description: '员工账号、角色与菜单授权',
    icon: 'lucide:users',
    path: '/system/user',
    title: '员工管理',
  },
  {
    description: '维护公司组织架构',
    icon: 'lucide:building-2',
    path: '/system/dept',
    title: '部门管理',
  },
  {
    description: '发布全员/部门公告',
    icon: 'lucide:megaphone',
    path: '/office/notice',
    title: '公告管理',
  },
  {
    description: '会议室档案与预约',
    icon: 'lucide:calendar-clock',
    path: '/office/meeting-booking',
    title: '会议室预约',
  },
];

const upcoming = [
  { badge: '规划中', text: '审批中心：对接 easy-bpm 审批中台（OpenAPI）' },
  { badge: '规划中', text: '考勤打卡：班次规则与月度报表' },
  { badge: '规划中', text: '日程与文档：个人日程、知识库' },
];

const canNotice = computed(() =>
  (accessStore.accessCodes ?? []).includes('office:notice:portal'),
);
const notices = ref<OfficeNoticeApi.PortalNotice[]>([]);
const unreadNoticeCount = computed(
  () => notices.value.filter((n) => !n.read).length,
);

async function loadNotices() {
  if (!canNotice.value) {
    return;
  }
  try {
    const result = await getPortalNoticePage({ pageNum: 1, pageSize: 5 });
    notices.value = result.items ?? [];
  } catch {
    // 公告中心不可达时静默
  }
}

function go(path: string) {
  router.push(path);
}

onMounted(() => {
  loadNotices();
});
</script>

<template>
  <Page>
    <ElCard class="mb-4" shadow="never">
      <div class="flex items-center gap-4">
        <div class="text-2xl font-semibold">
          {{ greeting }}，{{ realName }}
        </div>
        <ElTag size="small" type="success">easy-oa 已就绪</ElTag>
      </div>
      <div class="mt-2 text-gray-500">
        欢迎使用 Easy OA 办公系统。审批中心、考勤等模块将陆续上线。
      </div>
    </ElCard>

    <ElRow :gutter="16" class="mb-4">
      <ElCol v-for="entry in quickEntries" :key="entry.path" :span="6">
        <ElCard
          class="cursor-pointer"
          shadow="hover"
          @click="go(entry.path)"
        >
          <div class="flex items-center gap-3">
            <IconifyIcon :icon="entry.icon" class="text-2xl" />
            <div>
              <div class="font-medium">{{ entry.title }}</div>
              <div class="text-xs text-gray-500">{{ entry.description }}</div>
            </div>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElRow :gutter="16">
      <!-- 最新公告：让员工登录第一眼看到公司的通知 -->
      <ElCol :lg="14" :span="24">
        <ElCard shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2 font-medium">
                <IconifyIcon class="text-lg" icon="lucide:megaphone" />
                最新公告
                <ElTag v-if="unreadNoticeCount > 0" size="small" type="danger">
                  {{ unreadNoticeCount }} 条未读
                </ElTag>
              </div>
              <ElButton
                v-if="canNotice"
                link
                type="primary"
                @click="go('/office/notice-center')"
              >
                进入公告中心
              </ElButton>
            </div>
          </template>
          <div v-if="notices.length" class="space-y-1">
            <div
              v-for="item in notices"
              :key="item.id"
              class="flex cursor-pointer items-center gap-3 rounded px-2 py-2 hover:bg-gray-50 dark:hover:bg-gray-800"
              @click="go('/office/notice-center')"
            >
              <span
                v-if="!item.read"
                class="size-2 shrink-0 rounded-full bg-red-500"
              />
              <span v-else class="size-2 shrink-0" />
              <ElTag v-if="item.top === 1" size="small" type="danger">
                置顶
              </ElTag>
              <span class="truncate">{{ item.title }}</span>
              <span class="ml-auto shrink-0 text-xs text-gray-400">
                {{ (item.publishAt ?? '').slice(0, 10) }}
              </span>
            </div>
          </div>
          <ElEmpty v-else description="暂无公告" :image-size="80" />
        </ElCard>
      </ElCol>

      <ElCol :lg="10" :span="24">
        <ElCard shadow="never" class="h-full">
          <template #header>
            <div class="font-medium">即将上线</div>
          </template>
          <div class="space-y-3">
            <div
              v-for="item in upcoming"
              :key="item.text"
              class="flex items-center gap-3"
            >
              <ElTag size="small" type="info">{{ item.badge }}</ElTag>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>
  </Page>
</template>
