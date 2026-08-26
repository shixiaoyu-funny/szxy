import axios from './axios';

/** 申请成为本村村长 */
export const applyVgHead = () => axios.post('/fmr/vghead/apply');

/** 本人最新申请 */
export const getMyVgHeadAccess = () => axios.get('/fmr/vghead/mine');

/** 本人全部申请 */
export const getMyVgHeadList = () => axios.get('/fmr/vghead/my-list');

/** 申请详情 */
export const getVgHeadDetail = (id: number) => axios.get(`/fmr/vghead/${id}`);

/** 村长：待审列表 */
export const getPendingChiefList = () => axios.get('/fmr/vghead/pending-chief');

/** 村长：待审数量 */
export const getPendingChiefCount = () => axios.get('/fmr/vghead/pending-chief/count');

/** 村长：通过 */
export const chiefApproveVgHead = (id: number) => axios.post(`/fmr/vghead/${id}/chief-approve`);

/** 村长：拒绝 */
export const chiefRejectVgHead = (id: number) => axios.post(`/fmr/vghead/${id}/chief-reject`);
