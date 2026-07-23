import axios from './axios';

// 获取用户信息
export const getUserInfo = () => {
  return axios.get('/user/info');
};

// 获取用户位置
export const getUserLocation = () => {
  return axios.get('/user/location');
};

// 用户评论
export const userComment = (data: any) => {
  return axios.post('/user/comment', data, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
};

// 用户点赞
export const userLike = (id: number) => {
  return axios.post(`/user/like/${id}`);
};

// 用户收藏
export const userCollect = (id: number) => {
  return axios.post(`/user/collect/${id}`);
};

// 检查用户是否点赞
export const isLike = (id: number) => {
  return axios.post('/user/isLike', null, {
    params: { id }
  });
};

// 检查用户是否收藏
export const isCollect = (id: number) => {
  return axios.post('/user/isCollect', null, {
    params: { id }
  });
};