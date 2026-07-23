/**
 * 后端 SNAKE_CASE 序列化后，景点 VO 的多词字段为 snake_case，
 * 模板使用驼峰时需合并，避免 undefined 被当成「否」或隐藏住宿信息。
 */
export function normalizeScenicVO(raw: Record<string, unknown> | null | undefined): Record<string, unknown> {
  if (raw == null || typeof raw !== 'object') {
    return {};
  }
  const r = raw;
  return {
    ...r,
    villageName: r.villageName ?? r.village_name,
    hasAccommodation: r.hasAccommodation ?? r.has_accommodation,
    accommodationInfo: r.accommodationInfo ?? r.accommodation_info
  };
}
