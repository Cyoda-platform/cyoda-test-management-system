# Frontend - Cyoda Test Management System

React 18 + TypeScript + Vite frontend for the Cyoda Test Management System.

## 🛠️ Tech Stack

- **React 18** – UI framework
- **TypeScript** – Static typing
- **Vite** – Fast build tool and dev server
- **React Router** – Client-side routing
- **React Query (TanStack Query)** – Server state management
- **React Hook Form** – Form handling
- **Tailwind CSS** – Utility-first CSS
- **Radix UI** – Headless component library
- **Vitest** – Unit testing
- **Playwright** – E2E testing

## 📋 Prerequisites

- **Node.js 18+** and **npm** or **pnpm**
- **Backend running** on `http://localhost:8080/api` (started via `start-dev.sh`)

## 🚀 Getting Started

### 1. Install Dependencies

```bash
npm install
# or
pnpm install
```

### 2. Start Development Server

```bash
npm run dev
# or
pnpm dev
```

The app runs on `http://localhost:5173` with proxy to backend API at `/api`.

### 3. Build for Production

```bash
npm run build
# or
pnpm build
```

Output goes to `dist/` directory.

## 📁 Project Structure

```
src/
├── components/       # Reusable React components (UI building blocks)
├── pages/           # Page-level components (routed views)
├── contexts/        # React Context for global state
├── hooks/           # Custom React hooks
├── lib/             # Utilities and helpers (API calls, export/import, etc.)
├── test/            # Test utilities and mocks
├── App.tsx          # Main application component
├── main.tsx         # Application entry point
└── index.css        # Global styles
```

## 🧪 Testing

### Unit Tests (Vitest)

```bash
# Run all tests
npm run test

# Run in watch mode
npm run test:watch
```

### E2E Tests (Playwright)

```bash
# Requires backend running on localhost:8080
npm run test:e2e
```

Test files live in `e2e/` directory.

## 📝 Key Features

- **Project Management** – Create and manage test projects
- **Test Repository** – Organize test suites and cases
- **Test Execution** – Run tests and track results
- **Defect Tracking** – Link bugs to test cases
- **Attachments** – Upload files to cases and defects
- **Unified Search** – Search across all entities
- **Import/Export** – Bulk import test cases from CSV/JSON/XML

## 🔌 API Integration

API calls are handled through the `lib/api.ts` module:

```typescript
import { testCasesApi, suiteApi, projectsApi } from '@/lib/api';

// Example: fetch project
const project = await projectsApi.getById(projectId);
```

API base URL is configured via `VITE_API_URL` env var (defaults to `/api` for dev proxy).

## 🔄 Development Workflow

### Start Everything Together

From the project root:

```bash
./gradlew :apps:backend:build -x test -q && bash start-dev.sh
```

This starts both backend (port 8080) and frontend (port 5173).

### Typical Development Loop

1. Make changes to TypeScript/React code
2. Vite hot-reloads automatically
3. Run tests: `npm run test`
4. Build and verify: `npm run build`

## 🌐 Environment Variables

Create a `.env` file in this directory to override defaults:

```env
VITE_API_URL=/api                    # Backend API base URL
VITE_LOG_LEVEL=debug                 # Optional: debug logging
```

For production builds, use full backend URL if on different host.

## 📚 Documentation

- **Test Utilities** – See `src/test/` for test helpers and mocks
- **Backend API** – Swagger UI at `http://localhost:8080/api/swagger-ui/`
- **Project Guidelines** – See root `CLAUDE.md` and `CONTRIBUTING.md`

## ✅ Quality Gates

Before committing:

```bash
# Lint code
npm run lint

# Run tests
npm run test

# Build for production
npm run build
```

All checks must pass before code review.
