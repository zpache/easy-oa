import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OfficeAssetApi {
  export interface Asset {
    [key: string]: any;
    assetNo: string;
    brandModel?: string;
    category: string;
    createdAt?: string;
    holderName?: string;
    holderUserId?: number;
    id: number;
    location?: string;
    name: string;
    price?: number;
    purchaseDate?: string;
    remark?: string;
    status: 0 | 1 | 3 | 4;
  }

  export interface AssetPayload {
    assetNo: string;
    brandModel?: string;
    category: string;
    location?: string;
    name: string;
    price?: number;
    purchaseDate?: string;
    remark?: string;
  }

  export interface Record {
    action: string;
    assetStatus?: number;
    createdAt: string;
    id: number;
    operatorName?: string;
    remark?: string;
    targetUserId?: number;
    targetUserName?: string;
  }
}

/* ==================== 台账 ==================== */

async function getAssetPage(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/office/asset/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createAsset(data: OfficeAssetApi.AssetPayload) {
  return requestClient.post<{ id: number }>('/office/asset', data);
}

async function updateAsset(id: number, data: OfficeAssetApi.AssetPayload) {
  return requestClient.put(`/office/asset/${id}`, data);
}

async function deleteAsset(id: number) {
  return requestClient.delete(`/office/asset/${id}`);
}

/* ==================== 流转 ==================== */

/** 领用 / 转移 */
async function assignAsset(id: number, userId: number, remark?: string) {
  return requestClient.post(`/office/asset/${id}/assign`, { remark, userId });
}

async function transferAsset(id: number, userId: number, remark?: string) {
  return requestClient.post(`/office/asset/${id}/transfer`, { remark, userId });
}

/** 归还（可仅备注） */
async function returnAsset(id: number, remark?: string) {
  return requestClient.post(`/office/asset/${id}/return`, { remark });
}

/** 报废 / 丢失登记 */
async function scrapAsset(id: number, remark?: string) {
  return requestClient.put(`/office/asset/${id}/scrap`, { remark });
}

async function lostAsset(id: number, remark?: string) {
  return requestClient.put(`/office/asset/${id}/lost`, { remark });
}

/** 流转记录 */
async function getAssetRecords(id: number) {
  return requestClient.get<OfficeAssetApi.Record[]>(
    `/office/asset/${id}/records`,
  );
}

export {
  assignAsset,
  createAsset,
  deleteAsset,
  getAssetPage,
  getAssetRecords,
  lostAsset,
  returnAsset,
  scrapAsset,
  transferAsset,
  updateAsset,
};
