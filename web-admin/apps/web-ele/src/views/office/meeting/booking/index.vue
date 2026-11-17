<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OfficeMeetingApi } from '#/api/office/meeting';

import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { computed, onMounted, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElDialog,
  ElDescriptions,
  ElDescriptionsItem,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTag,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  cancelBooking,
  getBookingPage,
  getRoomList,
} from '#/api/office/meeting';
import { usePermission } from '#/composables/use-permission';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

dayjs.locale('zh-cn');

/** 时间轴展示范围：08:00 - 21:00 */
const DAY_START = 8 * 60;
const DAY_END = 21 * 60;
/** 每分钟像素高度（30 分钟 = 24px 格线） */
const PX_PER_MIN = 0.8;
const BODY_HEIGHT = (DAY_END - DAY_START) * PX_PER_MIN;
const SLOT_MINUTES = 30;

const { can } = usePermission();
const canCancel = can(OFFICE_PERMISSIONS.bookingCancel);
const canCreate = can(OFFICE_PERMISSIONS.bookingCreate);

/* ==================== 会议室 ==================== */

const rooms = ref<OfficeMeetingApi.MeetingRoom[]>([]);
const roomId = ref<number>();
const activeRooms = computed(() => rooms.value.filter((room) => room.status === 1));
const currentRoom = computed(
  () => activeRooms.value.find((room) => room.id === roomId.value),
);

/* ==================== 日期范围 ==================== */

const viewMode = ref<'calendar' | 'list'>('calendar');
const span = ref<'day' | 'week'>('week');
const anchorDate = ref(dayjs().format('YYYY-MM-DD'));

/** 以周一作为一周开始（dayjs 默认按英文 locale 从周日开始，手动对齐国内习惯） */
function weekStart(d: dayjs.Dayjs) {
  return d.subtract((d.day() + 6) % 7, 'day');
}

const rangeStart = computed(() => {
  const d = dayjs(anchorDate.value);
  return span.value === 'day' ? d : weekStart(d);
});

const days = computed(() => {
  const count = span.value === 'day' ? 1 : 7;
  return Array.from({ length: count }, (_, i) => rangeStart.value.add(i, 'day'));
});

const rangeEnd = computed(() => days.value[days.value.length - 1]!.endOf('day'));

const rangeLabel = computed(() => {
  if (span.value === 'day') {
    return rangeStart.value.format('YYYY年MM月DD日 dddd');
  }
  return `${rangeStart.value.format('YYYY年MM月DD日')} ~ ${rangeEnd.value.format('MM月DD日')}`;
});

function shiftRange(offset: number) {
  const unit = span.value === 'day' ? 'day' : 'week';
  anchorDate.value = dayjs(anchorDate.value).add(offset, unit).format('YYYY-MM-DD');
}

function backToToday() {
  anchorDate.value = dayjs().format('YYYY-MM-DD');
}

/* ==================== 预约数据 ==================== */

const loading = ref(false);
const bookings = ref<OfficeMeetingApi.Booking[]>([]);

async function loadBookings() {
  if (!roomId.value) {
    bookings.value = [];
    return;
  }
  loading.value = true;
  try {
    const result = await getBookingPage({
      endDate: rangeEnd.value.format('YYYY-MM-DD 23:59:59'),
      pageNum: 1,
      pageSize: 500,
      roomId: roomId.value,
      startDate: rangeStart.value.format('YYYY-MM-DD 00:00:00'),
      status: 1,
    });
    bookings.value = result.items ?? [];
  } finally {
    loading.value = false;
  }
}

watch(roomId, () => loadBookings());
watch(span, () => loadBookings());
watch(anchorDate, () => loadBookings());

onMounted(async () => {
  rooms.value = await getRoomList();
  if (!roomId.value && activeRooms.value.length > 0) {
    roomId.value = activeRooms.value[0]!.id;
  }
  await loadBookings();
});

/* ==================== 日历布局 ==================== */

interface CalendarEvent {
  booking: OfficeMeetingApi.Booking;
  col: number;
  cols: number;
  endMin: number;
  startMin: number;
}

/**
 * 把预约单换算成当天的时间轴坐标（与展示窗口求交集，跨天会议裁剪显示）。
 */
function clipToDay(day: dayjs.Dayjs, booking: OfficeMeetingApi.Booking): null | CalendarEvent {
  const dayStart = day.startOf('day');
  const rawStart = dayjs(booking.startAt).diff(dayStart, 'minute');
  const rawEnd = dayjs(booking.endAt).diff(dayStart, 'minute');
  const startMin = Math.max(rawStart, DAY_START);
  const endMin = Math.min(rawEnd, DAY_END);
  if (endMin <= startMin || rawEnd <= 0 || rawStart >= 24 * 60) {
    return null;
  }
  return { booking, col: 0, cols: 1, endMin, startMin };
}

const dayColumns = computed(() => {
  return days.value.map((day) => {
    const events = bookings.value
      .map((booking) => clipToDay(day, booking))
      .filter((event): event is CalendarEvent => event !== null)
      .sort((a, b) => a.startMin - b.startMin || a.endMin - b.endMin);

    // 同一时间段并行的会议分组，组内再按列并排，避免卡片互相覆盖
    const groups: CalendarEvent[][] = [];
    let current: CalendarEvent[] = [];
    let currentEnd = -1;
    for (const event of events) {
      if (current.length > 0 && event.startMin >= currentEnd) {
        groups.push(current);
        current = [];
        currentEnd = -1;
      }
      current.push(event);
      currentEnd = Math.max(currentEnd, event.endMin);
    }
    if (current.length > 0) {
      groups.push(current);
    }
    for (const group of groups) {
      const colEnds: number[] = [];
      for (const event of group) {
        let col = colEnds.findIndex((end) => end <= event.startMin);
        if (col === -1) {
          col = colEnds.length;
          colEnds.push(event.endMin);
        } else {
          colEnds[col] = event.endMin;
        }
        event.col = col;
      }
      for (const event of group) {
        event.cols = colEnds.length;
      }
    }

    const minutes = events.reduce((sum, e) => sum + (e.endMin - e.startMin), 0);
    return {
      bookings: events,
      day,
      hours: Math.round((minutes / 60) * 10) / 10,
      isToday: day.isSame(dayjs(), 'day'),
    };
  });
});

const hourMarks = computed(() => {
  const marks: number[] = [];
  for (let h = DAY_START / 60; h <= DAY_END / 60; h++) {
    marks.push(h * 60);
  }
  return marks;
});

function eventStyle(event: CalendarEvent) {
  const height = Math.max((event.endMin - event.startMin) * PX_PER_MIN - 2, 18);
  return {
    height: `${height}px`,
    left: `calc(${(event.col / event.cols) * 100}% + 2px)`,
    top: `${(event.startMin - DAY_START) * PX_PER_MIN + 1}px`,
    width: `calc(${(1 / event.cols) * 100}% - 4px)`,
  };
}

/** 当前时刻指示线（今天且在时间轴范围内时显示） */
const nowLineTop = computed(() => {
  const now = dayjs();
  const minute = now.hour() * 60 + now.minute();
  if (minute < DAY_START || minute > DAY_END) {
    return null;
  }
  return (minute - DAY_START) * PX_PER_MIN;
});

/* ==================== 点击空白预约 ==================== */

function onColumnClick(e: MouseEvent, day: dayjs.Dayjs) {
  if (!canCreate) {
    return;
  }
  const target = e.currentTarget as HTMLElement;
  const offsetY = e.clientY - target.getBoundingClientRect().top;
  const minute =
    DAY_START +
    Math.floor(offsetY / PX_PER_MIN / SLOT_MINUTES) * SLOT_MINUTES;
  const startMin = Math.min(minute, DAY_END - 60);
  const endMin = startMin + 60;
  // 与已有会议重叠时拦截，直接提示
  const conflict = dayColumns.value.some(
    (column) =>
      column.day.isSame(day, 'day') &&
      column.bookings.some(
        (event) => event.startMin < endMin && event.endMin > startMin,
      ),
  );
  if (conflict) {
    ElMessage.warning('该时段已有会议，请选择空闲时间段');
    return;
  }
  formModalApi
    .setData({
      endAt: day.startOf('day').add(endMin, 'minute').format('YYYY-MM-DD HH:mm:ss'),
      roomId: roomId.value,
      startAt: day
        .startOf('day')
        .add(startMin, 'minute')
        .format('YYYY-MM-DD HH:mm:ss'),
    })
    .open();
}

/* ==================== 详情与取消 ==================== */

const detailVisible = ref(false);
const detailBooking = ref<null | OfficeMeetingApi.Booking>(null);

function openDetail(event: CalendarEvent) {
  detailBooking.value = event.booking;
  detailVisible.value = true;
}

async function onCancelFromDetail() {
  const booking = detailBooking.value;
  if (!booking) {
    return;
  }
  await ElMessageBox.confirm(
    `确认取消「${booking.roomName || currentRoom.value?.name || ''}」的「${booking.title}」预约吗？`,
    '取消预约',
    { type: 'warning' },
  );
  await cancelBooking(booking.id);
  ElMessage.success('预约已取消');
  detailVisible.value = false;
  await loadBookings();
  gridApi.query();
}

/* ==================== 预约抽屉 ==================== */

const [FormModal, formModalApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function onCreate() {
  formModalApi.setData(roomId.value ? { roomId: roomId.value } : null).open();
}

function onRefresh() {
  loadBookings();
  gridApi.query();
}

/* ==================== 列表视图（保留原表格） ==================== */

async function onCancel(row: OfficeMeetingApi.Booking) {
  await ElMessageBox.confirm(
    `确认取消「${row.roomName}」的「${row.title}」预约吗？`,
    '取消预约',
    { type: 'warning' },
  );
  await cancelBooking(row.id);
  ElMessage.success('预约已取消');
  onRefresh();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<OfficeMeetingApi.Booking>) {
  switch (code) {
    case 'cancel': {
      onCancel(row).catch(() => {});
      break;
    }
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getBookingPage({
            endDate: formValues?.endDate,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            roomId: formValues?.roomId,
            startDate: formValues?.startDate,
            status: formValues?.status,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions,
});

function fmtMinute(minute: number) {
  const h = Math.floor(minute / 60);
  const m = minute % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="onRefresh" />

    <!-- 日历视图 -->
    <div
      v-if="viewMode === 'calendar'"
      class="flex h-full min-h-0 flex-col rounded-md bg-card"
    >
      <!-- 工具条 -->
      <div class="flex flex-wrap items-center gap-2 border-b p-3">
        <ElSelect
          v-model="roomId"
          class="w-56"
          placeholder="选择会议室"
          size="default"
        >
          <ElOption
            v-for="room in activeRooms"
            :key="room.id"
            :label="room.name"
            :value="room.id"
          >
            <div class="flex items-center justify-between">
              <span>{{ room.name }}</span>
              <span class="text-xs text-gray-400">
                {{ room.capacity }}人{{ room.location ? ` · ${room.location}` : '' }}
              </span>
            </div>
          </ElOption>
        </ElSelect>

        <ElRadioGroup v-model="span" size="default">
          <ElRadioButton value="day">日</ElRadioButton>
          <ElRadioButton value="week">周</ElRadioButton>
        </ElRadioGroup>

        <div class="flex items-center gap-1">
          <ElButton size="default" @click="shiftRange(-1)">上一{{ span === 'day' ? '天' : '周' }}</ElButton>
          <ElButton size="default" @click="backToToday">今天</ElButton>
          <ElButton size="default" @click="shiftRange(1)">下一{{ span === 'day' ? '天' : '周' }}</ElButton>
        </div>

        <span class="text-sm font-medium">{{ rangeLabel }}</span>
        <span v-if="loading" class="text-xs text-gray-400">加载中…</span>

        <div class="flex-auto"></div>

        <ElRadioGroup v-model="viewMode" size="default">
          <ElRadioButton value="calendar">日历</ElRadioButton>
          <ElRadioButton value="list">列表</ElRadioButton>
        </ElRadioGroup>

        <ElButton
          v-access:code="OFFICE_PERMISSIONS.bookingCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          预约会议室
        </ElButton>
      </div>

      <!-- 当前会议室信息 -->
      <div
        v-if="currentRoom"
        class="flex items-center gap-3 border-b px-4 py-2 text-xs text-gray-500"
      >
        <span class="text-sm font-medium text-gray-800">
          {{ currentRoom.name }}
        </span>
        <span v-if="currentRoom.location">📍 {{ currentRoom.location }}</span>
        <span v-if="currentRoom.capacity">容纳 {{ currentRoom.capacity }} 人</span>
        <span v-if="currentRoom.equipment">设备：{{ currentRoom.equipment }}</span>
        <span class="flex-auto"></span>
        <span>点击空白时段可直接发起预约</span>
      </div>

      <!-- 时间轴网格 -->
      <div class="flex-1 overflow-auto">
        <div class="flex min-w-full">
          <!-- 左侧时间列 -->
          <div class="sticky left-0 z-20 w-14 shrink-0 border-r bg-card">
            <div class="sticky top-0 z-30 h-12 border-b bg-card"></div>
            <div class="relative" :style="{ height: `${BODY_HEIGHT}px` }">
              <div
                v-for="mark in hourMarks"
                :key="mark"
                :style="{
                  position: 'absolute',
                  top: `${(mark - DAY_START) * PX_PER_MIN}px`,
                  transform: 'translateY(-50%)',
                }"
                class="w-full pr-1 text-right text-xs text-gray-400"
              >
                {{ fmtMinute(mark) }}
              </div>
            </div>
          </div>

          <!-- 日期列 -->
          <div class="flex min-w-0 flex-1">
            <div
              v-for="column in dayColumns"
              :key="column.day.format('YYYY-MM-DD')"
              class="flex min-w-[110px] flex-1 flex-col border-l"
              :class="column.isToday ? 'bg-primary/5' : ''"
            >
              <!-- 日期头：占用统计 -->
              <div
                class="sticky top-0 z-10 flex h-12 w-full flex-col items-center justify-center border-b bg-card"
                :class="column.isToday ? 'text-primary' : ''"
              >
                <span class="text-xs">
                  {{ column.day.format('ddd MM/DD') }}
                  <span v-if="column.isToday" class="ml-1 font-medium">今天</span>
                </span>
                <span class="text-xs text-gray-400">
                  {{ column.bookings.length }} 场 · {{ column.hours }} 小时
                </span>
              </div>

              <!-- 时间格子区域 -->
              <div
                class="relative flex-1"
                :class="canCreate ? 'cursor-pointer' : ''"
                :style="{
                  height: `${BODY_HEIGHT}px`,
                  backgroundImage:
                    'repeating-linear-gradient(to bottom, transparent 0, transparent 23px, var(--el-border-color-lighter) 23px, var(--el-border-color-lighter) 24px)',
                }"
                @click="onColumnClick($event, column.day)"
              >
                <!-- 预约卡片 -->
                <div
                  v-for="event in column.bookings"
                  :key="event.booking.id"
                  class="absolute cursor-pointer overflow-hidden rounded border border-primary/40 bg-primary/10 px-1 py-0.5 hover:bg-primary/20"
                  :style="eventStyle(event)"
                  @click.stop="openDetail(event)"
                >
                  <div class="truncate text-xs font-medium leading-4">
                    {{ event.booking.title }}
                  </div>
                  <div
                    v-if="(event.endMin - event.startMin) * PX_PER_MIN > 40"
                    class="truncate text-xs leading-4 text-gray-500"
                  >
                    {{ fmtMinute(event.startMin) }}-{{ fmtMinute(event.endMin) }}
                    {{ event.booking.userName || '' }}
                  </div>
                </div>

                <!-- 当前时刻线 -->
                <div
                  v-if="column.isToday && nowLineTop !== null"
                  :style="{ top: `${nowLineTop}px` }"
                  class="pointer-events-none absolute left-0 z-10 w-full"
                >
                  <div class="h-0.5 bg-red-500"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图（保留原有表格） -->
    <Grid v-else table-title="会议室预约">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OFFICE_PERMISSIONS.bookingCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          预约会议室
        </ElButton>
      </template>
    </Grid>

    <!-- 预约详情 -->
    <ElDialog v-model="detailVisible" title="预约详情" width="480px">
      <ElDescriptions v-if="detailBooking" :column="1" border>
        <ElDescriptionsItem label="会议室">
          {{ detailBooking.roomName || currentRoom?.name }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="会议主题">
          {{ detailBooking.title }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="时间">
          {{ detailBooking.startAt }} ~ {{ detailBooking.endAt }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="预约人">
          {{ detailBooking.userName || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="参与人">
          {{ detailBooking.participants || '-' }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="状态">
          <ElTag :type="detailBooking.status === 1 ? 'success' : 'info'">
            {{ detailBooking.status === 1 ? '已预约' : '已取消' }}
          </ElTag>
        </ElDescriptionsItem>
      </ElDescriptions>
      <template #footer>
        <ElButton @click="detailVisible = false">关闭</ElButton>
        <ElButton
          v-if="(canCancel || canCreate) && detailBooking?.status === 1"
          type="danger"
          @click="onCancelFromDetail"
        >
          取消预约
        </ElButton>
      </template>
    </ElDialog>
  </Page>
</template>
