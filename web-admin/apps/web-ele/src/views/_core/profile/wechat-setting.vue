<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { ElButton, ElDialog, ElMessage, ElMessageBox } from 'element-plus';

import {
  getWechatBinding,
  getWechatQrBindUrl,
  wechatBindConfirm,
  wechatUnbind,
} from '#/api/auth/wechat';
import type { WechatBinding } from '#/api/auth/wechat';

defineOptions({ name: 'ProfileWechatSetting' });

const binding = ref<WechatBinding | null>(null);
const loading = ref(false);
const qrVisible = ref(false);
const qrMock = ref(false);
const qrUrl = ref('');
const currentState = ref('');

async function loadBinding() {
  loading.value = true;
  try {
    binding.value = await getWechatBinding();
  } finally {
    loading.value = false;
  }
}
onMounted(() => loadBinding());

async function onBind() {
  const qr = await getWechatQrBindUrl();
  currentState.value = qr.state;
  qrMock.value = qr.mock;
  qrUrl.value = qr.url;
  qrVisible.value = true;
}

async function onMockConfirm() {
  const result = await wechatBindConfirm({ code: 'mock', state: currentState.value });
  qrVisible.value = false;
  if (result.bound) {
    ElMessage.success('微信绑定成功');
    await loadBinding();
  }
}

function onIframeLoad(event: Event) {
  const iframe = event.target as HTMLIFrameElement;
  try {
    const href = iframe.contentWindow?.location.href ?? '';
    if (!href.includes('/auth/wechat/callback')) {
      return;
    }
    const search = new URL(href).searchParams;
    const code = search.get('code') ?? '';
    const state = search.get('state') ?? '';
    if (!state || !code) {
      return;
    }
    // 回调页只负责登录场景；绑定场景直接在此消费
    qrVisible.value = false;
    wechatBindConfirm({ code, state })
      .then((result) => {
        if (result.bound) {
          ElMessage.success('微信绑定成功');
          return loadBinding();
        }
        return undefined;
      })
      .catch(() => {});
  } catch {
    // 跨域阶段（微信域名）无法读取，忽略
  }
}

async function onUnbind() {
  await ElMessageBox.confirm(
    '解绑后将无法使用微信扫码登录，确认解绑吗？',
    '解绑微信',
    { type: 'warning' },
  );
  await wechatUnbind();
  ElMessage.success('已解绑');
  await loadBinding();
}
</script>

<template>
  <div class="space-y-4 p-4">
    <div class="flex items-center justify-between rounded-lg border p-4">
      <template v-if="binding?.bound">
        <div class="flex items-center gap-3">
          <img
            v-if="binding.wxAvatar"
            :src="binding.wxAvatar"
            class="size-10 rounded-full"
          />
          <div>
            <p class="text-sm font-medium">{{ binding.wxNickname ?? '微信用户' }}</p>
            <p class="text-xs text-gray-400">
              绑定时间：{{ binding.boundAt?.replace('T', ' ').slice(0, 19) ?? '-' }}
            </p>
          </div>
        </div>
        <ElButton type="danger" plain @click="onUnbind">解绑微信</ElButton>
      </template>
      <template v-else>
        <div>
          <p class="text-sm font-medium">未绑定微信</p>
          <p class="text-xs text-gray-400">
            绑定后可在登录页使用微信扫码登录，无需再输入账号密码
          </p>
        </div>
        <ElButton type="primary" :loading="loading" @click="onBind">绑定微信</ElButton>
      </template>
    </div>

    <p class="text-xs text-gray-400">
      绑定与解绑记录可在「日志审计 - 登录日志」中查看；一个微信号只能绑定一个账号。
    </p>

    <ElDialog
      v-model="qrVisible"
      :title="qrMock ? '绑定微信（mock 模式）' : '绑定微信'"
      width="460px"
    >
      <template v-if="qrMock">
        <p class="mb-4 text-sm text-gray-500">
          当前为 mock 模式：跳过微信扫码，直接以固定测试身份完成绑定。
        </p>
        <ElButton class="w-full" type="primary" @click="onMockConfirm">
          模拟已扫码确认绑定
        </ElButton>
      </template>
      <template v-else>
        <iframe
          v-if="qrUrl"
          :src="qrUrl"
          class="h-[420px] w-full border-0"
          @load="onIframeLoad"
        ></iframe>
      </template>
    </ElDialog>
  </div>
</template>
