import { marked } from 'marked';
import DOMPurify from 'dompurify';

const renderer = new marked.Renderer();
renderer.link = ({ href, title, text }) => {
  const safeTitle = title ? ` title="${title}"` : '';
  return `<a href="${href}"${safeTitle} target="_blank" rel="noopener noreferrer">${text}</a>`;
};

marked.use({
  renderer,
  gfm: true,
  breaks: true,
});

export function renderMarkdown(content: string): string {
  if (!content) return '';
  const raw = marked.parse(content, { async: false }) as string;
  return DOMPurify.sanitize(raw, {
    ADD_ATTR: ['target', 'rel'],
  });
}
