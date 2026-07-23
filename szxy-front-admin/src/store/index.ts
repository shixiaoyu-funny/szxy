import { reactive, readonly } from 'vue';

interface State {
  user: {
    token: string | null;
    info: any;
  };
  loading: boolean;
}

const state = reactive<State>({
  user: {
    token: localStorage.getItem('token') || null,
    info: null
  },
  loading: false
});

const mutations = {
  setToken(token: string | null) {
    state.user.token = token;
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  },
  setUserInfo(info: any) {
    state.user.info = info;
  },
  setLoading(loading: boolean) {
    state.loading = loading;
  }
};

const actions = {
  login(token: string, info?: any) {
    mutations.setToken(token);
    if (info) {
      mutations.setUserInfo(info);
    }
  },
  logout() {
    mutations.setToken(null);
    mutations.setUserInfo(null);
  },
  updateUserInfo(info: any) {
    mutations.setUserInfo(info);
  }
};

export default {
  state: readonly(state),
  mutations,
  actions
};