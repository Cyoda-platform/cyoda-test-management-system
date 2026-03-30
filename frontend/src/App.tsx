import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import Login from "./pages/Login";
import Projects from "./pages/Projects";
import ProjectLayout from "./components/ProjectLayout";
import Repository from "./pages/Repository";
import TestRuns from "./pages/TestRuns";
import CreateTestRun from "./pages/CreateTestRun";
import RunExecution from "./pages/RunExecution";
import Defects from "./pages/Defects";
import Attachments from "./pages/Attachments";
import Reports from "./pages/Reports";
import CreateReport from "./pages/CreateReport";
import ReportDetail from "./pages/ReportDetail";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/projects/:projectId" element={<ProjectLayout />}>
            <Route index element={<Navigate to="repository" replace />} />
            <Route path="repository" element={<Repository />} />
            <Route path="runs" element={<TestRuns />} />
            <Route path="runs/create" element={<CreateTestRun />} />
            <Route path="runs/:runId" element={<RunExecution />} />
            <Route path="defects" element={<Defects />} />
            <Route path="attachments" element={<Attachments />} />
            <Route path="reports" element={<Reports />} />
            <Route path="reports/create" element={<CreateReport />} />
            <Route path="reports/:reportId" element={<ReportDetail />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
