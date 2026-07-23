import axios from './axios';

// 优质农村（点赞 TOP10）
export const getTopVillageByLikes = () => {
  return axios.get('/admin/village/likes');
};

// 优质农村（收藏 TOP10）
export const getTopVillageByCollections = () => {
  return axios.get('/admin/village/collections');
};