/**
 * 高德 Web 服务 · IP 定位（精确到区县）
 * 流程：公网 IP → 高德 IP 定位 →（无区县时）矩形中心逆地理拿 district
 * Key：.env.local 的 VITE_AMAP_KEY（Web 服务类型）
 */

const IPIFY_URL = 'https://api.ipify.org?format=json';
const AMAP_IP_V5_URL = 'https://restapi.amap.com/v5/ip/location';
const AMAP_IP_V3_URL = 'https://restapi.amap.com/v3/ip';
const AMAP_REGEO_URL = 'https://restapi.amap.com/v3/geocode/regeo';

export interface IpLocationResult {
  ip: string;
  province: string;
  city: string;
  /** 区/县 */
  district: string;
  /** 展示文案，如「山西省·太原市·小店区」 */
  displayText: string;
}

interface IpifyResponse {
  ip?: string;
}

interface AmapIpV5Response {
  status?: string;
  info?: string;
  province?: string | string[];
  city?: string | string[];
  district?: string | string[];
}

interface AmapIpV3Response {
  status?: string;
  info?: string;
  province?: string | string[];
  city?: string | string[];
  rectangle?: string | string[];
}

interface AmapRegeoResponse {
  status?: string;
  info?: string;
  regeocode?: {
    addressComponent?: {
      province?: string | string[];
      city?: string | string[];
      district?: string | string[];
    };
  };
}

/** 高德空字段常返回 []，统一成字符串 */
function amapText(value: unknown): string {
  if (typeof value === 'string') return value.trim();
  return '';
}

function requireAmapKey(): string {
  const key = import.meta.env.VITE_AMAP_KEY as string | undefined;
  if (!key?.trim()) {
    throw new Error('未配置 VITE_AMAP_KEY（高德 Web 服务 Key）');
  }
  return key.trim();
}

function buildDisplayText(province: string, city: string, district: string): string {
  const parts = [province, city, district].filter(Boolean);
  const unique: string[] = [];
  for (const p of parts) {
    if (!unique.includes(p)) unique.push(p);
  }
  return unique.join('·') || '未知位置';
}

/** 矩形 "lng1,lat1;lng2,lat2" → 中心点 "lng,lat" */
function rectangleCenter(rectangle: string): string | null {
  const segs = rectangle.split(';');
  if (segs.length !== 2) return null;
  const p1 = segs[0].split(',').map((s) => Number(s.trim()));
  const p2 = segs[1].split(',').map((s) => Number(s.trim()));
  if (p1.length !== 2 || p2.length !== 2 || p1.some(Number.isNaN) || p2.some(Number.isNaN)) {
    return null;
  }
  const lng = ((p1[0] + p2[0]) / 2).toFixed(6);
  const lat = ((p1[1] + p2[1]) / 2).toFixed(6);
  return `${lng},${lat}`;
}

/** 获取当前用户公网 IPv4 */
export async function fetchPublicIp(signal?: AbortSignal): Promise<string> {
  const res = await fetch(IPIFY_URL, { signal });
  if (!res.ok) {
    throw new Error(`获取公网 IP 失败 HTTP ${res.status}`);
  }
  const data = (await res.json()) as IpifyResponse;
  const ip = (data.ip || '').trim();
  if (!ip) {
    throw new Error('未拿到公网 IP');
  }
  return ip;
}

/** 逆地理：用坐标补全区县 */
async function regeoDistrict(
  location: string,
  key: string,
  signal?: AbortSignal
): Promise<{ province: string; city: string; district: string } | null> {
  const params = new URLSearchParams({
    key,
    location,
    extensions: 'base',
    output: 'JSON',
  });
  const res = await fetch(`${AMAP_REGEO_URL}?${params.toString()}`, { signal });
  if (!res.ok) return null;
  const data = (await res.json()) as AmapRegeoResponse;
  if (data.status !== '1' || !data.regeocode?.addressComponent) return null;
  const ac = data.regeocode.addressComponent;
  return {
    province: amapText(ac.province),
    city: amapText(ac.city),
    district: amapText(ac.district),
  };
}

async function locateByIpV5(ip: string, key: string, signal?: AbortSignal): Promise<IpLocationResult | null> {
  const params = new URLSearchParams({ key, ip, type: '4' });
  const res = await fetch(`${AMAP_IP_V5_URL}?${params.toString()}`, { signal });
  if (!res.ok) return null;
  const data = (await res.json()) as AmapIpV5Response;
  if (data.status !== '1') return null;
  const province = amapText(data.province);
  const city = amapText(data.city);
  const district = amapText(data.district);
  if (!province && !city && !district) return null;
  return {
    ip,
    province,
    city,
    district,
    displayText: buildDisplayText(province, city, district),
  };
}

/**
 * 基础 IP 定位到市 + 矩形中心逆地理到区县
 */
async function locateByIpV3(ip: string, key: string, signal?: AbortSignal): Promise<IpLocationResult> {
  const params = new URLSearchParams({ key, ip, output: 'JSON' });
  const res = await fetch(`${AMAP_IP_V3_URL}?${params.toString()}`, { signal });
  if (!res.ok) {
    throw new Error(`高德 IP 定位 HTTP ${res.status}`);
  }
  const data = (await res.json()) as AmapIpV3Response;
  if (data.status !== '1') {
    throw new Error(amapText(data.info) || '高德 IP 定位失败');
  }

  let province = amapText(data.province);
  let city = amapText(data.city);
  let district = '';

  if (province === '局域网' || (!province && !city)) {
    throw new Error('当前为局域网 IP，无法解析到区县');
  }

  const rect = amapText(data.rectangle);
  const center = rect ? rectangleCenter(rect) : null;
  if (center) {
    const detail = await regeoDistrict(center, key, signal);
    if (detail) {
      province = detail.province || province;
      city = detail.city || city;
      district = detail.district || district;
    }
  }

  return {
    ip,
    province,
    city,
    district,
    displayText: buildDisplayText(province, city, district),
  };
}

/**
 * 取公网 IP → 调高德 IP 定位（精确到区县）
 */
export async function fetchIpLocation(signal?: AbortSignal): Promise<IpLocationResult> {
  const key = requireAmapKey();
  const ip = await fetchPublicIp(signal);
  const v5 = await locateByIpV5(ip, key, signal);
  if (v5?.district) return v5;
  if (v5) {
    // v5 有省市但无区县时仍走 v3+逆地理补全
    try {
      return await locateByIpV3(ip, key, signal);
    } catch {
      return v5;
    }
  }
  return locateByIpV3(ip, key, signal);
}
