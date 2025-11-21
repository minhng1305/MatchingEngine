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
                <h1 style={styles.title}>
                    {isLogin ? 'GreenTrader Login' : 'GreenTrader Register'}
                </h1>

                {error && (
                    <div style={styles.error}>
                        {error}
                    </div>
                )}

                {success && (
                    <div style={styles.success}>
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
                            backgroundColor: isLogin ? '#3b82f6' : '#16a34a'
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
                            <h4>Demo Credentials:</h4>
                            <p>Username: admin</p>
                            <p>Password: password</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

const styles = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f3f4f6',
        padding: '1rem',
    },
    loginBox: {
        backgroundColor: 'white',
        padding: '2rem',
        borderRadius: '0.5rem',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        width: '100%',
        maxWidth: '400px',
    },
    title: {
        fontSize: '2rem',
        fontWeight: 'bold',
        textAlign: 'center' as const,
        marginBottom: '2rem',
        color: '#1f2937',
    },
    error: {
        backgroundColor: '#fef2f2',
        border: '1px solid #fecaca',
        color: '#dc2626',
        padding: '0.75rem',
        borderRadius: '0.25rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
    },
    success: {
        backgroundColor: '#f0fdf4',
        border: '1px solid #bbf7d0',
        color: '#16a34a',
        padding: '0.75rem',
        borderRadius: '0.25rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
    },
    form: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '1rem',
    },
    inputGroup: {
        display: 'flex',
        flexDirection: 'column' as const,
    },
    label: {
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#374151',
        marginBottom: '0.25rem',
    },
    input: {
        padding: '0.75rem',
        border: '1px solid #d1d5db',
        borderRadius: '0.25rem',
        fontSize: '1rem',
        outline: 'none',
        transition: 'border-color 0.2s',
    },
    submitButton: {
        color: 'white',
        border: 'none',
        padding: '0.75rem',
        borderRadius: '0.25rem',
        fontSize: '1rem',
        fontWeight: '500',
        cursor: 'pointer',
        marginTop: '0.5rem',
        transition: 'background-color 0.2s',
    },
    toggleContainer: {
        marginTop: '1.5rem',
        textAlign: 'center' as const,
    },
    toggleText: {
        fontSize: '0.875rem',
        color: '#6b7280',
        marginBottom: '0.5rem',
    },
    toggleButton: {
        backgroundColor: 'transparent',
        color: '#3b82f6',
        border: 'none',
        fontSize: '0.875rem',
        fontWeight: '500',
        cursor: 'pointer',
        textDecoration: 'underline',
    },
    footer: {
        marginTop: '1.5rem',
    },
    demoCredentials: {
        backgroundColor: '#f9fafb',
        padding: '1rem',
        borderRadius: '0.25rem',
        fontSize: '0.875rem',
    },
};

export default LoginPage;