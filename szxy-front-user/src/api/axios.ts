import axios from 'axios';
import { useUserStore } from '../stores/user';
import router from '../router';

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
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  error => {
    console.error('API Error:', error);
    // 处理 HTTP 非 2xx（拦截器级 401/403 等）
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore();
      userStore.logout();
      router.push('/login');
    }
    if (error.response && error.response.status === 403) {
      return Promise.reject(new Error(error.response.data?.message || '权限不足'));
    }
    return Promise.reject(error);
  }
);

export default instance;