<script lang="ts" setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  ElCard,
  ElCol,
  ElEmpty,
  ElRow,
  ElTag,
} from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

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
    description: '角色权限配置',
    icon: 'lucide:shield-check',
    path: '/system/role',
    title: '角色管理',
  },
  {
    description: '动态菜单与路由配置',
    icon: 'lucide:menu',
    path: '/system/menu',
    title: '菜单管理',
  },
];

const upcoming = [
  { badge: '规划中', text: '审批中心：请假 / 报销 / 采购审批' },
  { badge: '规划中', text: '公告通知：全员公告与已读统计' },
  { badge: '规划中', text: '考勤打卡：班次规则与月度报表' },
  { badge: '规划中', text: '日程与文档：个人日程、知识库' },
];

function go(path: string) {
  router.push(path);
}
</script>

<template>
  <Page>
    <ElCard class="mb-4" shadow="never">
      <div class="flex items-center gap-4">
        <div class="text-2xl font-semibold">
          {{ greeting }}，{{ realName }}
        </div>
        <ElTag type="success" size="small">easy-oa 已就绪</ElTag>
      </div>
      <div class="mt-2 text-gray-500">
        欢迎使用 Easy OA 办公系统。审批中心、行政办公、考勤等模块将陆续上线。
      </div>
    </ElCard>

    <ElRow :gutter="16" class="mb-4">
      <ElCol v-for="entry in quickEntries" :key="entry.path" :span="6">
        <ElCard
          shadow="hover"
          class="cursor-pointer"
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

    <ElCard shadow="never">
      <template #header>
        <div class="font-medium">即将上线</div>
      </template>
      <div v-if="upcoming.length" class="space-y-3">
        <div
          v-for="item in upcoming"
          :key="item.text"
          class="flex items-center gap-3"
        >
          <ElTag size="small" type="info">{{ item.badge }}</ElTag>
          <span>{{ item.text }}</span>
        </div>
      </div>
      <ElEmpty v-else description="敬请期待" />
    </ElCard>
  </Page>
</template>
