<script lang="ts" setup>
import { computed } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

import { $t } from '#/locales';

// 登录页右侧品牌插画（智能办公主题，透明背景，适配明暗主题）
import sloganImage from '#/assets/images/oa-slogan.png';

const appName = computed(() => preferences.app.name);
const logo = computed(() => preferences.logo.source);
const logoDark = computed(() => preferences.logo.sourceDark);
</script>

<template>
  <AuthPageLayout
    class="oa-auth-layout"
    :app-name="appName"
    :logo="logo"
    :logo-dark="logoDark"
    :page-description="$t('authentication.pageDesc')"
    :page-title="$t('authentication.pageTitle')"
    :slogan-image="sloganImage"
  >
    <!-- 自定义工具栏 -->
    <!-- <template #toolbar></template> -->
  </AuthPageLayout>
</template>

<style scoped>
/* 默认样式把插画限制在 h-64 / w-2/5，会把 3:2 的图压扁；这里改为按宽度自适应 */
.oa-auth-layout :deep(img.animate-float) {
  width: 68%;
  max-width: 640px;
  height: auto;
}
</style>
