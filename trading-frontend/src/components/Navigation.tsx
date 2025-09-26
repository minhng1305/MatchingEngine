import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navigation: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleUserClick = () => {
        navigate('/profile');
    };

    return (
        <nav style={styles.nav}>
            <div style={styles.container}>
                <h1 style={styles.title}>TradePro</h1>
                <div style={styles.userSection}>
                    {/* ✅ CHANGED: Made username clickable */}
                    <span
                        style={styles.username}
                        onClick={handleUserClick}
                        title="Click to view profile"
                    >
            Welcome, {user?.username}
          </span>
                    <button style={styles.logoutBtn} onClick={logout}>
                        Logout
                    </button>
                </div>
            </div>
        </nav>
    );
};

const styles = {
    nav: {
        backgroundColor: '#1f2937',
        color: 'white',
        padding: '1rem 0',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    },
    container: {
        maxWidth: '1200px',
        margin: '0 auto',
        padding: '0 1rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    title: {
        fontSize: '1.5rem',
        fontWeight: 'bold',
    },
    userSection: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    username: {
        fontSize: '0.9rem',
        cursor: 'pointer', // ✅ ADDED: Make it look clickable
        padding: '0.5rem 0.75rem', // ✅ ADDED: Better click area
        borderRadius: '0.25rem', // ✅ ADDED: Rounded corners
        transition: 'background-color 0.2s', // ✅ ADDED: Smooth hover
        ':hover': {
            backgroundColor: '#374151', // ✅ ADDED: Hover effect
        },
    },
    logoutBtn: {
        backgroundColor: '#dc2626',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.25rem',
        cursor: 'pointer',
        fontSize: '0.9rem',
    },
};

export default Navigation;