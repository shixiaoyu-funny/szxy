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

// 搜索农村（多字段模糊+分页）
export const searchVillage = (content?: string, pageNo?: number, pageSize?: number) => {
  return axios.get('/ur/search', {
    params: { content, page_no: pageNo, page_size: pageSize }
  });
};
