import { requestClient } from '#/api/request';

export namespace SchEventApi {
  /** 日历区间返回条目（重复日程已展开为虚拟场次） */
  export interface Event {
    allDay: number;
    color?: null | string;
    done?: number;
    endTime: string;
    id: number;
    location?: null | string;
    mine: boolean;
    ownerName?: null | string;
    remindMinutes?: null | number;
    remark?: null | string;
    repeatType: string;
    scope: 'dept' | 'personal';
    startTime: string;
    title: string;
    type: 1 | 2;
    /** 1 虚拟展开场次（编辑按母本整条编辑） */
    virtual: number;
  }

  export interface EventPayload {
    allDay?: number;
    color?: string;
    endTime: string;
    location?: string;
    remindMinutes?: null | number;
    remark?: string;
    repeatType?: string;
    repeatUntil?: string;
    scope?: 'dept' | 'personal';
    startTime: string;
    title: string;
    type: 1 | 2;
  }
}

/** 区间查询（ISO 起止，重复日程已展开） */
async function getEventRange(start: string, end: string, type?: number) {
  return requestClient.get<SchEventApi.Event[]>('/schedule/events', {
    params: { end, start, type },
  });
}

/** 今日日程/待办（工作台） */
async function getTodayEvents() {
  return requestClient.get<SchEventApi.Event[]>('/schedule/events/today');
}

async function createEvent(data: SchEventApi.EventPayload) {
  return requestClient.post<{ id: number }>('/schedule/events', data);
}

async function updateEvent(id: number, data: SchEventApi.EventPayload) {
  return requestClient.put(`/schedule/events/${id}`, data);
}

async function deleteEvent(id: number) {
  return requestClient.delete(`/schedule/events/${id}`);
}

async function toggleEventDone(id: number) {
  return requestClient.put(`/schedule/events/${id}/done`);
}

export {
  createEvent,
  deleteEvent,
  getEventRange,
  getTodayEvents,
  toggleEventDone,
  updateEvent,
};
