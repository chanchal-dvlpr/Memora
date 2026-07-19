import { Renderer } from './renderer';

export interface TableColumn<T> {
  readonly header: string;
  readonly key: keyof T | ((item: T) => string);
}

export interface TableData<T> {
  readonly columns: TableColumn<T>[];
  readonly rows: T[];
}

/**
 * Renderer generating padded tabular CLI strings.
 */
export class TableRenderer<T> implements Renderer<TableData<T>> {
  render(model: TableData<T>): string {
    const { columns, rows } = model;
    if (rows.length === 0) {
      return 'No data available.';
    }

    const widths = columns.map((col) => {
      let max = col.header.length;
      for (const row of rows) {
        const val = typeof col.key === 'function' ? col.key(row) : String((row as Record<string, unknown>)[col.key as string] ?? '');
        if (val.length > max) {
          max = val.length;
        }
      }
      return max;
    });

    const lines: string[] = [];

    // Header row
    const headerRow = columns
      .map((col, i) => col.header.padEnd(widths[i]))
      .join(' | ');
    lines.push(headerRow);

    // Separator line
    const separatorRow = columns
      .map((_, i) => '-'.repeat(widths[i]))
      .join('-+-');
    lines.push(separatorRow);

    // Data rows
    for (const row of rows) {
      const dataRow = columns
        .map((col, i) => {
          const val = typeof col.key === 'function' ? col.key(row) : String((row as Record<string, unknown>)[col.key as string] ?? '');
          return val.padEnd(widths[i]);
        })
        .join(' | ');
      lines.push(dataRow);
    }

    return lines.join('\n');
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const tableRenderer = new TableRenderer<any>();
