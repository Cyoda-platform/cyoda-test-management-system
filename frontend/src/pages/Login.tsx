import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, LayoutGrid } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/contexts/AuthContext';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setError('Please enter both username and password.');
      return;
    }
    setIsLoading(true);
    setError('');
    try {
      await login(username, password);
      navigate('/projects');
    } catch {
      setError('Invalid username or password.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center surface-low">
      <div className="w-full max-w-sm animate-fade-in">
        {/* Branding */}
        <div className="flex items-center justify-center gap-2.5 mb-8">
          <LayoutGrid className="h-8 w-8 text-foreground" strokeWidth={1.5} />
          <span className="text-2xl font-bold text-foreground tracking-[-0.02em]">CYODA TMS</span>
        </div>

        {/* Credential Card — tonal layering, no border */}
        <div className="bg-card rounded-lg shadow-ambient p-8">
          <form onSubmit={handleLogin} className="space-y-5">
            <div className="space-y-1.5">
              <Label className="text-[10px] font-semibold tracking-widest text-muted-foreground uppercase font-mono">
                Username
              </Label>
              <Input
                type="text"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => { setUsername(e.target.value); setError(''); }}
                className="bg-secondary border-0 h-11 focus-visible:ring-1 focus-visible:ring-accent/40"
                disabled={isLoading}
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-[10px] font-semibold tracking-widest text-muted-foreground uppercase font-mono">
                Password
              </Label>
              <div className="relative">
                <Input
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); setError(''); }}
                  className="bg-secondary border-0 h-11 pr-10 focus-visible:ring-1 focus-visible:ring-accent/40"
                  disabled={isLoading}
                />
                <Lock className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
              </div>
            </div>

            {error && (
              <p className="text-destructive text-sm">{error}</p>
            )}

            <Button
              type="submit"
              className="w-full h-11 bg-gradient-to-br from-primary to-primary/80 text-primary-foreground font-semibold border-0"
              disabled={isLoading}
            >
              {isLoading ? 'Signing in…' : 'Sign In'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Login;
