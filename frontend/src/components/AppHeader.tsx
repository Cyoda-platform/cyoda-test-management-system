import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, LogOut, Shield } from 'lucide-react';
import GlobalSearch from './GlobalSearch';
import LogoutModal from './LogoutModal';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

const AppHeader = () => {
  const [showLogout, setShowLogout] = useState(false);

  return (
    <header className="h-14 bg-sidebar-deep text-header-foreground flex items-center px-6 shrink-0 z-50 border-b border-white/5" style={{ boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.3)' }}>
      <span className="text-lg font-bold tracking-[-0.02em]">CYODA TMS</span>

      <div className="ml-auto flex items-center gap-3">
        <GlobalSearch />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="h-8 w-8 rounded-md bg-accent flex items-center justify-center hover:bg-accent/80 transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-accent">
              <User className="h-4 w-4 text-accent-foreground" strokeWidth={1.5} />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuLabel className="flex items-center gap-2 text-xs font-normal text-muted-foreground">
              <Shield className="h-3.5 w-3.5" strokeWidth={1.5} />
              Role: Admin
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={() => setShowLogout(true)}
              className="text-destructive focus:text-destructive cursor-pointer"
            >
              <LogOut className="mr-2 h-4 w-4" strokeWidth={1.5} />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <LogoutModal open={showLogout} onClose={() => setShowLogout(false)} />
    </header>
  );
};

export default AppHeader;
