import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const LoginPage: React.FC = () => {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const { login, register } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            if (isLogin) {
                console.log('Attempting login...');
                await login({ username, password });
                console.log('Login successful, navigating to home');
                navigate('/');
            } else {
                if (password !== confirmPassword) {
                    throw new Error('Passwords do not match');
                }
                if (password.length < 6) {
                    throw new Error('Password must be at least 6 characters long');
                }
                if (!email || !email.includes('@')) {
                    throw new Error('Please enter a valid email address');
                }

                console.log('Attempting registration...');
                await register({ username, email, password });
                setSuccess('Registration successful! Redirecting...');
                setTimeout(() => navigate('/'), 1500);
            }
        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : isLogin ? 'Login failed' : 'Registration failed';
            console.error('Auth error:', errorMessage);
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const toggleMode = () => {
        setIsLogin(!isLogin);
        setError('');
        setSuccess('');
        setUsername('');
        setEmail('');
        setPassword('');
        setConfirmPassword('');
    };

    return (
        <div style={styles.container}>
            <div style={styles.loginBox}>
                <div style={styles.logoSection}>
                    <span style={styles.logoIcon}>📈</span>
                    <h1 style={styles.title}>
                        GreenTrader
                    </h1>
                    <p style={styles.subtitle}>
                        {isLogin ? 'Welcome back' : 'Create your account'}
                    </p>
                </div>

                {error && (
                    <div style={styles.error}>
                        <span style={styles.errorIcon}>⚠️</span>
                        {error}
                    </div>
                )}

                {success && (
                    <div style={styles.success}>
                        <span style={styles.successIcon}>✓</span>
                        {success}
                    </div>
                )}

                <form onSubmit={handleSubmit} style={styles.form}>
                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Username</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            style={styles.input}
                            required
                            disabled={loading}
                            minLength={3}
                            placeholder="Enter your username"
                        />
                    </div>

                    {!isLogin && (
                        <div style={styles.inputGroup}>
                            <label style={styles.label}>Email</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                style={styles.input}
                                required
                                disabled={loading}
                                placeholder="Enter your email"
                            />
                        </div>
                    )}

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            style={styles.input}
                            required
                            disabled={loading}
                            minLength={isLogin ? 1 : 6}
                            placeholder="Enter your password"
                        />
                    </div>

                    {!isLogin && (
                        <div style={styles.inputGroup}>
                            <label style={styles.label}>Confirm Password</label>
                            <input
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                style={styles.input}
                                required
                                disabled={loading}
                                minLength={6}
                                placeholder="Confirm your password"
                            />
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            ...styles.submitButton,
                            opacity: loading ? 0.7 : 1,
                            backgroundColor: isLogin ? '#10b981' : '#3b82f6'
                        }}
                    >
                        {loading ? (isLogin ? 'Logging in...' : 'Creating account...') : (isLogin ? 'Login' : 'Create Account')}
                    </button>
                </form>

                <div style={styles.toggleContainer}>
                    <p style={styles.toggleText}>
                        {isLogin ? "Don't have an account?" : "Already have an account?"}
                    </p>
                    <button
                        type="button"
                        onClick={toggleMode}
                        style={styles.toggleButton}
                        disabled={loading}
                    >
                        {isLogin ? 'Sign up here' : 'Login here'}
                    </button>
                </div>

                {isLogin && (
                    <div style={styles.footer}>
                        <div style={styles.demoCredentials}>
                            <h4 style={styles.demoTitle}>Demo Credentials:</h4>
                            <p style={styles.demoText}>Username: admin</p>
                            <p style={styles.demoText}>Password: password</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#0f172a',
        padding: '1rem',
    },
    loginBox: {
        backgroundColor: '#1e293b',
        padding: '2.5rem',
        borderRadius: '0.75rem',
        boxShadow: '0 10px 25px rgba(0, 0, 0, 0.5)',
        width: '100%',
        maxWidth: '450px',
        border: '1px solid #334155',
    },
    logoSection: {
        textAlign: 'center',
        marginBottom: '2rem',
    },
    logoIcon: {
        fontSize: '3rem',
        display: 'block',
        marginBottom: '0.5rem',
    },
    title: {
        fontSize: '2.5rem',
        fontWeight: '700',
        margin: '0 0 0.5rem 0',
        background: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        backgroundClip: 'text',
    },
    subtitle: {
        fontSize: '1rem',
        color: '#94a3b8',
        margin: 0,
    },
    error: {
        backgroundColor: '#7f1d1d',
        border: '1px solid #991b1b',
        color: '#fca5a5',
        padding: '0.75rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    errorIcon: {
        fontSize: '1.25rem',
    },
    success: {
        backgroundColor: '#14532d',
        border: '1px solid #166534',
        color: '#86efac',
        padding: '0.75rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    successIcon: {
        fontSize: '1.25rem',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem',
    },
    inputGroup: {
        display: 'flex',
        flexDirection: 'column',
    },
    label: {
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#e2e8f0',
        marginBottom: '0.5rem',
    },
    input: {
        padding: '0.75rem',
        border: '1px solid #475569',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        outline: 'none',
        transition: 'border-color 0.2s',
        backgroundColor: '#0f172a',
        color: '#e2e8f0',
    },
    submitButton: {
        color: 'white',
        border: 'none',
        padding: '0.875rem',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        fontWeight: '600',
        cursor: 'pointer',
        marginTop: '0.5rem',
        transition: 'all 0.2s',
    },
    toggleContainer: {
        marginTop: '1.5rem',
        textAlign: 'center',
    },
    toggleText: {
        fontSize: '0.875rem',
        color: '#94a3b8',
        marginBottom: '0.5rem',
    },
    toggleButton: {
        backgroundColor: 'transparent',
        color: '#10b981',
        border: 'none',
        fontSize: '0.875rem',
        fontWeight: '600',
        cursor: 'pointer',
        textDecoration: 'underline',
    },
    footer: {
        marginTop: '1.5rem',
    },
    demoCredentials: {
        backgroundColor: '#0f172a',
        padding: '1rem',
        borderRadius: '0.5rem',
        border: '1px solid #334155',
    },
    demoTitle: {
        fontSize: '0.875rem',
        fontWeight: '600',
        color: '#e2e8f0',
        marginBottom: '0.5rem',
    },
    demoText: {
        fontSize: '0.75rem',
        color: '#94a3b8',
        margin: '0.25rem 0',
    },
};

export default LoginPage;
