import { useNavigate, useLocation } from 'react-router-dom';
import {
  FolderOpen, Play, Bug, Paperclip, BarChart3, ArrowLeft, PanelLeftClose, PanelLeft,
} from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger, TooltipProvider } from '@/components/ui/tooltip';

interface AppSidebarProps {
  projectName: string;
  projectId: string;
  projectInitials: string;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}

const navItems = [
  { label: 'Repository', icon: FolderOpen, path: 'repository' },
  { label: 'Test Runs', icon: Play, path: 'runs' },
  { label: 'Defects', icon: Bug, path: 'defects' },
  { label: 'Reports', icon: BarChart3, path: 'reports' },
  { label: 'Attachments', icon: Paperclip, path: 'attachments' },
];

const bottomItems = [
  { label: 'Back to Projects', icon: ArrowLeft, action: 'back' },
];

const AppSidebar = ({ projectName, projectId, projectInitials, collapsed = false, onToggleCollapse }: AppSidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  

  const basePath = `/projects/${projectId}`;

  const handleBottomAction = (action: string) => {
    if (action === 'back') navigate('/projects');
  };

  const NavButton = ({ icon: Icon, label, isActive, onClick }: { icon: typeof FolderOpen; label: string; isActive?: boolean; onClick: () => void }) => {
    const btn = (
      <button
        onClick={onClick}
        className={`w-full flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors ${
          collapsed ? 'justify-center' : ''
        } ${
          isActive
            ? 'bg-sidebar-accent text-sidebar-accent-foreground font-medium'
            : 'hover:bg-sidebar-accent/50 text-sidebar-foreground'
        }`}
      >
        <Icon className="h-4 w-4 shrink-0" strokeWidth={1.5} />
        {!collapsed && label}
      </button>
    );

    if (collapsed) {
      return (
        <Tooltip>
          <TooltipTrigger asChild>{btn}</TooltipTrigger>
          <TooltipContent side="right" className="text-xs">{label}</TooltipContent>
        </Tooltip>
      );
    }
    return btn;
  };

  return (
    <TooltipProvider delayDuration={0}>
      <aside className={`bg-sidebar-deep text-sidebar-foreground flex flex-col shrink-0 h-full transition-all duration-200 border-r border-white/5 ${collapsed ? 'w-14' : 'w-56'}`}>
        {/* Project context */}
        <div className={`p-4 ${collapsed ? 'px-2 flex justify-center' : ''}`}>
          {collapsed ? (
            <Tooltip>
              <TooltipTrigger asChild>
                <div className="h-8 w-8 rounded-md bg-accent flex items-center justify-center text-xs font-bold text-accent-foreground cursor-default">
                  {projectInitials}
                </div>
              </TooltipTrigger>
              <TooltipContent side="right" className="text-xs">{projectName}</TooltipContent>
            </Tooltip>
          ) : (
            <div className="flex items-center gap-2.5">
              <div className="h-8 w-8 rounded-md bg-accent flex items-center justify-center text-xs font-bold text-accent-foreground">
                {projectInitials}
              </div>
              <span className="text-sm font-semibold text-sidebar-accent-foreground truncate">{projectName}</span>
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className={`flex-1 space-y-0.5 ${collapsed ? 'px-1' : 'px-3'}`}>
          {navItems.map((item) => {
            const fullPath = `${basePath}/${item.path}`;
            const isActive = location.pathname.startsWith(fullPath);
            return (
              <NavButton
                key={item.path}
                icon={item.icon}
                label={item.label}
                isActive={isActive}
                onClick={() => navigate(fullPath)}
              />
            );
          })}
        </nav>

        {/* Bottom section */}
        <div className={`space-y-0.5 ${collapsed ? 'p-1' : 'p-3'}`}>
          {bottomItems.map((item) => (
            <NavButton
              key={item.action}
              icon={item.icon}
              label={item.label}
              onClick={() => handleBottomAction(item.action)}
            />
          ))}

          {/* Collapse toggle */}
          <div className={`pt-1 ${collapsed ? '' : 'border-t border-sidebar-accent/30 mt-1'}`}>
            {collapsed ? (
              <Tooltip>
                <TooltipTrigger asChild>
                  <button
                    onClick={onToggleCollapse}
                    className="w-full flex items-center justify-center px-3 py-2 rounded-md text-sm hover:bg-sidebar-accent/50 text-sidebar-foreground transition-colors"
                  >
                    <PanelLeft className="h-4 w-4" strokeWidth={1.5} />
                  </button>
                </TooltipTrigger>
                <TooltipContent side="right" className="text-xs">Expand sidebar</TooltipContent>
              </Tooltip>
            ) : (
              <button
                onClick={onToggleCollapse}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-md text-sm hover:bg-sidebar-accent/50 text-sidebar-foreground transition-colors"
              >
                <PanelLeftClose className="h-4 w-4" strokeWidth={1.5} />
                Collapse
              </button>
            )}
          </div>
        </div>
      </aside>

      
    </TooltipProvider>
  );
};

export default AppSidebar;
