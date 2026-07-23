import axios from './axios';

// 1. 发送验证码
export const sendCode = (content: string) => {
  return axios.post('/common/login/sendcode', null, {
    params: { content }
  });
};

// 2. 手机号登录/注册
export const phoneLogin = (phone: string, code: string) => {
  return axios.post('/common/login/phone_login', null, {
    params: { phone, code }
  });
};

// 3. 邮箱登录/注册
export const emailLogin = (email: string, code: string) => {
  return axios.post('/common/login/email_login', null, {
    params: { email, code }
  });
};

// 4. 账密登录
export const pwLogin = (username: string, password: string) => {
  return axios.post('/common/login/pw_login', null, {
    params: { username, password }
  });
};

// 5. 个人信息设置
export const setUserInfo = (data: any) => {
  return axios.post('/common/login/info_set', data);
};

// 6. 退出登录
export const logout = () => {
  return axios.post('/common/login/logout');
}
