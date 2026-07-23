import axios from './axios';

/** 农家乐/私人景点上报（普通农户） POST /scenic/private_scenic */
export const registerPrivateScenic = (data: Record<string, unknown>) => {
  return axios.post('/scenic/private_scenic', data);
};

/** 景点/公共景点上报（村长） POST /scenic/public_scenic */
export const registerPublicScenic = (data: Record<string, unknown>) => {
  return axios.post('/scenic/public_scenic', data);
};
