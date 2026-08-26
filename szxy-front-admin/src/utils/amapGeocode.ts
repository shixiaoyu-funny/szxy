/**
 * 高德地图 Web 服务 · 地理编码（地址 → 经纬度）
 * 文档：https://lbs.amap.com/api/webservice/guide/api/georegeo
 * 接口：GET https://restapi.amap.com/v3/geocode/geo
 * Key：须为「Web 服务」类型，配置在 .env.local 的 VITE_AMAP_KEY
 */

const AMAP_GEO_URL = 'https://restapi.amap.com/v3/geocode/geo';

export interface GeocodeResult {
  /** 经度（高德 GCJ-02） */
  longitude: string;
  /** 纬度（高德 GCJ-02） */
  latitude: string;
  /** 原始 location 字符串：经度,纬度 */
  location: string;
}

interface AmapGeoResponse {
  status?: string;
  info?: string;
  geocodes?: Array<{ location?: string; formatted_address?: string }>;
}

/**
 * 按省市区拼接地址，调用高德地理编码。
 * @returns location 拆成的经纬度；失败抛 Error
 */
export async function geocodeByRegion(
  province: string,
  city: string,
  county: string
): Promise<GeocodeResult> {
  const key = import.meta.env.VITE_AMAP_KEY as string | undefined;
  if (!key) {
    throw new Error('未配置 VITE_AMAP_KEY（高德 Web 服务 Key）');
  }
  // address：结构化地址；city：限定城市提高精度
  const address = `${province}${city}${county}`;
  const params = new URLSearchParams({
    key,
    address,
    city,
    output: 'JSON'
  });
  const res = await fetch(`${AMAP_GEO_URL}?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`高德地理编码 HTTP ${res.status}`);
  }
  const data = (await res.json()) as AmapGeoResponse;
  // status "1" 表示成功
  if (data.status !== '1' || !data.geocodes?.length) {
    throw new Error(data.info || '高德未返回该地址的坐标');
  }
  const location = (data.geocodes[0]?.location || '').trim();
  const parts = location.split(',');
  if (parts.length !== 2 || !parts[0] || !parts[1]) {
    throw new Error('高德返回的 location 格式异常');
  }
  return {
    longitude: parts[0].trim(),
    latitude: parts[1].trim(),
    location
  };
}
