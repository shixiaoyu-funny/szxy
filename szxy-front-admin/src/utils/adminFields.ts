/**
 * 读取表格行字段（优先小驼峰；兼容旧 snake_case 响应）。
 */
export function pick<T = unknown>(row: Record<string, unknown> | null | undefined, camel: string, snake: string): T | undefined {
  if (row == null) return undefined;
  const a = row[camel];
  const b = row[snake];
  return (a !== undefined && a !== null ? a : b) as T | undefined;
}

export function formatAuditStatus(status: unknown): { text: string; type: 'info' | 'success' | 'warning' | 'danger' } {
  const s = Number(status);
  if (s === 1) return { text: '已通过', type: 'success' };
  if (s === 2) return { text: '已拒绝', type: 'danger' };
  return { text: '待审核', type: 'warning' };
}

export function farmerRoleText(type: unknown): string {
  const t = Number(type);
  if (t === 2) return '村长';
  if (t === 1) return '农户';
  return t ? `类型${t}` : '—';
}

export function farmerApplyBizText(type: unknown): string {
  const t = Number(type);
  const m: Record<number, string> = { 1: '民宿经营者', 2: '农产品销售者', 3: '文旅服务者' };
  return m[t] || (t ? `类型${t}` : '—');
}

export function villageTypeText(type: unknown): string {
  const t = Number(type);
  const m: Record<number, string> = { 1: '古村落', 2: '生态村', 3: '民俗村', 4: '文旅村' };
  return m[t] || '—';
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
