/** 民政部国家地名信息库 — 行政区划搜索（前端直连，CORS *） */
import taiwanRoot from '../data/taiwanRegions.json';

const XZQH_BASE = 'https://dmfw.mca.gov.cn/9095/xzqh/getList';

export const TAIWAN_PROVINCE_CODE = '710000000000';

export interface XzqhNode {
  code: string;
  name: string;
  level?: number;
  type?: string;
  children?: XzqhNode[];
}

interface XzqhResponse {
  status?: number | string;
  message?: string;
  data?: XzqhNode | XzqhNode[] | null;
  total?: number;
}

const taiwanTree = taiwanRoot as XzqhNode;

function findTaiwanNode(code: string): XzqhNode | null {
  if (!code || code === TAIWAN_PROVINCE_CODE) return taiwanTree;
  const city = (taiwanTree.children || []).find((c) => c.code === code);
  if (city) return city;
  for (const c of taiwanTree.children || []) {
    const d = (c.children || []).find((x) => x.code === code);
    if (d) return d;
  }
  return null;
}

/** 台湾省本地树：省码返回市/县；市码返回区；县无下级 */
function fetchTaiwanChildren(code: string): XzqhNode[] {
  const node = findTaiwanNode(code || TAIWAN_PROVINCE_CODE);
  if (!node) return [];
  // 请求省码时返回二级；请求市码时返回区；请求区/县码时返回空
  if (!code || code === TAIWAN_PROVINCE_CODE) {
    return (node.children || []).filter((n) => n && n.name);
  }
  return (node.children || []).filter((n) => n && n.name);
}

async function fetchMcaChildren(code = '', maxLevel = 1): Promise<XzqhNode[]> {
  const params = new URLSearchParams();
  params.set('maxLevel', String(maxLevel));
  params.set('code', code || '');

  const res = await fetch(`${XZQH_BASE}?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`地名接口请求失败 HTTP ${res.status}`);
  }
  const json = (await res.json()) as XzqhResponse;
  const status = Number(json.status);
  if (status && status !== 200) {
    throw new Error(json.message || '地名接口返回异常');
  }
  const data = json.data;
  if (!data) return [];
  if (Array.isArray(data)) return data.filter((n) => n && n.name);
  return (data.children || []).filter((n) => n && n.name);
}

/**
 * 拉取行政区划子节点。
 * code 为空：全国省级（合并台湾省）；71 开头走本地静态树。
 */
export async function fetchXzqhChildren(code = '', maxLevel = 1): Promise<XzqhNode[]> {
  if (code.startsWith('71')) {
    return fetchTaiwanChildren(code);
  }

  const list = await fetchMcaChildren(code, maxLevel);

  // 省级列表补入台湾省（民政部接口不含）
  if (!code) {
    const hasTw = list.some((n) => n.code === TAIWAN_PROVINCE_CODE || n.name === '台湾省');
    if (!hasTw) {
      list.push({
        code: TAIWAN_PROVINCE_CODE,
        name: taiwanTree.name,
        level: 1,
        type: taiwanTree.type || '省',
        children: []
      });
    }
  }

  return list;
}
