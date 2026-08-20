import axios from './axios';

// 1. 发送验证码
export const sendCode = (content: string) => {
  return axios.post('/lg/sdcode', null, {
    params: { content }
  });
};

// 2. 手机号登录/注册
export const phoneLogin = (phone: string, code: string) => {
  return axios.post('/lg/ph', null, {
    params: { phone, code }
  });
};

// 3. 邮箱登录/注册
export const emailLogin = (email: string, code: string) => {
  return axios.post('/lg/em', null, {
    params: { email, code }
  });
};

// 4. 账密登录
export const pwLogin = (username: string, password: string) => {
  return axios.post('/lg/pw', null, {
    params: { username, password }
  });
};

// 5. 个人信息设置
export const setUserInfo = (data: any) => {
  return axios.post('/ur/infoset', data);
};

// 6. 退出登录
export const logout = () => {
  return axios.post('/lg/lgout');
}
