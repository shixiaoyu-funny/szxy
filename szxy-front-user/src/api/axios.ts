import axios from 'axios';
import { useUserStore } from '../stores/user';
import router from '../router';

/** 仅提取后端返回的 message，不构造前端文案 */
export function getErrorMessage(err: unknown): string {
  if (err instanceof Error && err.message) return err.message;
  if (typeof err === 'object' && err !== null) {
    const e = err as {
      message?: string;
      response?: { data?: { message?: string } };
    };
    if (e.response?.data?.message) return e.response.data.message;
    if (e.message) return e.message;
  }
  return '';
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['access_token'] = `${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  response => {
    const res = response.data;
    // 后端约定：业务错误 HTTP 仍为 200，靠 body 里的 code 区分
    if (res && typeof res === 'object' && res.code !== undefined && res.code !== 200) {
      if (res.code === 401) {
        const userStore = useUserStore();
        userStore.logout();
        router.push('/login');
      }
      return Promise.reject(new Error(res.message ?? ''));
    }
    return res;
  },
  error => {
    console.error('API Error:', error);
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore();
      userStore.logout();
      router.push('/login');
    }
    return Promise.reject(new Error(getErrorMessage(error)));
  }
);

export default instance;
