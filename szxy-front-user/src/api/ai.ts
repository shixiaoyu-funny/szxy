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
  /** 用户上传图片 URL 列表 */
  mediaUrls?: string[] | null;
  createTime?: string;
}

export interface AiChatBody {
  content?: string;
  mediaUrls?: string[];
}

export interface AnonymousHistoryMsg {
  role: number;
  content: string;
  mediaUrls?: string[];
}

export interface AnonymousChatBody {
  content?: string;
  mediaUrls?: string[];
  history?: AnonymousHistoryMsg[];
}

export const newSession = () => axios.post('/ai/new');

export const getSessionsByUid = () => axios.get('/ai/gssByUID');

export const getChatsBySid = (sessionId: number) => axios.get(`/ai/gcBySID/${sessionId}`);

/** 发送消息（文本 + 可选图片 URL） */
export const sendChat = (sessionId: number, body: AiChatBody) =>
  axios.post(`/ai/chat/${sessionId}`, body, {
    timeout: 120000,
  });

/** 游客匿名聊天（不落库） */
export const sendChatAnonymous = (body: AnonymousChatBody) =>
  axios.post('/ai/chat/anonymous', body, {
    timeout: 120000,
  });

/** 修改会话名称（侧栏 simpleDesc） */
export const renameSession = (sessionId: number, simpleDesc: string) =>
  axios.post(`/ai/rename/${sessionId}`, { simpleDesc });
