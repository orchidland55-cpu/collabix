import DOMPurify from 'dompurify';
import { cn } from '../../lib/cn';

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

function parseMarkdown(text: string): string {
  if (!text) return '';

  let html = text
    .replace(/&/g, '&')
    .replace(/</g, '<')
    .replace(/>/g, '>');

  html = html.replace(/^### (.+)$/gm, '<h3 class="text-body font-semibold text-text-primary mt-4 mb-2">$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2 class="text-section font-semibold text-text-primary mt-5 mb-2">$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1 class="text-page font-bold text-text-primary mt-6 mb-3">$1</h1>');
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong class="font-semibold text-text-primary">$1</strong>');
  html = html.replace(/\*(.+?)\*/g, '<em class="italic">$1</em>');
  html = html.replace(/`(.+?)`/g, '<code class="rounded bg-surface-2 px-1.5 py-0.5 text-caption font-mono text-accent-600 dark:text-accent-400">$1</code>');

  html = html.replace(/^> (.+)$/gm, '<blockquote class="border-l-2 border-accent-400 pl-4 py-1 my-3 text-text-secondary italic">$1</blockquote>');

  html = html.replace(/^- \[x\] (.+)$/gm, '<label class="flex items-center gap-2 my-1 text-text-secondary"><input type="checkbox" checked disabled class="rounded border-border-default accent-accent-600" /><span class="line-through text-text-tertiary">$1</span></label>');
  html = html.replace(/^- \[ \] (.+)$/gm, '<label class="flex items-center gap-2 my-1 text-text-secondary"><input type="checkbox" disabled class="rounded border-border-default accent-accent-600" />$1</label>');

  html = html.replace(/^- (.+)$/gm, '<li class="flex items-start gap-2 text-text-secondary my-0.5"><span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-text-tertiary"></span><span>$1</span></li>');

  html = html.replace(/^\d+\. (.+)$/gm, '<li class="flex items-start gap-2 text-text-secondary my-0.5"><span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-text-tertiary"></span><span>$1</span></li>');

  html = html.replace(/^---$/gm, '<hr class="my-4 border-border-subtle" />');

  const tableRegex = /\|(.+)\|\n\|[-| ]+\|\n((?:\|.+\|\n?)*)/g;
  html = html.replace(tableRegex, (_match: string, headerRow: string, bodyRows: string) => {
    const headers = headerRow.split('|').map((h: string) => h.trim()).filter(Boolean);
    const rows = bodyRows.trim().split('\n').map((row: string) =>
      row.split('|').map((c: string) => c.trim()).filter(Boolean)
    );
    let tableHtml = '<div class="overflow-x-auto my-3"><table class="w-full border-collapse text-caption">';
    tableHtml += '<thead><tr>';
    headers.forEach((h: string) => {
      tableHtml += `<th class="border border-border-subtle bg-surface-2 px-3 py-2 text-left font-medium text-text-primary">${h}</th>`;
    });
    tableHtml += '</tr></thead><tbody>';
    rows.forEach((row: string[]) => {
      tableHtml += '<tr>';
      row.forEach((cell: string) => {
        tableHtml += `<td class="border border-border-subtle px-3 py-2 text-text-secondary">${cell}</td>`;
      });
      tableHtml += '</tr>';
    });
    tableHtml += '</tbody></table></div>';
    return tableHtml;
  });

  const paragraphs = html.split('\n\n');
  html = paragraphs
    .map((p) => {
      const trimmed = p.trim();
      if (!trimmed) return '';
      if (
        trimmed.startsWith('<h') ||
        trimmed.startsWith('<li') ||
        trimmed.startsWith('<ul') ||
        trimmed.startsWith('<blockquote') ||
        trimmed.startsWith('<div') ||
        trimmed.startsWith('<hr') ||
        trimmed.startsWith('<label') ||
        trimmed.startsWith('<table')
      ) {
        return trimmed;
      }
      return `<p class="text-body text-text-secondary leading-relaxed my-1.5">${trimmed}</p>`;
    })
    .join('\n');

  return html;
}

export function MarkdownRenderer({ content, className }: MarkdownRendererProps) {
  return (
    <div
      className={cn('prose-cx', className)}
      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(parseMarkdown(content)) }}
    />
  );
}