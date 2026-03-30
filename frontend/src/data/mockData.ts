export interface Project {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  deleted: boolean;
  initials: string;
}

export interface Suite {
  id: string;
  projectId: string;
  name: string;
  cases: TestCase[];
}

export interface TestCase {
  id: string;
  suiteId: string;
  title: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  preconditions: string;
  steps: TestStep[];
  deleted: boolean;
}

export interface TestStep {
  order: number;
  action: string;
  expectedResult: string;
  status: 'untested' | 'passed' | 'failed' | 'skipped';
}

export interface TestRun {
  id: string;
  projectId: string;
  name: string;
  environment: string;
  buildVersion: string;
  status: 'initial' | 'active' | 'completed';
  passed: number;
  failed: number;
  skipped: number;
  untested: number;
  createdAt: string;
  description: string;
}

export interface ReportSections {
  executiveSummary: boolean;
  suiteAnalytics: boolean;
  defectTable: boolean;
  environmentInfo: boolean;
}

export interface Defect {
  id: string;
  projectId: string;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  link: string;
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source: string;
  createdAt: string;
}

export const mockProjects: Project[] = [
  { id: 'PROJ-CC-890', name: 'CloudCore Platform', description: 'End-to-end testing for the CloudCore infrastructure management platform including CI/CD pipelines.', createdAt: '2026-03-15', deleted: false, initials: 'CC' },
  { id: 'PROJ-PA-201', name: 'Project Alpha', description: 'Quality assurance suite for the Alpha product launch. Covers authentication, dashboards, and API endpoints.', createdAt: '2026-03-10', deleted: false, initials: 'PA' },
  { id: 'PROJ-MB-445', name: 'MobileBanking v3', description: 'Regression tests for mobile banking application v3.0 release across iOS and Android platforms.', createdAt: '2026-02-28', deleted: false, initials: 'MB' },
  { id: 'PROJ-DW-112', name: 'DataWarehouse ETL', description: 'Validation of ETL pipeline transformations and data integrity checks for the enterprise data warehouse.', createdAt: '2026-02-20', deleted: false, initials: 'DW' },
  { id: 'PROJ-SS-667', name: 'SecureShield Audit', description: 'Security compliance testing and penetration test tracking for SOC2 certification audit.', createdAt: '2026-01-15', deleted: false, initials: 'SS' },
];

export const mockSuites: Suite[] = [
  {
    id: 'S-1', projectId: 'PROJ-PA-201', name: 'Authorization',
    cases: [
      { id: 'AS-1', suiteId: 'S-1', title: 'Login with valid credentials', priority: 'HIGH', description: 'Verify user can login with correct username and password', preconditions: 'User account exists', steps: [{ order: 1, action: 'Navigate to login page', expectedResult: 'Login form displayed', status: 'untested' }, { order: 2, action: 'Enter valid credentials', expectedResult: 'Credentials accepted', status: 'untested' }, { order: 3, action: 'Click Sign In', expectedResult: 'Redirected to dashboard', status: 'untested' }], deleted: false },
      { id: 'AS-2', suiteId: 'S-1', title: 'Login with invalid password', priority: 'HIGH', description: 'Verify error on wrong password', preconditions: 'User account exists', steps: [{ order: 1, action: 'Enter invalid password', expectedResult: 'Error message shown', status: 'untested' }], deleted: false },
      { id: 'AS-3', suiteId: 'S-1', title: 'Session timeout handling', priority: 'MEDIUM', description: 'Verify session expires after inactivity', preconditions: 'User is logged in', steps: [], deleted: false },
      { id: 'AS-4', suiteId: 'S-1', title: 'Password reset flow', priority: 'MEDIUM', description: '', preconditions: '', steps: [], deleted: false },
      { id: 'AS-5', suiteId: 'S-1', title: 'Multi-factor authentication', priority: 'HIGH', description: '', preconditions: '', steps: [], deleted: false },
    ],
  },
  {
    id: 'S-2', projectId: 'PROJ-PA-201', name: 'Dashboard',
    cases: [
      { id: 'DB-1', suiteId: 'S-2', title: 'Dashboard loads correctly', priority: 'HIGH', description: '', preconditions: 'User is authenticated', steps: [], deleted: false },
      { id: 'DB-2', suiteId: 'S-2', title: 'Widget data refresh', priority: 'MEDIUM', description: '', preconditions: '', steps: [], deleted: false },
      { id: 'DB-3', suiteId: 'S-2', title: 'Filter persistence across sessions', priority: 'LOW', description: '', preconditions: '', steps: [], deleted: false },
    ],
  },
  {
    id: 'S-3', projectId: 'PROJ-PA-201', name: 'API Integration',
    cases: [
      { id: 'API-1', suiteId: 'S-3', title: 'GET /users returns 200', priority: 'HIGH', description: '', preconditions: 'API server running', steps: [], deleted: false },
      { id: 'API-2', suiteId: 'S-3', title: 'POST /users validates input', priority: 'HIGH', description: '', preconditions: '', steps: [], deleted: false },
      { id: 'API-3', suiteId: 'S-3', title: 'Rate limiting enforcement', priority: 'MEDIUM', description: '', preconditions: '', steps: [], deleted: false },
      { id: 'API-4', suiteId: 'S-3', title: 'Pagination header correctness', priority: 'LOW', description: '', preconditions: '', steps: [], deleted: false },
    ],
  },
];

export const mockTestRuns: TestRun[] = [
  { id: 'TR-01', projectId: 'PROJ-PA-201', name: 'Sprint 12 Regression', environment: 'Staging', buildVersion: 'v2.4.0-rc1', status: 'active', passed: 45, failed: 2, skipped: 3, untested: 10, createdAt: '2026-03-20', description: 'Full regression for sprint 12 release candidate' },
  { id: 'TR-02', projectId: 'PROJ-PA-201', name: 'Smoke Test - Production', environment: 'Production', buildVersion: 'v2.3.1', status: 'completed', passed: 30, failed: 0, skipped: 1, untested: 0, createdAt: '2026-03-18', description: 'Post-deployment smoke test' },
  { id: 'TR-03', projectId: 'PROJ-PA-201', name: 'Auth Module Deep Dive', environment: 'QA-Env', buildVersion: 'v2.4.0-beta2', status: 'initial', passed: 0, failed: 0, skipped: 0, untested: 15, createdAt: '2026-03-22', description: 'Targeted testing of authentication module' },
  { id: 'TR-04', projectId: 'PROJ-PA-201', name: 'API v2 Validation', environment: 'Dev', buildVersion: 'v2.4.0-alpha', status: 'active', passed: 12, failed: 5, skipped: 2, untested: 8, createdAt: '2026-03-19', description: 'API v2 endpoint validation' },
];

export const mockDefects: Defect[] = [
  { id: 'DEF-101', projectId: 'PROJ-PA-201', title: 'Login fails with special characters in username', description: 'Users with @ in username cannot authenticate. Returns 401 on valid credentials.', severity: 'Critical', link: 'https://jira.example.com/BUG-101', status: 'Open', source: 'TR-01', createdAt: '2026-03-20' },
  { id: 'DEF-102', projectId: 'PROJ-PA-201', title: 'Dashboard analytics widget timeout on slow connections', description: 'Analytics widget fails to load after 30s on slow connections. No retry mechanism.', severity: 'Major', link: 'https://jira.example.com/BUG-102', status: 'In Progress', source: 'TR-01', createdAt: '2026-03-21' },
  { id: 'DEF-103', projectId: 'PROJ-PA-201', title: 'API rate limit header missing on GET /users', description: 'X-RateLimit-Remaining header not returned on GET /users endpoint.', severity: 'Minor', link: 'https://jira.example.com/BUG-103', status: 'Fixed', source: 'TR-04', createdAt: '2026-03-19' },
  { id: 'DEF-104', projectId: 'PROJ-PA-201', title: 'Session token not invalidated on logout', description: 'JWT token remains valid after logout action. Security vulnerability.', severity: 'Critical', link: 'https://jira.example.com/BUG-104', status: 'Open', source: 'TR-03', createdAt: '2026-03-22' },
  { id: 'DEF-105', projectId: 'PROJ-PA-201', title: 'Pagination returns duplicate entries on page 2', description: 'Page 2 of /users returns 3 duplicate records from page 1.', severity: 'Major', link: 'https://jira.example.com/BUG-105', status: 'Closed', source: 'TR-04', createdAt: '2026-03-18' },
  { id: 'DEF-106', projectId: 'PROJ-PA-201', title: 'MFA code not validated on re-login', description: 'After session expiry, MFA step is skipped on re-authentication.', severity: 'Critical', link: '', status: 'Open', source: 'AS-5', createdAt: '2026-03-23' },
  { id: 'DEF-107', projectId: 'PROJ-PA-201', title: 'Filter state lost on browser back navigation', description: 'Dashboard filters reset when user navigates back from detail view.', severity: 'Minor', link: '', status: 'In Progress', source: 'DB-3', createdAt: '2026-03-22' },
  { id: 'DEF-108', projectId: 'PROJ-PA-201', title: 'POST /users accepts empty email field', description: 'API does not validate required email field, allows user creation without email.', severity: 'Major', link: '', status: 'Open', source: 'API-2', createdAt: '2026-03-24' },
];
