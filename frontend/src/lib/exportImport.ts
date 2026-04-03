import type { LocalSuite as Suite, LocalCase as TestCase, LocalStep as TestStep } from '@/lib/localTypes';
import { formatDate } from '@/lib/utils';

// ── Helpers ──

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function dateStamp() {
  return new Date().toISOString().slice(0, 10).replace(/-/g, '');
}

interface ExportOptions {
  suites: Suite[];
  projectName: string;
  format: string;
  includeSteps: boolean;
  includePreconditions: boolean;
}

interface FlatCase {
  suiteId: string;
  suiteName: string;
  /** Human-readable stable ID (displayId if set, UUID fallback). Used as Case_ID in exports. */
  caseId: string;
  title: string;
  priority: string;
  description: string;
  preconditions: string;
  stepOrder: number | '';
  stepAction: string;
  stepExpected: string;
}

function flattenCases(suites: Suite[], includeSteps: boolean, includePreconditions: boolean): FlatCase[] {
  const rows: FlatCase[] = [];
  for (const suite of suites) {
    for (const tc of suite.cases.filter(c => !c.deleted)) {
      const caseId = tc.displayId || tc.id;
      if (includeSteps && tc.steps.length > 0) {
        for (const step of tc.steps) {
          rows.push({
            suiteId: suite.id, suiteName: suite.name,
            caseId, title: tc.title, priority: tc.priority,
            description: tc.description,
            preconditions: includePreconditions ? tc.preconditions : '',
            stepOrder: step.order, stepAction: step.action, stepExpected: step.expectedResult,
          });
        }
      } else {
        rows.push({
          suiteId: suite.id, suiteName: suite.name,
          caseId, title: tc.title, priority: tc.priority,
          description: tc.description,
          preconditions: includePreconditions ? tc.preconditions : '',
          stepOrder: '', stepAction: '', stepExpected: '',
        });
      }
    }
  }
  return rows;
}

// ── CSV ──

function escapeCSV(val: string): string {
  if (val.includes(',') || val.includes('"') || val.includes('\n')) {
    return `"${val.replace(/"/g, '""')}"`;
  }
  return val;
}

function exportCSV(opts: ExportOptions) {
  const rows = flattenCases(opts.suites, opts.includeSteps, opts.includePreconditions);
  const headers = ['Suite_ID', 'Suite_Name', 'Case_ID', 'Title', 'Priority', 'Description'];
  if (opts.includePreconditions) headers.push('Pre_Conditions');
  if (opts.includeSteps) headers.push('Step_Order', 'Step_Action', 'Step_Expected_Result');

  const lines = [headers.join(',')];
  for (const r of rows) {
    const vals = [r.suiteId, r.suiteName, r.caseId, r.title, r.priority, r.description];
    if (opts.includePreconditions) vals.push(r.preconditions);
    if (opts.includeSteps) vals.push(String(r.stepOrder), r.stepAction, r.stepExpected);
    lines.push(vals.map(v => escapeCSV(v)).join(','));
  }
  return new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
}

// ── JSON ──

function exportJSON(opts: ExportOptions) {
  const data = opts.suites.map(s => ({
    suiteId: s.id,
    suiteName: s.name,
    cases: s.cases.filter(c => !c.deleted).map(tc => {
      const c: Record<string, unknown> = {
        id: tc.displayId || tc.id, title: tc.title, priority: tc.priority, description: tc.description,
      };
      if (opts.includePreconditions) c.preconditions = tc.preconditions;
      if (opts.includeSteps) c.steps = tc.steps.map(st => ({ order: st.order, action: st.action, expectedResult: st.expectedResult }));
      return c;
    }),
  }));
  return new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' });
}

// ── XML ──

function escapeXML(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function exportXML(opts: ExportOptions) {
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n<TestRepository>\n';
  for (const suite of opts.suites) {
    xml += `  <Suite id="${escapeXML(suite.id)}" name="${escapeXML(suite.name)}">\n`;
    for (const tc of suite.cases.filter(c => !c.deleted)) {
      xml += `    <TestCase id="${escapeXML(tc.displayId || tc.id)}" priority="${tc.priority}">\n`;
      xml += `      <Title>${escapeXML(tc.title)}</Title>\n`;
      xml += `      <Description>${escapeXML(tc.description)}</Description>\n`;
      if (opts.includePreconditions) xml += `      <PreConditions>${escapeXML(tc.preconditions)}</PreConditions>\n`;
      if (opts.includeSteps && tc.steps.length > 0) {
        xml += '      <Steps>\n';
        for (const st of tc.steps) {
          xml += `        <Step order="${st.order}">\n`;
          xml += `          <Action>${escapeXML(st.action)}</Action>\n`;
          xml += `          <ExpectedResult>${escapeXML(st.expectedResult)}</ExpectedResult>\n`;
          xml += '        </Step>\n';
        }
        xml += '      </Steps>\n';
      }
      xml += '    </TestCase>\n';
    }
    xml += '  </Suite>\n';
  }
  xml += '</TestRepository>';
  return new Blob([xml], { type: 'application/xml;charset=utf-8' });
}

// ── Excel (simple CSV with .xlsx-compatible TSV) ──

function exportExcel(opts: ExportOptions) {
  // Generate a real xlsx using a minimal XLSX approach via CSV tab-separated
  // For a proper xlsx we build a simple spreadsheet XML (SpreadsheetML)
  const rows = flattenCases(opts.suites, opts.includeSteps, opts.includePreconditions);
  const headers = ['Suite_ID', 'Suite_Name', 'Case_ID', 'Title', 'Priority', 'Description'];
  if (opts.includePreconditions) headers.push('Pre_Conditions');
  if (opts.includeSteps) headers.push('Step_Order', 'Step_Action', 'Step_Expected_Result');

  // Build minimal xlsx via tab-separated CSV that Excel can open
  const lines = [headers.join('\t')];
  for (const r of rows) {
    const vals = [r.suiteId, r.suiteName, r.caseId, r.title, r.priority, r.description];
    if (opts.includePreconditions) vals.push(r.preconditions);
    if (opts.includeSteps) vals.push(String(r.stepOrder), r.stepAction, r.stepExpected);
    lines.push(vals.map(v => v.replace(/\t/g, ' ')).join('\t'));
  }
  // Use xls extension with TSV content - Excel opens this natively
  return new Blob(['\uFEFF' + lines.join('\n')], { type: 'application/vnd.ms-excel;charset=utf-8' });
}

// ── PDF ──

function exportPDF(opts: ExportOptions) {
  // Generate a printable HTML document and trigger print/save as PDF
  const rows = flattenCases(opts.suites, opts.includeSteps, opts.includePreconditions);
  
  let html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>Test Cases Export</title>
<style>
  body { font-family: Arial, sans-serif; font-size: 11px; margin: 20px; color: #1e293b; }
  h1 { font-size: 18px; margin-bottom: 4px; }
  .meta { color: #64748b; font-size: 10px; margin-bottom: 16px; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; }
  th { background: #f1f5f9; text-align: left; padding: 6px 8px; border: 1px solid #e2e8f0; font-size: 10px; text-transform: uppercase; color: #475569; }
  td { padding: 5px 8px; border: 1px solid #e2e8f0; vertical-align: top; }
  tr:nth-child(even) { background: #f8fafc; }
  .priority-HIGH { color: #F43F5E; font-weight: 600; }
  .priority-MEDIUM { color: #F59E0B; font-weight: 600; }
  .priority-LOW { color: #64748b; }
  @media print { body { margin: 0; } }
</style></head><body>
<h1>Test Cases — ${opts.projectName}</h1>
<div class="meta">Exported on ${formatDate(new Date())} | ${rows.length} rows</div>
<table><thead><tr>
<th>Suite</th><th>Case ID</th><th>Title</th><th>Priority</th><th>Description</th>`;
  if (opts.includePreconditions) html += '<th>Pre-Conditions</th>';
  if (opts.includeSteps) html += '<th>Step #</th><th>Action</th><th>Expected</th>';
  html += '</tr></thead><tbody>';
  for (const r of rows) {
    html += `<tr><td>${escapeXML(r.suiteName)}</td><td>${escapeXML(r.caseId)}</td><td>${escapeXML(r.title)}</td><td class="priority-${r.priority}">${r.priority}</td><td>${escapeXML(r.description)}</td>`;
    if (opts.includePreconditions) html += `<td>${escapeXML(r.preconditions)}</td>`;
    if (opts.includeSteps) html += `<td>${r.stepOrder}</td><td>${escapeXML(r.stepAction)}</td><td>${escapeXML(r.stepExpected)}</td>`;
    html += '</tr>';
  }
  html += '</tbody></table></body></html>';

  // Open in new window for print-to-PDF
  const w = window.open('', '_blank');
  if (w) {
    w.document.write(html);
    w.document.close();
    setTimeout(() => w.print(), 500);
  }
}

// ── Main export dispatcher ──

export async function performExport(opts: ExportOptions): Promise<void> {
  const filename = `CYODA_Export_${opts.projectName.replace(/\s+/g, '_')}_${dateStamp()}`;

  if (opts.format === 'pdf') {
    exportPDF(opts);
    return;
  }

  let blob: Blob;
  let ext: string;
  switch (opts.format) {
    case 'json':
      blob = exportJSON(opts); ext = 'json'; break;
    case 'xml':
      blob = exportXML(opts); ext = 'xml'; break;
    case 'excel':
      blob = exportExcel(opts); ext = 'xls'; break;
    default:
      blob = exportCSV(opts); ext = 'csv'; break;
  }
  triggerDownload(blob, `${filename}.${ext}`);
}

// ── CSV Template ──

export function downloadCSVTemplate() {
  const headers = 'Title,Description,Pre_Conditions,Step_Action,Step_Expected_Result';
  const example = '"Login with valid credentials","Verify user can login","User account exists","Navigate to login page","Login form displayed"';
  const blob = new Blob([headers + '\n' + example + '\n'], { type: 'text/csv;charset=utf-8' });
  triggerDownload(blob, 'CYODA_Import_Template.csv');
}

// ── Import ──

interface ParsedCase {
  id?: string;
  title: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  preconditions: string;
  steps: TestStep[];
}

function parseCSVLine(line: string): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"' && line[i + 1] === '"') { current += '"'; i++; }
      else if (ch === '"') { inQuotes = false; }
      else { current += ch; }
    } else {
      if (ch === '"') { inQuotes = true; }
      else if (ch === ',') { result.push(current); current = ''; }
      else { current += ch; }
    }
  }
  result.push(current);
  return result;
}

function parseCSVImport(text: string): ParsedCase[] {
  const lines = text.split(/\r?\n/).filter(l => l.trim());
  if (lines.length < 2) return [];
  const headers = parseCSVLine(lines[0]).map(h => h.trim().toLowerCase());
  
  const titleIdx = headers.findIndex(h => h.includes('title'));
  const descIdx = headers.findIndex(h => h.includes('description'));
  const preIdx = headers.findIndex(h => h.includes('pre_condition') || h.includes('precondition'));
  const prioIdx = headers.findIndex(h => h.includes('priority'));
  const idIdx = headers.findIndex(h => h === 'case_id' || h === 'id');
  const stepActionIdx = headers.findIndex(h => h.includes('step_action') || h.includes('action'));
  const stepExpIdx = headers.findIndex(h => h.includes('step_expected') || h.includes('expected'));
  const stepOrderIdx = headers.findIndex(h => h.includes('step_order') || h.includes('order'));

  if (titleIdx === -1) throw new Error('CSV must contain a "Title" column');

  // Group rows by case (rows with same title or case_id belong together for steps)
  const caseMap = new Map<string, ParsedCase>();
  for (let i = 1; i < lines.length; i++) {
    const vals = parseCSVLine(lines[i]);
    const title = vals[titleIdx]?.trim() || '';
    if (!title) continue;
    const id = idIdx >= 0 ? vals[idIdx]?.trim() : undefined;
    const key = id || title;

    if (!caseMap.has(key)) {
      caseMap.set(key, {
        id: id || undefined,
        title,
        priority: normalizePriority(prioIdx >= 0 ? vals[prioIdx]?.trim() : ''),
        description: descIdx >= 0 ? vals[descIdx]?.trim() || '' : '',
        preconditions: preIdx >= 0 ? vals[preIdx]?.trim() || '' : '',
        steps: [],
      });
    }
    const tc = caseMap.get(key)!;
    const action = stepActionIdx >= 0 ? vals[stepActionIdx]?.trim() : '';
    const expected = stepExpIdx >= 0 ? vals[stepExpIdx]?.trim() : '';
    if (action) {
      tc.steps.push({
        order: stepOrderIdx >= 0 && vals[stepOrderIdx] ? parseInt(vals[stepOrderIdx]) || tc.steps.length + 1 : tc.steps.length + 1,
        action,
        expectedResult: expected || '',
        status: 'untested',
      });
    }
  }
  return Array.from(caseMap.values());
}

function parseJSONImport(text: string): ParsedCase[] {
  const data = JSON.parse(text);
  const cases: ParsedCase[] = [];
  // Support array of suites or flat array of cases
  const items = Array.isArray(data) ? data : [data];
  for (const item of items) {
    if (item.cases && Array.isArray(item.cases)) {
      for (const c of item.cases) cases.push(normalizeCase(c));
    } else {
      cases.push(normalizeCase(item));
    }
  }
  return cases;
}

function normalizeCase(c: Record<string, unknown>): ParsedCase {
  const steps = Array.isArray(c.steps) ? c.steps.map((s: Record<string, unknown>, i: number) => ({
    order: (s.order as number) || i + 1,
    action: String(s.action || ''),
    expectedResult: String(s.expectedResult || s.expected_result || ''),
    status: 'untested' as const,
  })) : [];
  return {
    id: c.id ? String(c.id) : undefined,
    title: String(c.title || ''),
    priority: normalizePriority(String(c.priority || '')),
    description: String(c.description || ''),
    preconditions: String(c.preconditions || c.pre_conditions || ''),
    steps,
  };
}

function parseXMLImport(text: string): ParsedCase[] {
  const parser = new DOMParser();
  const doc = parser.parseFromString(text, 'application/xml');
  const cases: ParsedCase[] = [];
  const tcElements = doc.querySelectorAll('TestCase');
  for (const el of tcElements) {
    const steps: TestStep[] = [];
    el.querySelectorAll('Step').forEach((s, i) => {
      steps.push({
        order: parseInt(s.getAttribute('order') || '') || i + 1,
        action: s.querySelector('Action')?.textContent || '',
        expectedResult: s.querySelector('ExpectedResult')?.textContent || '',
        status: 'untested',
      });
    });
    cases.push({
      id: el.getAttribute('id') || undefined,
      title: el.querySelector('Title')?.textContent || '',
      priority: normalizePriority(el.getAttribute('priority') || ''),
      description: el.querySelector('Description')?.textContent || '',
      preconditions: el.querySelector('PreConditions')?.textContent || '',
      steps,
    });
  }
  return cases;
}

function normalizePriority(p: string): 'HIGH' | 'MEDIUM' | 'LOW' {
  const u = p.toUpperCase();
  if (u === 'HIGH' || u === 'H') return 'HIGH';
  if (u === 'LOW' || u === 'L') return 'LOW';
  return 'MEDIUM';
}

export interface ImportResult {
  imported: number;
  skipped: number;
  overwritten: number;
}

export async function performImport(
  file: File,
  targetSuiteId: string,
  conflict: 'skip' | 'overwrite' | 'create_new',
  suites: Suite[],
  projectId: string,
): Promise<{ updatedSuites: Suite[]; result: ImportResult }> {
  const text = await file.text();
  const ext = file.name.split('.').pop()?.toLowerCase();

  let parsed: ParsedCase[];
  if (ext === 'json') parsed = parseJSONImport(text);
  else if (ext === 'xml') parsed = parseXMLImport(text);
  else parsed = parseCSVImport(text);

  if (parsed.length === 0) throw new Error('No valid test cases found in the file.');

  const result: ImportResult = { imported: 0, skipped: 0, overwritten: 0 };
  const updatedSuites = [...suites.map(s => ({ ...s, cases: [...s.cases] }))];

  // Ensure target suite exists — create one if needed
  let actualTargetId = targetSuiteId;
  let targetSuite = updatedSuites.find(s => s.id === targetSuiteId);
  if (!targetSuite) {
    const newSuiteId = `S-${Date.now()}`;
    targetSuite = { id: newSuiteId, projectId, name: 'Imported', cases: [] };
    updatedSuites.push(targetSuite);
    actualTargetId = newSuiteId;
  }

  // Build a map of existing cases indexed by both UUID and displayId so that
  // re-importing an exported file (which uses displayId as Case_ID) still detects duplicates.
  const existingMap = new Map<string, { suiteIdx: number; caseIdx: number }>();
  updatedSuites.forEach((s, si) => s.cases.forEach((c, ci) => {
    existingMap.set(c.id, { suiteIdx: si, caseIdx: ci });
    if (c.displayId) existingMap.set(c.displayId, { suiteIdx: si, caseIdx: ci });
  }));

  for (const pc of parsed) {
    if (!pc.title) continue;

    const existing = pc.id ? existingMap.get(pc.id) : undefined;

    if (existing && conflict === 'skip') {
      result.skipped++;
      continue;
    }

    const newCase: TestCase = {
      id: (conflict === 'create_new' || !pc.id) ? `TC-${Date.now()}-${Math.random().toString(36).slice(2, 6)}` : pc.id!,
      suiteId: actualTargetId,
      title: pc.title,
      priority: pc.priority,
      description: pc.description,
      preconditions: pc.preconditions,
      steps: pc.steps,
      deleted: false,
    };

    if (existing && conflict === 'overwrite') {
      updatedSuites[existing.suiteIdx].cases[existing.caseIdx] = { ...newCase, id: pc.id!, suiteId: updatedSuites[existing.suiteIdx].id };
      result.overwritten++;
    } else {
      const tgtIdx = updatedSuites.findIndex(s => s.id === actualTargetId);
      if (tgtIdx >= 0) {
        updatedSuites[tgtIdx].cases.push(newCase);
      }
      result.imported++;
    }
  }

  return { updatedSuites, result };
}
