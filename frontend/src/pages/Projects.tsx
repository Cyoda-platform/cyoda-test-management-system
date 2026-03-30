import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, ExternalLink, Pencil, Trash2 } from 'lucide-react';
import AppHeader from '@/components/AppHeader';
import { Button } from '@/components/ui/button';
import { mockProjects, type Project } from '@/data/mockData';
import ProjectModal from '@/components/ProjectModal';
import DeleteProjectModal from '@/components/DeleteProjectModal';

const Projects = () => {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<Project[]>(mockProjects);
  const [createOpen, setCreateOpen] = useState(false);
  const [editProject, setEditProject] = useState<Project | null>(null);
  const [deleteProject, setDeleteProject] = useState<Project | null>(null);
  const [page, setPage] = useState(1);
  const perPage = 10;
  const totalPages = Math.ceil(projects.length / perPage);

  const formatDate = (d: string) => {
    const date = new Date(d);
    return date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' }).replace(',', '').toUpperCase().replace(/ /g, '-');
  };

  const handleCreate = (name: string, description: string) => {
    const id = `PROJ-${name.substring(0, 2).toUpperCase()}-${Math.floor(Math.random() * 900) + 100}`;
    setProjects([...projects, { id, name, description, createdAt: new Date().toISOString().split('T')[0], deleted: false, initials: name.substring(0, 2).toUpperCase() }]);
    setCreateOpen(false);
  };

  const handleEdit = (name: string, description: string) => {
    if (!editProject) return;
    setProjects(projects.map((p) => p.id === editProject.id ? { ...p, name, description, initials: name.substring(0, 2).toUpperCase() } : p));
    setEditProject(null);
  };

  const handleDelete = () => {
    if (!deleteProject) return;
    setProjects(projects.filter((p) => p.id !== deleteProject.id));
    setDeleteProject(null);
  };

  return (
    <div className="h-screen flex flex-col">
      <AppHeader />
      <main className="flex-1 overflow-auto surface-base">
        <div className="max-w-7xl mx-auto px-8 py-8 animate-fade-in">
          {/* Header */}
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-2xl font-bold text-foreground tracking-[-0.02em]">Projects</h1>
            <Button onClick={() => setCreateOpen(true)} className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-2 border-0">
              <Plus className="h-4 w-4" strokeWidth={1.5} />
              Create Project
            </Button>
          </div>

          {/* Table — no traditional borders, tonal layering */}
          <div className="bg-card rounded-lg shadow-soft overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-200 dark:bg-slate-700">
                  <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Project Name</th>
                  <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Description</th>
                  <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
                  <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody>
                {projects.slice((page - 1) * perPage, page * perPage).map((project) => (
                  <tr key={project.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 group">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-md bg-accent/10 flex items-center justify-center text-xs font-bold text-accent font-mono">
                          {project.initials}
                        </div>
                        <button
                          onClick={() => navigate(`/projects/${project.id}/repository`)}
                          className="font-semibold text-foreground hover:text-accent transition-colors"
                        >
                          {project.name}
                        </button>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground max-w-xs truncate">{project.description}</td>
                    <td className="px-5 py-3.5 text-muted-foreground whitespace-nowrap font-mono text-xs">{formatDate(project.createdAt)}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground" onClick={() => navigate(`/projects/${project.id}/repository`)}>
                          <ExternalLink className="h-4 w-4" strokeWidth={1.5} />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground" onClick={() => setEditProject(project)}>
                          <Pencil className="h-4 w-4" strokeWidth={1.5} />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-destructive" onClick={() => setDeleteProject(project)}>
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
                Showing {(page - 1) * perPage + 1}-{Math.min(page * perPage, projects.length)} of {projects.length} projects
              </span>
              <div className="flex items-center gap-1">
                <Button variant="ghost" size="sm" disabled={page === 1} onClick={() => setPage(page - 1)}>Prev</Button>
                {Array.from({ length: totalPages }, (_, i) => (
                  <Button key={i} variant={page === i + 1 ? 'default' : 'ghost'} size="sm" onClick={() => setPage(i + 1)} className="w-8">
                    {i + 1}
                  </Button>
                ))}
                <Button variant="ghost" size="sm" disabled={page === totalPages} onClick={() => setPage(page + 1)}>Next</Button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <ProjectModal open={createOpen} onClose={() => setCreateOpen(false)} onSave={handleCreate} mode="create" />
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
        projectName={deleteProject?.name || ''}
      />
    </div>
  );
};

export default Projects;
