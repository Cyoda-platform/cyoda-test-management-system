/**
 * Returns true if the test run executor should automatically advance to the
 * next test case after setting a step status.
 *
 * Conditions for auto-advance:
 * - All steps have a non-untested status
 * - No step is failed (failed cases require defect logging — stay put)
 * - The current case is not the last one in the run
 */
export function shouldAutoAdvance(
  statuses: string[],
  selectedIdx: number,
  totalCases: number
): boolean {
  if (statuses.length === 0) return false;
  if (selectedIdx >= totalCases - 1) return false;
  const allFilled = statuses.every((s) => s !== 'untested');
  if (!allFilled) return false;
  return !statuses.some((s) => s === 'failed');
}
