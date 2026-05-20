import { describe, it, expect } from 'vitest';
import { attachmentsApi } from '@/lib/api';

/**
 * SG-07: attachmentsApi.upload must reject disallowed MIME types client-side.
 */
describe('attachmentsApi.upload MIME type guard', () => {
  const makeFile = (name: string, type: string) =>
    new File([new Uint8Array([1, 2, 3])], name, { type });

  it('rejects executable files before sending', async () => {
    const file = makeFile('virus.exe', 'application/x-msdownload');
    await expect(
      attachmentsApi.upload('proj-id', file)
    ).rejects.toThrow(/not allowed/i);
  });

  it('rejects HTML files', async () => {
    const file = makeFile('xss.html', 'text/html');
    await expect(
      attachmentsApi.upload('proj-id', file)
    ).rejects.toThrow(/not allowed/i);
  });

  it('rejects JavaScript files', async () => {
    const file = makeFile('script.js', 'application/javascript');
    await expect(
      attachmentsApi.upload('proj-id', file)
    ).rejects.toThrow(/not allowed/i);
  });

  it('allows PDF files to proceed to fetch', async () => {
    const file = makeFile('report.pdf', 'application/pdf');
    // fetch is not mocked → will throw network error, but NOT our type-guard error
    const result = attachmentsApi.upload('proj-id', file).catch(err => err.message);
    await expect(result).resolves.not.toMatch(/not allowed/i);
  });

  it('allows PNG images to proceed to fetch', async () => {
    const file = makeFile('screenshot.png', 'image/png');
    const result = attachmentsApi.upload('proj-id', file).catch(err => err.message);
    await expect(result).resolves.not.toMatch(/not allowed/i);
  });
});
