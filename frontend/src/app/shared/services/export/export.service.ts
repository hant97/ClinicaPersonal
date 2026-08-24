import { Injectable } from '@angular/core';

export type ExportRow = Record<string, unknown>;

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  exportToCsv(data: ExportRow[], fileName: string): void {
    if (data.length === 0) {
      return;
    }

    const csv = buildCsv(data);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${sanitizeFileName(fileName)}.csv`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
}

export function buildCsv(data: ExportRow[]): string {
  if (data.length === 0) {
    return '';
  }

  const columns = Object.keys(data[0]);
  const lines = [
    columns.map(escapeCsvCell).join(';'),
    ...data.map(row => columns.map(column => escapeCsvCell(row[column])).join(';'))
  ];
  return `\uFEFF${lines.join('\r\n')}`;
}

function escapeCsvCell(value: unknown): string {
  let text = value == null ? '' : String(value);
  if (/^[=+\-@]/.test(text)) {
    text = `'${text}`;
  }
  return `"${text.replace(/"/g, '""')}"`;
}

function sanitizeFileName(fileName: string): string {
  const sanitized = fileName.trim().replace(/[<>:"/\\|?*\u0000-\u001F]/g, '_');
  return sanitized || 'exportacion';
}
