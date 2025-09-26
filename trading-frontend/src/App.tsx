import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import HomePage from './pages/HomePage';
import StockDetailPage from './pages/StockDetailPage';
import LoginPage from './pages/LoginPage';
import Navigation from './components/Navigation';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return (
            <div style={styles.loading}>
                <div>Loading...</div>
            </div>
        );
    }

    return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
};

const AppRoutes: React.FC = () => {
    const { isAuthenticated } = useAuth();

    return (
        <div style={styles.app}>
            {isAuthenticated && <Navigation />}
            <main style={styles.main}>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <HomePage />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/stock/:symbol"
                        element={
                            <ProtectedRoute>
                                <StockDetailPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </main>
        </div>
    );
};

const App: React.FC = () => {
    return (
        <AuthProvider>
            <Router>
                <AppRoutes />
            </Router>
        </AuthProvider>
    );
};

const styles = {
    app: {
        minHeight: '100vh',
        backgroundColor: '#f5f5f5',
    },
    main: {
        minHeight: '100vh',
    },
    loading: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        fontSize: '1.125rem',
        color: '#6b7280',
    },
};

export default App;