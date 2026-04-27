import { lazy, Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import Login from "./pages/Login";
import Projects from "./pages/Projects";
import ProjectLayout from "./components/ProjectLayout";
import TestRuns from "./pages/TestRuns";
import CreateTestRun from "./pages/CreateTestRun";
import NotFound from "./pages/NotFound";

const Repository   = lazy(() => import("./pages/Repository"));
const RunExecution = lazy(() => import("./pages/RunExecution"));
const Defects      = lazy(() => import("./pages/Defects"));
const Attachments  = lazy(() => import("./pages/Attachments"));
const Reports      = lazy(() => import("./pages/Reports"));
const CreateReport = lazy(() => import("./pages/CreateReport"));
const ReportDetail = lazy(() => import("./pages/ReportDetail"));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center">
    <span className="text-muted-foreground text-sm">Loading…</span>
  </div>
);

/** Redirects to login if the user is not authenticated. */
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <span className="text-muted-foreground text-sm">Loading…</span>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<Login />} />

            <Route path="/projects" element={
              <ProtectedRoute><Projects /></ProtectedRoute>
            } />

            <Route path="/projects/:projectId" element={
              <ProtectedRoute><ProjectLayout /></ProtectedRoute>
            }>
              <Route index element={<Navigate to="repository" replace />} />
              <Route path="repository" element={
                <Suspense fallback={<PageLoader />}><Repository /></Suspense>
              } />
              <Route path="runs"            element={<TestRuns />} />
              <Route path="runs/create"     element={<CreateTestRun />} />
              <Route path="runs/:runId" element={
                <Suspense fallback={<PageLoader />}><RunExecution /></Suspense>
              } />
              <Route path="defects" element={
                <Suspense fallback={<PageLoader />}><Defects /></Suspense>
              } />
              <Route path="attachments" element={
                <Suspense fallback={<PageLoader />}><Attachments /></Suspense>
              } />
              <Route path="reports" element={
                <Suspense fallback={<PageLoader />}><Reports /></Suspense>
              } />
              <Route path="reports/create" element={
                <Suspense fallback={<PageLoader />}><CreateReport /></Suspense>
              } />
              <Route path="reports/:reportId" element={
                <Suspense fallback={<PageLoader />}><ReportDetail /></Suspense>
              } />
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
