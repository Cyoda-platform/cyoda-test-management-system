import { readFileSync } from 'fs';
import { resolve } from 'path';
import { describe, it, expect } from 'vitest';

/**
 * SG-03: production source files must not contain console.log calls that
 * fire on every render / page load.
 */
describe('no production console.log leaks', () => {
  function sourceLines(relativePath: string): string[] {
    const abs = resolve(__dirname, '../../', relativePath);
    return readFileSync(abs, 'utf-8').split('\n');
  }

  function consoleLogLines(lines: string[]): string[] {
    return lines.filter(l => /console\.log\(/.test(l));
  }

  it('AuthContext.tsx has no console.log calls', () => {
    const offending = consoleLogLines(sourceLines('src/contexts/AuthContext.tsx'));
    expect(offending).toEqual([]);
  });

  it('Attachments.tsx has no console.log calls', () => {
    const offending = consoleLogLines(sourceLines('src/pages/Attachments.tsx'));
    expect(offending).toEqual([]);
  });
});
