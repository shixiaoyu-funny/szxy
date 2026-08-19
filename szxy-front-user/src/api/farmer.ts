import axios from './axios';

// 本村农户列表（村长）
export const getVillageFarmers = () => {
  return axios.get('/fmr/ls');
};

// 村长新增本村农户
export const addVillageFarmer = (villageId: number, data: any) => {
  return axios.post('/fmr/new', data, {
    params: { village_id: villageId }
  });
};

// 村长修改本村农户
export const updateVillageFarmer = (id: number, data: any) => {
  return axios.post(`/fmr/modify/${id}`, data);
};

// 村长删除本村农户
export const deleteVillageFarmer = (id: number) => {
  return axios.post(`/fmr/remote/${id}`);
};

// 我的村（农户/村长）
export const getMyVillage = () => {
  return axios.get('/fmr/vlg');
};

// 我的景点（农户/村长）
export const getMyScenics = () => {
  return axios.get('/fmr/sc');
};

// ===== 管理端 =====

// 全部农户列表
export const adminGetAllFarmers = () => {
  return axios.get('/fmr/all');
};

// 建档农户（任意村，默认密码 123456）
export const adminCreateFarmer = (villageId: number, data: any) => {
  return axios.post('/fmr/create', data, {
    params: { village_id: villageId }
  });
};

// 任命/更换村长
export const adminSetManager = (villageId: number, farmerUserId: number) => {
  return axios.post('/fmr/set-manager', null, {
    params: { village_id: villageId, farmer_user_id: farmerUserId }
  });
};
