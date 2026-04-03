import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Returns a sequential display ID with a prefix, matching the REP-01 / IMP-1 patterns.
 * e.g. listDisplayId("TR", 0) → "TR-01"
 *      listDisplayId("DEF", 2) → "DEF-03"
 */
export function listDisplayId(prefix: string, index: number): string {
  return `${prefix}-${String(index + 1).padStart(2, "0")}`;
}

/**
 * Generates the next stable display ID for a new entity given the list of
 * already-persisted items. Reads the highest sequential number already stored
 * in their displayId values so the new ID is independent of list position.
 *
 * e.g. nextListDisplayId("TR", runs) → "TR-04" if runs already have TR-01…TR-03
 */
export function nextListDisplayId(prefix: string, existing: Array<{ displayId?: string }>): string {
  const pattern = new RegExp(`^${prefix}-(\\d+)$`);
  const maxNum = existing.reduce((max, item) => {
    const m = (item.displayId ?? "").match(pattern);
    return m ? Math.max(max, parseInt(m[1], 10)) : max;
  }, 0);
  return `${prefix}-${String(maxNum + 1).padStart(2, "0")}`;
}

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Returns true if the string looks like a UUID. */
export function isUuid(value: string): boolean {
  return UUID_REGEX.test(value);
}

/**
 * Formats a date string or Date object as "MMM-DD-YYYY" (e.g. "APR-01-2026").
 * Returns "—" for missing or invalid values.
 */
export function formatDate(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const d = typeof value === "string" ? new Date(value) : value;
  if (isNaN(d.getTime())) return "—";
  const month = d.toLocaleDateString("en-US", { month: "short" }).toUpperCase();
  const day = String(d.getDate()).padStart(2, "0");
  const year = d.getFullYear();
  return `${month}-${day}-${year}`;
}
