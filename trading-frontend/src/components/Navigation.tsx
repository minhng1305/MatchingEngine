import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navigation: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleUserClick = () => {
        navigate('/profile');
    };

    const handleLogoClick = () => {
        navigate('/');
    };

    return (
        <nav style={styles.nav}>
            <div style={styles.container}>
                <div style={styles.logoSection} onClick={handleLogoClick}>
                    <span style={styles.logoIcon}>📈</span>
                    <h1 style={styles.title}>GreenTrader</h1>
                </div>
                <div style={styles.userSection}>
                    <span
                        style={styles.username}
                        onClick={handleUserClick}
                        title="Click to view profile"
                    >
                        {user?.username}
                    </span>
                    <button style={styles.logoutBtn} onClick={logout}>
                        Logout
                    </button>
                </div>
            </div>
        </nav>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    nav: {
        backgroundColor: '#1e293b',
        color: 'white',
        padding: '1rem 0',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
        borderBottom: '1px solid #334155',
    },
    container: {
        maxWidth: '1600px',
        margin: '0 auto',
        padding: '0 2rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    logoSection: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        cursor: 'pointer',
    },
    logoIcon: {
        fontSize: '1.75rem',
    },
    title: {
        fontSize: '1.5rem',
        fontWeight: '700',
        margin: 0,
        background: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        backgroundClip: 'text',
    },
    userSection: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    username: {
        fontSize: '0.9rem',
        cursor: 'pointer',
        padding: '0.5rem 1rem',
        borderRadius: '0.5rem',
        transition: 'background-color 0.2s',
        backgroundColor: '#334155',
        fontWeight: '500',
        color: '#e2e8f0',
    },
    logoutBtn: {
        backgroundColor: '#ef4444',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.5rem',
        cursor: 'pointer',
        fontSize: '0.9rem',
        fontWeight: '500',
        transition: 'all 0.2s',
    },
};

// Add hover effect via CSS
const styleSheet = document.createElement('style');
styleSheet.textContent = `
    .nav-username:hover {
        background-color: #475569 !important;
    }
    .nav-logout:hover {
        background-color: #dc2626 !important;
        transform: translateY(-1px);
    }
`;
if (!document.getElementById('nav-styles')) {
    styleSheet.id = 'nav-styles';
    document.head.appendChild(styleSheet);
}

export default Navigation;
