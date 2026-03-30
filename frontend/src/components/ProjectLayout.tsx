import { useState } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import AppHeader from './AppHeader';
import AppSidebar from './AppSidebar';
import { mockProjects } from '@/data/mockData';

const ProjectLayout = () => {
  const { projectId } = useParams();
  const project = mockProjects.find((p) => p.id === projectId);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  if (!project) {
    return <div className="flex items-center justify-center min-h-screen text-muted-foreground">Project not found</div>;
  }

  return (
    <div className="h-screen flex flex-col">
      <AppHeader />
      <div className="flex flex-1 overflow-hidden">
        <AppSidebar
          projectName={project.name}
          projectId={project.id}
          projectInitials={project.initials}
          collapsed={sidebarCollapsed}
          onToggleCollapse={() => setSidebarCollapsed((c) => !c)}
        />
        <main className="flex-1 overflow-auto surface-base">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default ProjectLayout;
