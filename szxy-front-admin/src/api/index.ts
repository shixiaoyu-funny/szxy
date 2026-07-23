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
      config.headers['Authorization'] = `${store.state.user.token}`;
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
    return Promise.reject(error);
  }
);

/** 报表（对应 ReportController `/report`） */
export const reportApi = {
  /** 农户总数 */
  getFarmCount: (): Promise<number> => service.get('/report/farm'),
  /** 当日 UV */
  getUv: (): Promise<number> => service.get('/report/uv'),
  /** 当日 PV */
  getPv: (): Promise<number> => service.get('/report/pv'),
  /** UV/PV 比值（粘性） */
  getUvpv: (): Promise<number> => service.get('/report/uvpv'),
  /** 当日新增景区（HyperLogLog） */
  getNewScenicCount: (): Promise<number> => service.get('/report/scenic'),
  /** 当日新增农村（HyperLogLog，接口名 newFarmCnt 为后端历史命名） */
  getNewVillageCount: (): Promise<number> => service.get('/report/village'),
  /** 近 7 天 PV（索引 0 为当天，依次往前一日） */
  getPv7: (): Promise<number[]> => service.get('/report/pv7'),
  /** 近 7 天 UV（索引 0 为当天，依次往前一日） */
  getUv7: (): Promise<number[]> => service.get('/report/uv7')
};

export const loginApi = {
  sendCode: (content: string): Promise<string> =>
    service.post('/common/login/sendcode', null, { params: { content } }),
  phoneLogin: (phone: string, code: string): Promise<string> =>
    service.post('/common/login/phone_login', null, { params: { phone, code } }),
  emailLogin: (email: string, code: string): Promise<string> =>
    service.post('/common/login/email_login', null, { params: { email, code } }),
  pwLogin: (username: string, password: string): Promise<string> =>
    service.post('/common/login/pw_login', null, { params: { username, password } }),
  infoSet: (data: Record<string, unknown>): Promise<unknown> => service.post('/common/login/info_set', data),
  logout: (): Promise<unknown> => service.post('/common/login/logout')
};

export const userApi = {
  getInfo: (): Promise<Record<string, unknown>> => service.get('/user/info')
};

/** 景点资质申请列表：ScenicController GET `/scenic/list` */
export const scenicApi = {
  getScenicAccessList: (): Promise<unknown[]> => service.get('/scenic/list')
};

/** 农村管理：VillageController `/admin/village` */
export const villageApi = {
  getVillageList: (pageResultDTO: { pageNo: number; pageSize: number }): Promise<{ total: number; data: unknown[] }> =>
    service.get('/admin/village/list', { params: pageResultDTO }),
  addVillage: (body: Record<string, unknown>): Promise<unknown> => service.post('/admin/village', body),
  updateVillage: (id: number, body: Record<string, unknown>): Promise<unknown> => service.put(`/admin/village/${id}`, body),
  deleteVillage: (id: number): Promise<unknown> => service.delete(`/admin/village/${id}`)
};

/**
 * 管理端农户与资质审批：AdminFarmerController `/admin/farmer`
 * 说明：本村增删改农户为村长端能力（`/farmer`），管理端仅做全量列表与审批。
 */
export const farmerApi = {
  getFarmers: (): Promise<unknown[]> => service.get('/admin/farmer/list'),
  getFarmerAccessList: (): Promise<unknown[]> => service.get('/admin/farmer/farmer_access'),
  getManagerAccessList: (): Promise<unknown[]> => service.get('/admin/farmer/manager_access'),
  /** status: 1 通过, 2 拒绝（与后端 solve* 一致） */
  processFarmerAccess: (id: number, status: number): Promise<unknown> =>
    service.post(`/admin/farmer/farmer_access/${id}`, null, { params: { status } }),
  processManagerAccess: (id: number, status: number): Promise<unknown> =>
    service.post(`/admin/farmer/manager_access/${id}`, null, { params: { status } }),
  processScenicAccess: (id: number, status: number): Promise<unknown> =>
    service.post(`/admin/farmer/scenic_access/${id}`, null, { params: { status } })
};

export const uploadApi = {
  uploadFile: (data: FormData): Promise<string> =>
    service.post('/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
};
