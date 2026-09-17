export type ContactSegment =
  | { type: 'text'; value: string }
  | { type: 'phone'; value: string; href: string }
  | { type: 'email'; value: string; href: string };

type MatchItem = {
  start: number;
  end: number;
  segment: ContactSegment;
};

const EMAIL_PATTERN = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g;
const PHONE_PATTERN = /(?:\+?86[-\s]?)?1[3-9]\d{9}|(?:0\d{2,3}[-\s]?)?\d{7,8}/g;

function normalizePhone(raw: string) {
  const digits = raw.replace(/\D/g, '');
  if (digits.startsWith('86') && digits.length > 11) {
    return digits.slice(2);
  }
  return digits;
}

/** 将联系方式文本解析为普通文本 / 电话 / 邮箱片段 */
export function parseContactSegments(contact: string): ContactSegment[] {
  const text = contact.trim();
  if (!text) return [];

  const matches: MatchItem[] = [];

  for (const m of text.matchAll(EMAIL_PATTERN)) {
    if (m.index == null) continue;
    const value = m[0];
    matches.push({
      start: m.index,
      end: m.index + value.length,
      segment: { type: 'email', value, href: `mailto:${value}` }
    });
  }

  for (const m of text.matchAll(PHONE_PATTERN)) {
    if (m.index == null) continue;
    const value = m[0];
    const start = m.index;
    const end = start + value.length;
    if (matches.some((item) => start >= item.start && start < item.end)) {
      continue;
    }
    matches.push({
      start,
      end,
      segment: {
        type: 'phone',
        value,
        href: `tel:${normalizePhone(value)}`
      }
    });
  }

  if (!matches.length) {
    return [{ type: 'text', value: text }];
  }

  matches.sort((a, b) => a.start - b.start);

  const segments: ContactSegment[] = [];
  let cursor = 0;
  for (const item of matches) {
    if (item.start > cursor) {
      segments.push({ type: 'text', value: text.slice(cursor, item.start) });
    }
    segments.push(item.segment);
    cursor = item.end;
  }
  if (cursor < text.length) {
    segments.push({ type: 'text', value: text.slice(cursor) });
  }
  return segments;
}
