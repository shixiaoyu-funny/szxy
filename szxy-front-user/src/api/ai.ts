import axios from './axios';

export interface ChatSessionVO {
  id: number;
  userId: number;
  simpleDesc: string;
  createTime?: string;
  updateTime?: string;
}

export interface ChatMessageVO {
  id?: number;
  sessionId?: number;
  userId?: number;
  role: number | { code: number };
  content: string;
  createTime?: string;
}

export const newSession = () => axios.post('/ai/new');

export const getSessionsByUid = () => axios.get('/ai/gssByUID');

export const getChatsBySid = (sessionId: number) => axios.get(`/ai/gcBySID/${sessionId}`);

export const sendChat = (sessionId: number, content: string) =>
  axios.post(`/ai/chat/${sessionId}`, null, {
    params: { content },
    timeout: 120000,
  });
