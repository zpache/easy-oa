<script lang="ts" setup>
import type { OfficeNoticeApi } from '#/api/office/notice';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import {
  ElCard,
  ElDialog,
  ElEmpty,
  ElPagination,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import {
  getPortalNoticeDetail,
  getPortalNoticePage,
} from '#/api/office/notice';

const CATEGORY_TEXT: Record<string, string> = {
  activity: '活动',
  notice: '通知',
  policy: '制度',
};

const CATEGORY_TAG: Record<
  string,
  'danger' | 'info' | 'primary' | 'success' | 'warning'
> = {
  activity: 'warning',
  notice: 'primary',
  policy: 'success',
};

const loading = ref(false);
const list = ref<OfficeNoticeApi.PortalNotice[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const detailVisible = ref(false);
const detail = ref<Recordable<any>>({});
const detailLoading = ref(false);

async function load() {
  loading.value = true;
  try {
    const result = await getPortalNoticePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    list.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: OfficeNoticeApi.PortalNotice) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getPortalNoticeDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
  load();
}

function onDetailClosed() {
  detail.value = {};
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mx-auto max-w-4xl">
      <ElEmpty v-if="!loading && list.length === 0" description="暂无公告" />
      <ElTimeline v-else>
        <ElTimelineItem
          v-for="item in list"
          :key="item.id"
          :timestamp="item.publishAt ?? ''"
          :type="item.read ? 'info' : 'primary'"
        >
          <ElCard
            class="cursor-pointer"
            :class="{ 'opacity-60': item.read }"
            shadow="hover"
            @click="openDetail(item)"
          >
            <div class="flex items-center gap-3">
              <ElTag size="small" :type="CATEGORY_TAG[item.category] ?? 'primary'">
                {{ CATEGORY_TEXT[item.category] ?? item.category }}
              </ElTag>
              <span class="font-medium" :class="{ 'text-gray-500': item.read }">
                {{ item.title }}
              </span>
              <ElTag v-if="item.top === 1" type="danger" size="small">置顶</ElTag>
              <ElTag v-if="!item.read" effect="plain" size="small">未读</ElTag>
              <span
                v-if="item.expireAt"
                class="ml-auto shrink-0 text-xs text-gray-400"
              >
                有效期至 {{ item.expireAt }}
              </span>
            </div>
          </ElCard>
        </ElTimelineItem>
      </ElTimeline>

      <div class="mt-4 flex justify-center">
        <ElPagination
          v-model:current-page="pageNum"
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          @current-change="load"
        />
      </div>
    </div>

    <ElDialog
      v-model="detailVisible"
      :title="detail.title ?? '公告详情'"
      top="8vh"
      width="720px"
      @closed="onDetailClosed"
    >
      <div v-loading="detailLoading">
        <div class="mb-4 flex flex-wrap items-center gap-3 text-sm text-gray-500">
          <ElTag size="small" :type="CATEGORY_TAG[detail.category] ?? 'primary'">
            {{ CATEGORY_TEXT[detail.category] ?? detail.category }}
          </ElTag>
          <span>发布人：{{ detail.publisher ?? '-' }}</span>
          <span>发布时间：{{ detail.publishAt ?? '-' }}</span>
          <span>{{ detail.readCount ?? 0 }} 人已读</span>
        </div>
        <!-- 公告正文由管理端维护（富文本 HTML），员工端只读展示 -->
        <div class="notice-content leading-7" v-html="detail.content"></div>
      </div>
    </ElDialog>
  </Page>
</template>
