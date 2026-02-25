import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { Activity, Eye, EyeOff } from 'lucide-react';
import clsx from 'clsx';

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const { login, register, loading, error } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (isRegister) {
        await register(username, email, password);
      } else {
        await login(username, password);
      }
      navigate('/');
    } catch {
      // error is set in store
    }
  };

  return (
    <div className="min-h-screen bg-[#0a0e14] flex items-center justify-center p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-accent/5 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-bull/5 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 mb-3">
            <Activity className="w-8 h-8 text-accent" />
            <span className="text-2xl font-bold tracking-tight">MatchingEngine</span>
          </div>
          <p className="text-sm text-gray-500">Real-time order matching terminal</p>
        </div>

        <div className="panel p-6">
          <div className="flex mb-6 bg-panel-lighter rounded-lg p-1">
            <button
              onClick={() => setIsRegister(false)}
              className={clsx(
                'flex-1 py-2 text-sm font-medium rounded-md transition-colors',
                !isRegister ? 'bg-accent text-white' : 'text-gray-400 hover:text-gray-200'
              )}
            >
              Sign In
            </button>
            <button
              onClick={() => setIsRegister(true)}
              className={clsx(
                'flex-1 py-2 text-sm font-medium rounded-md transition-colors',
                isRegister ? 'bg-accent text-white' : 'text-gray-400 hover:text-gray-200'
              )}
            >
              Register
            </button>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-bear-dim border border-bear/30 rounded-md text-xs text-bear-text">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1.5">Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="input-field"
                placeholder="Enter username"
                required
                autoFocus
              />
            </div>

            {isRegister && (
              <div className="animate-fade-in">
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input-field"
                  placeholder="Enter email"
                  required
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-field pr-10"
                  placeholder="Enter password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full py-2.5">
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Processing...
                </span>
              ) : isRegister ? (
                'Create Account'
              ) : (
                'Sign In'
              )}
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-gray-600 mt-6">
          Secure trading terminal with real-time market data
        </p>
      </div>
    </div>
  );
}
