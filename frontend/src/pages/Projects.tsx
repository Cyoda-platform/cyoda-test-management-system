import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, ExternalLink, Pencil, Trash2, Loader2, AlertCircle, FolderOpen } from 'lucide-react';
import { toast } from 'sonner';
import AppHeader from '@/components/AppHeader';
import { Button } from '@/components/ui/button';
import ProjectModal from '@/components/ProjectModal';
import DeleteProjectModal from '@/components/DeleteProjectModal';
import {
  useProjects,
  useCreateProject,
  useUpdateProject,
  useDeleteProject,
} from '@/hooks/useApi';
import type { Project } from '@/lib/api';

const PAGE_SIZE = 10;

/** Derive two-letter initials from a project name */
const initials = (name: string) => name.substring(0, 2).toUpperCase();

const formatDate = (d: string) =>
  new Date(d)
    .toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })
    .replace(',', '')
    .toUpperCase()
    .replace(/ /g, '-');

const Projects = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0); // 0-indexed for API
  const [createOpen, setCreateOpen] = useState(false);
  const [editProject, setEditProject] = useState<Project | null>(null);
  const [deleteProject, setDeleteProject] = useState<Project | null>(null);

  const { data: projects = [], isLoading, isError, error } = useProjects(page);
  const createProject = useCreateProject();
  const updateProject = useUpdateProject();
  const deleteProjectMutation = useDeleteProject();

  const currentPage = page + 1; // 1-indexed for UI
  // Backend doesn't return total count yet — disable "Next" when fewer than PAGE_SIZE items came back
  const hasNextPage = projects.length === PAGE_SIZE;
  const totalShown = page * PAGE_SIZE + projects.length;

  const handleCreate = (name: string, description: string) => {
    createProject.mutate(
      { name, description },
      {
        onSuccess: () => {
          toast.success('Project created');
          setCreateOpen(false);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const handleEdit = (name: string, description: string) => {
    if (!editProject) return;
    updateProject.mutate(
      { id: editProject.id, body: { name, description } },
      {
        onSuccess: () => {
          toast.success('Project updated');
          setEditProject(null);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const handleDelete = () => {
    if (!deleteProject) return;
    deleteProjectMutation.mutate(deleteProject.id, {
      onSuccess: () => {
        toast.success('Project deleted');
        setDeleteProject(null);
        // If we deleted the last item on a page beyond 0, go back one page
        if (projects.length === 1 && page > 0) setPage(page - 1);
      },
      onError: (e) => toast.error(e.message),
    });
  };

  return (
    <div className="h-screen flex flex-col">
      <AppHeader />
      <main className="flex-1 overflow-auto surface-base">
        <div className="max-w-7xl mx-auto px-8 py-8 animate-fade-in">
          {/* Header */}
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-2xl font-bold text-foreground tracking-[-0.02em]">Projects</h1>
            <Button
              onClick={() => setCreateOpen(true)}
              className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
            >
              <Plus className="h-4 w-4" strokeWidth={1.5} />
              Create Project
            </Button>
          </div>

          {/* Loading */}
          {isLoading && (
            <div className="flex items-center justify-center py-20 text-muted-foreground gap-2">
              <Loader2 className="h-5 w-5 animate-spin" />
              <span>Loading projects…</span>
            </div>
          )}

          {/* Error */}
          {isError && (
            <div className="flex items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/10 px-5 py-4 text-destructive">
              <AlertCircle className="h-5 w-5 shrink-0" strokeWidth={1.5} />
              <span className="text-sm">{(error as Error).message}</span>
            </div>
          )}

          {/* Empty state */}
          {!isLoading && !isError && projects.length === 0 && (
            <div className="flex flex-col items-center justify-center py-24 gap-4 text-muted-foreground">
              <FolderOpen className="h-12 w-12 opacity-30" strokeWidth={1} />
              <p className="text-sm">No projects yet. Create your first one.</p>
              <Button
                onClick={() => setCreateOpen(true)}
                className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0"
              >
                <Plus className="h-4 w-4" strokeWidth={1.5} />
                Create Project
              </Button>
            </div>
          )}

          {/* Table */}
          {!isLoading && !isError && projects.length > 0 && (
            <div className="bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-200 dark:bg-slate-700">
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">
                      Project Name
                    </th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">
                      Description
                    </th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">
                      Created
                    </th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {projects.map((project) => (
                    <tr
                      key={project.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 group"
                    >
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="h-8 w-8 rounded-md bg-accent/10 flex items-center justify-center text-xs font-bold text-accent font-mono">
                            {initials(project.name)}
                          </div>
                          <button
                            onClick={() => navigate(`/projects/${project.id}/repository`)}
                            className="font-semibold text-foreground hover:text-accent transition-colors"
                          >
                            {project.name}
                          </button>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground max-w-xs truncate">
                        {project.description}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground whitespace-nowrap font-mono text-xs">
                        {formatDate(project.createdAt)}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-foreground"
                            onClick={() => navigate(`/projects/${project.id}/repository`)}
                          >
                            <ExternalLink className="h-4 w-4" strokeWidth={1.5} />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-foreground"
                            onClick={() => setEditProject(project)}
                          >
                            <Pencil className="h-4 w-4" strokeWidth={1.5} />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-destructive"
                            onClick={() => setDeleteProject(project)}
                          >
                            <Trash2 className="h-4 w-4" strokeWidth={1.5} />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Pagination */}
              <div className="flex items-center justify-between px-5 py-3 bg-slate-100 dark:bg-slate-800">
                <span className="text-sm text-muted-foreground">
                  Showing {page * PAGE_SIZE + 1}–{totalShown}
                </span>
                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    Prev
                  </Button>
                  <Button variant="default" size="sm" className="w-8">
                    {currentPage}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={!hasNextPage}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      <ProjectModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSave={handleCreate}
        mode="create"
      />
      {editProject && (
        <ProjectModal
          open={!!editProject}
          onClose={() => setEditProject(null)}
          onSave={handleEdit}
          mode="edit"
          initialName={editProject.name}
          initialDescription={editProject.description}
          projectId={editProject.id}
        />
      )}
      <DeleteProjectModal
        open={!!deleteProject}
        onClose={() => setDeleteProject(null)}
        onConfirm={handleDelete}
        projectName={deleteProject?.name ?? ''}
      />
    </div>
  );
};

export default Projects;
