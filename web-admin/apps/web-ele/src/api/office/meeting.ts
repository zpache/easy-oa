import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OfficeMeetingApi {
  export interface MeetingRoom {
    [key: string]: any;
    capacity?: number;
    equipment?: string;
    id: number;
    location?: string;
    name: string;
    remark?: string;
    status: 0 | 1;
  }

  export interface RoomPayload {
    capacity?: number;
    equipment?: string;
    location?: string;
    name: string;
    remark?: string;
    status?: number;
  }

  export interface Booking {
    [key: string]: any;
    createdAt?: string;
    endAt: string;
    id: number;
    participants?: string;
    roomId: number;
    roomName?: string;
    startAt: string;
    /** 展示状态：后端按时间推导，cancelled/ended/ongoing/upcoming */
    state?: 'cancelled' | 'ended' | 'ongoing' | 'upcoming';
    status: 0 | 1;
    title: string;
    userId: number;
    userName?: string;
  }

  export interface BookingPayload {
    endAt: string;
    participants?: string;
    roomId: number;
    startAt: string;
    title: string;
  }
}

/**
 * 会议室列表（含停用，前端自行过滤）
 */
async function getRoomList() {
  return requestClient.get<OfficeMeetingApi.MeetingRoom[]>(
    '/office/meeting/room/list',
  );
}

async function createRoom(data: OfficeMeetingApi.RoomPayload) {
  return requestClient.post('/office/meeting/room', data);
}

async function updateRoom(id: number, data: OfficeMeetingApi.RoomPayload) {
  return requestClient.put(`/office/meeting/room/${id}`, data);
}

async function deleteRoom(id: number) {
  return requestClient.delete(`/office/meeting/room/${id}`);
}

/**
 * 预约单分页
 */
async function getBookingPage(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/office/meeting/booking/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createBooking(data: OfficeMeetingApi.BookingPayload) {
  return requestClient.post('/office/meeting/booking', data);
}

async function cancelBooking(id: number) {
  return requestClient.put(`/office/meeting/booking/${id}/cancel`);
}

export {
  cancelBooking,
  createBooking,
  createRoom,
  deleteRoom,
  getBookingPage,
  getRoomList,
  updateRoom,
};
