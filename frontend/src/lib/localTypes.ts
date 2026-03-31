/**
 * Local view-layer types for Repository page.
 *
 * These mirror the API DTOs but add UI-only shape requirements:
 *  - LocalSuite has `cases` nested (flattened by the API, re-assembled client-side)
 *  - LocalCase  has `steps` nested (loaded lazily, empty array by default)
 *  - LocalStep  adds optional `id` so existing steps can be updated/deleted by key
 */

export interface LocalStep {
  id?: string;           // API UUID; absent for newly-added steps
  order: number;         // maps to stepNumber in the backend DTO
  action: string;
  expectedResult: string;
  status: string;
}

export interface LocalCase {
  id: string;
  suiteId: string;
  title: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  preconditions: string;
  steps: LocalStep[];    // empty until case is selected and steps are fetched
  deleted: boolean;
}

export interface LocalSuite {
  id: string;
  projectId: string;
  name: string;
  cases: LocalCase[];    // populated from GET /projects/:id/suites/:sid/cases
}
