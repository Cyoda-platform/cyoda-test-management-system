import { describe, it, expect } from 'vitest';
import { shouldAutoAdvance } from '@/lib/runUtils';

describe('shouldAutoAdvance', () => {
  it('returns true when all steps passed and not last case', () => {
    expect(shouldAutoAdvance(['passed', 'passed', 'passed'], 0, 5)).toBe(true);
  });

  it('returns true when all steps skipped and not last case', () => {
    expect(shouldAutoAdvance(['skipped', 'skipped'], 2, 5)).toBe(true);
  });

  it('returns true when steps are mixed passed+skipped and not last case', () => {
    expect(shouldAutoAdvance(['passed', 'skipped', 'passed'], 1, 3)).toBe(true);
  });

  it('returns false when any step is failed', () => {
    expect(shouldAutoAdvance(['passed', 'failed', 'passed'], 0, 5)).toBe(false);
  });

  it('returns false when not all steps are filled (untested present)', () => {
    expect(shouldAutoAdvance(['passed', 'untested', 'passed'], 0, 5)).toBe(false);
  });

  it('returns false when on last case', () => {
    expect(shouldAutoAdvance(['passed', 'passed'], 4, 5)).toBe(false);
  });

  it('returns false for empty statuses array', () => {
    expect(shouldAutoAdvance([], 0, 5)).toBe(false);
  });
});
