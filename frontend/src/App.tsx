import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useEffect, useState } from 'react';
import TradingLayout from './layouts/TradingLayout';
import DashboardPage from './pages/DashboardPage';
import TradingPage from './pages/TradingPage';
import PortfolioPage from './pages/PortfolioPage';
import LoginPage from './pages/LoginPage';
import { useAuthStore } from './stores/authStore';

export default function App() {
  const restoreSession = useAuthStore((s) => s.restoreSession);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    restoreSession();
    setReady(true);
  }, [restoreSession]);

  if (!ready) return null;

  return (
    <BrowserRouter>
      <Routes>
        <Route element={<TradingLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/trade/:symbol" element={<TradingPage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
        </Route>
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  );
}
