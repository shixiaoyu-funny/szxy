import axios from 'axios';
import store from '../store';
import router from '../router';

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
});

service.interceptors.request.use(
  (config) => {
    if (store.state.user.token) {
      config.headers['access_token'] = `${store.state.user.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401) {
        store.actions.logout();
        router.push('/login');
      }
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      store.actions.logout();
      router.push('/login');
    }
    if (error.response?.status === 403) {
      return Promise.reject(new Error(error.response.data?.message || '权限不足：仅管理员可访问'));
    }
    return Promise.reject(error);
  }
);

/** 报表（对应 ReportController `/report`，AdminInterceptor 拦截 role=4） */
export const reportApi = {
  /** 农户总数 */
  getFarmCount: (): Promise<number> => service.get('/report/farm'),
  /** 当日 UV */
  getUv: (): Promise<number> => service.get('/report/uv'),
  /** 当日 PV */
  getPv: (): Promise<number> => service.get('/report/pv'),
  /** UV/PV 比值（粘性） */
  getUvpv: (): Promise<number> => service.get('/report/uvpv'),
  /** 当日新增景区（DB 按 create_time 计数） */
  getNewScenicCount: (): Promise<number> => service.get('/report/scenic'),
  /** 当日新增农村 */
  getNewVillageCount: (): Promise<number> => service.get('/report/village'),
  /** 近 7 天 PV（索引 0 为当天，依次往前一日） */
  getPv7: (): Promise<number[]> => service.get('/report/pv7'),
  /** 近 7 天 UV（索引 0 为当天，依次往前一日） */
  getUv7: (): Promise<number[]> => service.get('/report/uv7')
};

/** 登录（`/lg`，管理端仅账密登录） */
export const loginApi = {
  pwLogin: (username: string, password: string): Promise<string> =>
    service.post('/lg/pw', null, { params: { username, password } }),
  logout: (): Promise<unknown> => service.post('/lg/lgout')
};

/** 用户（`/ur`） */
export const userApi = {
  getInfo: (): Promise<Record<string, unknown>> => service.get('/ur/info'),
  infoSet: (data: Record<string, unknown>): Promise<unknown> => service.post('/ur/infoset', data)
};

/** 景点管理（`/sc`，Service 层 requireAdmin 校验 role=4） */
export const scenicApi = {
  /** 分页全部景点 */
  getList: (pageResultDTO: { pageNo: number; pageSize: number }): Promise<{ total: number; data: unknown[] }> =>
    service.get('/sc/ls', { params: pageResultDTO }),
  /** 新增景点到指定村落 */
  add: (villageId: number, body: Record<string, unknown>): Promise<unknown> =>
    service.post('/sc/new', body, { params: { village_id: villageId } }),
  /** 修改景点 */
  update: (id: number, body: Record<string, unknown>): Promise<unknown> =>
    service.post(`/sc/modify/${id}`, body),
  /** 删除景点 */
  remove: (id: number): Promise<unknown> =>
    service.post(`/sc/del/${id}`)
};

/** 农村管理（`/vlg`，Service 层 requireAdmin 校验 role=4） */
export const villageApi = {
  getVillageList: (pageResultDTO: { pageNo: number; pageSize: number }): Promise<{ total: number; data: unknown[] }> =>
    service.get('/vlg/ls', { params: pageResultDTO }),
  addVillage: (body: Record<string, unknown>): Promise<unknown> => service.post('/vlg/new', body),
  updateVillage: (id: number, body: Record<string, unknown>): Promise<unknown> => service.post(`/vlg/modify/${id}`, body),
  deleteVillage: (id: number): Promise<unknown> => service.post(`/vlg/del/${id}`)
};

/** 农户管理（`/fmr`，Service 层 requireAdmin 校验 role=4） */
export const farmerApi = {
  /** 全部农户列表 */
  getFarmers: (): Promise<unknown[]> => service.get('/fmr/all'),
  /** 建档农户（任意村，默认密码 123456） */
  createFarmer: (villageId: number, body: Record<string, unknown>): Promise<unknown> =>
    service.post('/fmr/create', body, { params: { village_id: villageId } }),
  /** 任命/更换村长 */
  setManager: (villageId: number, farmerUserId: number): Promise<unknown> =>
    service.post('/fmr/set-manager', null, { params: { village_id: villageId, farmer_user_id: farmerUserId } })
};

export const uploadApi = {
  uploadFile: (data: FormData): Promise<string> =>
    service.post('/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
};
