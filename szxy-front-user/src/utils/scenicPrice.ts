/** 景点价格展示：0 元显示「免费」 */
export function formatScenicPrice(
  price: number | null | undefined,
  options?: { suffix?: string; empty?: string }
): string {
  if (price == null) return options?.empty ?? '';
  if (Number(price) === 0) return '免费';
  return `¥${price}${options?.suffix ?? ''}`;
}

export function hasScenicPrice(price: number | null | undefined): boolean {
  return price != null;
}
