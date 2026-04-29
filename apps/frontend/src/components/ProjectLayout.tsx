import { Outlet, useParams } from 'react-router-dom';
import useLocalStorage from '@/hooks/useLocalStorage';
import AppHeader from './AppHeader';
import AppSidebar from './AppSidebar';
import { useQuery } from '@tanstack/react-query';
import { projectsApi } from '@/lib/api';
import { Loader2 } from 'lucide-react';

const ProjectLayout = () => {
  const { projectId } = useParams();
  const [sidebarCollapsed, setSidebarCollapsed] = useLocalStorage(
    `tms.sidebar.collapsed.${projectId}`,
    false,
  );

  // Fetch project from API
  const { data: project, isLoading, isError, error } = useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => projectsApi.get(projectId!),
    enabled: !!projectId,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (isError || !project) {
    return (
      <div className="flex items-center justify-center min-h-screen text-muted-foreground">
        Project not found {error && `(${(error as any).message})`}
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col print:h-auto">
      <div className="print:hidden"><AppHeader /></div>
      <div className="flex flex-1 overflow-hidden print:block print:overflow-visible">
        <div className="print:hidden">
          <AppSidebar
            projectName={project.name}
            projectId={project.id}
            projectInitials={project.name.substring(0, 2).toUpperCase()}
            collapsed={sidebarCollapsed}
            onToggleCollapse={() => setSidebarCollapsed((c) => !c)}
          />
        </div>
        <main className="flex-1 overflow-hidden surface-base print:overflow-visible print:flex-none">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default ProjectLayout;
