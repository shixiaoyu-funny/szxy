import axios from './axios';

// 获取用户信息
export const getUserInfo = () => {
  return axios.get('/ur/info');
};

// 用户评论
export const userComment = (data: any) => {
  return axios.post('/ur/comment', data, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
};

// 用户点赞/取消
export const userLike = (id: number) => {
  return axios.post(`/ur/like/${id}`);
};

// 用户收藏/取消
export const userCollect = (id: number) => {
  return axios.post(`/ur/collect/${id}`);
};

// 检查用户是否点赞
export const isLike = (id: number) => {
  return axios.post('/ur/isLike', null, {
    params: { id }
  });
};

// 检查用户是否收藏
export const isCollect = (id: number) => {
  return axios.post('/ur/isCollect', null, {
    params: { id }
  });
};

// 我赞过的景点
export const getMyLikes = () => {
  return axios.get('/ur/like');
};

// 我评论过的景点
export const getMyComments = () => {
  return axios.get('/ur/comment');
};

// 我收藏的景点
export const getMyCollections = () => {
  return axios.get('/ur/collection');
};

// 搜索（type: 1-农村，2-景点）
export const searchContent = (
  content?: string,
  type: number = 1,
  pageNo?: number,
  pageSize?: number
) => {
  return axios.get('/ur/search', {
    params: { content, type, pageNo, pageSize }
  });
};

/** @deprecated 请使用 searchContent(content, 1, ...) */
export const searchVillage = (content?: string, pageNo?: number, pageSize?: number) =>
  searchContent(content, 1, pageNo, pageSize);

export const searchScenic = (content?: string, pageNo?: number, pageSize?: number) =>
  searchContent(content, 2, pageNo, pageSize);

/** 收到的点赞动态（消息页） */
export const getLikesReceived = () => axios.get('/ur/msg/likes-received');
