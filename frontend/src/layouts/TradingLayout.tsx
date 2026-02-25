import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import {
  LayoutDashboard,
  CandlestickChart,
  Briefcase,
  LogOut,
  LogIn,
  Activity,
} from 'lucide-react';
import clsx from 'clsx';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Markets' },
  { to: '/trade/AAPL', icon: CandlestickChart, label: 'Trade' },
  { to: '/portfolio', icon: Briefcase, label: 'Portfolio' },
];

export default function TradingLayout() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="h-screen flex flex-col overflow-hidden">
      {/* Top bar */}
      <header className="flex-shrink-0 h-12 bg-panel border-b border-panel-border flex items-center justify-between px-4 z-50">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-accent" />
            <span className="font-bold text-sm tracking-wide">MatchingEngine</span>
          </div>

          <nav className="flex items-center gap-1">
            {navItems.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  clsx(
                    'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors',
                    isActive
                      ? 'bg-accent/10 text-accent'
                      : 'text-gray-400 hover:text-gray-200 hover:bg-panel-lighter'
                  )
                }
              >
                <Icon className="w-3.5 h-3.5" />
                {label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {user ? (
            <>
              <span className="text-xs text-gray-400">
                <span className="text-gray-300 font-medium">{user.username}</span>
              </span>
              <button
                onClick={handleLogout}
                className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-300 transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </>
          ) : (
            <NavLink
              to="/login"
              className="flex items-center gap-1 text-xs text-gray-400 hover:text-accent transition-colors"
            >
              <LogIn className="w-3.5 h-3.5" />
              Sign In
            </NavLink>
          )}
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}
