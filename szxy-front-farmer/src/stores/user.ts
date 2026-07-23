import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    isLoggedIn: !!localStorage.getItem('token'),
    userInfo: null as any,
    token: localStorage.getItem('token') || ''
  }),
  getters: {
    getUserInfo: (state) => state.userInfo,
    getToken: (state) => state.token
  },
  actions: {
    setUserInfo(info: any) {
      this.userInfo = info;
      this.isLoggedIn = true;
    },
    setToken(token: string) {
      this.token = token;
      this.isLoggedIn = true;
      localStorage.setItem('token', token);
    },
    logout() {
      this.userInfo = null;
      this.isLoggedIn = false;
      this.token = '';
      localStorage.removeItem('token');
    }
  }
});