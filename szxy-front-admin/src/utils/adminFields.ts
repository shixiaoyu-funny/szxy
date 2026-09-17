/**
 * 读取表格行字段（优先小驼峰；兼容旧 snake_case 响应）。
 */
export function pick<T = unknown>(row: Record<string, unknown> | null | undefined, camel: string, snake: string): T | undefined {
  if (row == null) return undefined;
  const a = row[camel];
  const b = row[snake];
  return (a !== undefined && a !== null ? a : b) as T | undefined;
}

export function farmerApplyBizText(type: unknown): string {
  const t = Number(type);
  const m: Record<number, string> = { 1: '民宿经营者', 2: '农产品销售者', 3: '文旅服务者' };
  return m[t] || (t ? `类型${t}` : '—');
}

export const VILLAGE_TYPE_MAP: Record<number, string> = {
  1: '古建聚落型',
  2: '非遗民俗型',
  3: '山水生态型',
  4: '农业观光型',
  5: '近郊休闲型',
  6: '康养度假型',
  7: '红色研学型',
  8: '滨水渔乡型',
  9: '民族村寨型',
  10: '综合文旅型'
};

export const VILLAGE_TYPE_OPTIONS = Object.entries(VILLAGE_TYPE_MAP).map(([value, label]) => ({
  value: Number(value),
  label
}));

export function villageTypeText(type: unknown): string {
  const t = Number(type);
  return VILLAGE_TYPE_MAP[t] || '—';
}

export function userRoleText(role: unknown): string {
  const r = Number(role);
  const m: Record<number, string> = { 1: '游客', 2: '农户', 3: '村长', 4: '管理员' };
  return m[r] || (r ? `角色${r}` : '—');
}

export function userStatusText(status: unknown): { text: string; type: 'success' | 'danger' | 'info' } {
  const s = Number(status);
  if (s === 1) return { text: '正常', type: 'success' };
  if (s === 0) return { text: '禁用', type: 'danger' };
  return { text: s ? `状态${s}` : '—', type: 'info' };
}

export function scenicTypeText(type: unknown): string {
  const t = Number(type);
  const m: Record<number, string> = { 1: '自然景观', 2: '人文景观', 3: '娱乐体验', 4: '民俗体验' };
  return m[t] || '—';
}

export function formatScenicPrice(price: unknown, empty = '—'): string {
  if (price == null || price === '') return empty;
  const n = Number(price);
  if (Number.isNaN(n)) return empty;
  if (n === 0) return '免费';
  return `¥${n}`;
}
