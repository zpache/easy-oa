<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, ref } from 'vue';

import { AuthenticationLogin, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { ElButton, ElDialog } from 'element-plus';

import { useRouter } from 'vue-router';

import {
  getWechatQrLoginUrl,
  wechatScanLogin,
} from '#/api/auth/wechat';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const router = useRouter();

/**
 * 对接 easy-oa-admin 真实接口：
 * - 去掉了 vben 演示用的账号下拉与假密码
 * - 只保留账号密码登录 + 微信扫码登录（mock 模式弹确认框；真实模式开窗展示二维码）
 */
const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
  ];
});

/* ==================== 微信扫码登录 ==================== */

const qrVisible = ref(false);
const qrLoading = ref(false);
const qrUrl = ref('');
const qrMock = ref(false);
const currentState = ref('');

async function onWechatLogin() {
  qrLoading.value = true;
  try {
    const qr = await getWechatQrLoginUrl();
    currentState.value = qr.state;
    qrMock.value = qr.mock;
    qrUrl.value = qr.url;
    if (qr.mock) {
      qrVisible.value = true;
    } else {
      // 真实模式：弹窗内 iframe 展示微信二维码，扫码确认后微信 302 回回调页
      qrVisible.value = true;
    }
  } finally {
    qrLoading.value = false;
  }
}

/** mock 模式：模拟「用户已扫码确认」 */
async function onMockConfirm() {
  const result = await wechatScanLogin({ code: 'mock', state: currentState.value });
  if (result.result === 'LOGIN' && result.login?.accessToken) {
    qrVisible.value = false;
    await authStore.loginByToken(result.login.accessToken);
    return;
  }
  // 未绑定：带一次性票据去回调页走绑定引导（票据已由登录接口返回，无需再消费 state）
  qrVisible.value = false;
  await router.push({
    path: '/auth/wechat/callback',
    query: { bindTicket: result.bindTicket ?? '', wxNickname: result.wxNickname ?? '' },
  });
}

function onIframeLoad(event: Event) {
  // 扫码确认后微信把 iframe 内导航到我们的回调页（同源），从其地址解析 code/state
  const iframe = event.target as HTMLIFrameElement;
  try {
    const href = iframe.contentWindow?.location.href ?? '';
    if (!href.includes('/auth/wechat/callback')) {
      return;
    }
    const search = new URL(href).searchParams;
    const code = search.get('code') ?? '';
    const state = search.get('state') ?? '';
    if (state && code) {
      qrVisible.value = false;
      router.push({ path: '/auth/wechat/callback', query: { code, state } });
    }
  } catch {
    // 跨域阶段（微信域名）无法读取，忽略
  }
}
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-code-login="false"
    :show-forget-password="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    @submit="authStore.authLogin"
  >
    <template #third-party-login>
      <ElButton
        :loading="qrLoading"
        class="mt-3 w-full"
        size="large"
        variant="outline"
        @click="onWechatLogin"
      >
        微信扫码登录
      </ElButton>
    </template>
  </AuthenticationLogin>

  <ElDialog
    v-model="qrVisible"
    :title="qrMock ? '微信扫码登录（mock 模式）' : '微信扫码登录'"
    width="460px"
    @close="qrVisible = false"
  >
    <template v-if="qrMock">
      <p class="mb-4 text-sm text-gray-500">
        当前为 mock 模式：跳过微信扫码，以固定测试身份
        「微信测试用户」走登录/绑定全流程。
      </p>
      <ElButton class="w-full" type="primary" @click="onMockConfirm">
        模拟已扫码确认
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
</template>
