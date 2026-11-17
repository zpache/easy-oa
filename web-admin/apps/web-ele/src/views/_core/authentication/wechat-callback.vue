<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import { useRouter } from 'vue-router';

import { ElButton, ElMessage } from 'element-plus';

import { LOGIN_PATH } from '@vben/constants';

import { useAuthStore } from '#/store';
import { wechatBind, wechatScanLogin } from '#/api/auth/wechat';

defineOptions({ name: 'WechatCallback' });

const router = useRouter();
const authStore = useAuthStore();

const status = ref<'binding' | 'error' | 'loading' | 'success'>('loading');
const errorMsg = ref('');
/** NEED_BIND 时的绑定引导 */
const needBind = ref(false);
const bindTicket = ref('');
const wxNickname = ref('');
const username = ref('');
const password = ref('');
const bindLoading = ref(false);

async function finishLogin(token: string) {
  status.value = 'success';
  await authStore.loginByToken(token);
}

async function handleCallback() {
  const query = router.currentRoute.value.query;
  // 登录页/绑定弹窗直接携带票据进入（mock NEED_BIND 路径）：免消费 state，直接引导绑定
  if (query.bindTicket) {
    needBind.value = true;
    bindTicket.value = query.bindTicket as string;
    wxNickname.value = (query.wxNickname as string) ?? '';
    status.value = 'binding';
    return;
  }
  const code = (query.code as string) || '';
  const state = (query.state as string) || '';
  if (!state) {
    status.value = 'error';
    errorMsg.value = '缺少扫码回调参数，请从登录页重新扫码';
    return;
  }
  // mock 模式回调页（无真实微信跳转）：code 用 mock 占位
  const result = await wechatScanLogin({ code: code || 'mock', state });
  if (result.result === 'LOGIN' && result.login?.accessToken) {
    await finishLogin(result.login.accessToken);
    return;
  }
  needBind.value = true;
  bindTicket.value = result.bindTicket ?? '';
  wxNickname.value = result.wxNickname ?? '';
  status.value = 'binding';
}

async function onBind() {
  if (!username.value.trim() || !password.value) {
    ElMessage.error('请输入账号和密码');
    return;
  }
  bindLoading.value = true;
  try {
    const login = await wechatBind({
      bindTicket: bindTicket.value,
      password: password.value,
      username: username.value.trim(),
    });
    ElMessage.success('微信绑定成功，已自动登录');
    await finishLogin(login.accessToken);
  } finally {
    bindLoading.value = false;
  }
}

function backToLogin() {
  router.replace(LOGIN_PATH);
}

onMounted(() => {
  handleCallback().catch((e: any) => {
    status.value = 'error';
    errorMsg.value = e?.message || '微信登录失败，请重试';
  });
});
</script>

<template>
  <div class="flex w-full flex-col items-center justify-center py-16">
    <template v-if="status === 'loading'">
      <p class="text-base">微信登录中，请稍候…</p>
    </template>

    <template v-else-if="status === 'success'">
      <p class="text-base">登录成功，正在进入系统…</p>
    </template>

    <template v-else-if="status === 'binding'">
      <div class="w-full max-w-sm rounded-lg border p-6">
        <p class="mb-1 text-base font-medium">绑定微信账号</p>
        <p class="mb-4 text-sm text-gray-500">
          该微信{{ wxNickname ? `（${wxNickname}）` : '' }}尚未绑定系统账号，
          请输入已有账号密码完成绑定（绑定后可扫码登录）
        </p>
        <div class="flex flex-col gap-3">
          <input
            v-model="username"
            class="h-9 rounded border px-3 text-sm"
            placeholder="系统账号"
            @keyup.enter="onBind"
          />
          <input
            v-model="password"
            class="h-9 rounded border px-3 text-sm"
            placeholder="密码"
            type="password"
            @keyup.enter="onBind"
          />
          <ElButton :loading="bindLoading" type="primary" @click="onBind">
            确认绑定并登录
          </ElButton>
          <ElButton text @click="backToLogin">返回登录</ElButton>
        </div>
      </div>
    </template>

    <template v-else>
      <p class="mb-3 text-base text-red-500">{{ errorMsg }}</p>
      <ElButton type="primary" @click="backToLogin">返回登录页</ElButton>
    </template>
  </div>
</template>
