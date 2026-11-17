import { requestClient } from '#/api/request';

/** 微信授权链接响应 */
export interface WechatQrUrl {
  /** true = mock 模式（前端弹模拟扫码确认框，不渲染真实二维码） */
  mock: boolean;
  state: string;
  url: string;
}

/** 扫码登录结果 */
export interface WechatLoginResult {
  bindTicket?: null | string;
  login?: null | {
    accessToken: string;
    expiresIn: number;
    refreshToken: string;
    tokenType: string;
  };
  result: 'LOGIN' | 'NEED_BIND';
  wxNickname?: null | string;
}

/** 登录响应结构（与密码登录一致） */
export interface WechatLoginVO {
  accessToken: string;
  expiresIn: number;
  refreshToken: string;
  tokenType: string;
}

/** 微信绑定信息 */
export interface WechatBinding {
  bound: boolean;
  boundAt?: null | string;
  wxAvatar?: null | string;
  wxNickname?: null | string;
}

/** 扫码登录授权链接 */
async function getWechatQrLoginUrl() {
  return requestClient.post<WechatQrUrl>('/auth/wechat/qr-login-url');
}

/** 扫码回调登录 */
async function wechatScanLogin(data: { code: string; state: string }) {
  return requestClient.post<WechatLoginResult>('/auth/wechat/login', data);
}

/** 绑定引导：账号密码确认绑定并直接登录 */
async function wechatBind(data: {
  bindTicket: string;
  password: string;
  username: string;
}) {
  return requestClient.post<WechatLoginVO>('/auth/wechat/bind', data);
}

/** 我的微信绑定信息 */
async function getWechatBinding() {
  return requestClient.get<WechatBinding>('/auth/wechat/binding');
}

/** 个人中心绑定用授权链接 */
async function getWechatQrBindUrl() {
  return requestClient.post<WechatQrUrl>('/auth/wechat/binding/qr-bind-url');
}

/** 个人中心绑定确认 */
async function wechatBindConfirm(data: { code: string; state: string }) {
  return requestClient.post<WechatBinding>('/auth/wechat/binding/confirm', data);
}

/** 解绑微信 */
async function wechatUnbind() {
  return requestClient.delete<WechatBinding>('/auth/wechat/binding');
}

export {
  getWechatBinding,
  getWechatQrBindUrl,
  getWechatQrLoginUrl,
  wechatBind,
  wechatBindConfirm,
  wechatScanLogin,
  wechatUnbind,
};
