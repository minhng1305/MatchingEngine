import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, LoginCredentials, AuthResponse } from '../types';
import { apiService } from '../services/api';

interface RegisterData {
    username: string;
    email: string;
    password: string;
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    loading: boolean;
    login: (credentials: LoginCredentials) => Promise<void>;
    register: (userData: RegisterData) => Promise<void>;
    logout: () => void;
    isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check for existing token on app start
        const savedToken = sessionStorage.getItem('authToken');
        const savedUser = sessionStorage.getItem('user');

        console.log('Checking saved auth data:', { savedToken: !!savedToken, savedUser: !!savedUser });

        if (savedToken && savedUser) {
            try {
                const parsedUser = JSON.parse(savedUser);
                setToken(savedToken);
                setUser(parsedUser);
                apiService.setToken(savedToken);
                console.log('Restored auth state:', { user: parsedUser, token: savedToken });
            } catch (error) {
                console.error('Error parsing saved user data:', error);
                sessionStorage.removeItem('authToken');
                sessionStorage.removeItem('user');
            }
        }
        setLoading(false);
    }, []);

    const login = async (credentials: LoginCredentials): Promise<void> => {
        try {
            console.log('Attempting login with:', credentials.username);
            const response: AuthResponse = await apiService.login(credentials);
            console.log('Login response:', response);

            if (!response.token || !response.user) {
                throw new Error('Invalid response format from server');
            }

            setToken(response.token);
            setUser(response.user);
            apiService.setToken(response.token);

            // Store in sessionStorage
            sessionStorage.setItem('authToken', response.token);
            sessionStorage.setItem('user', JSON.stringify(response.user));

            console.log('Login successful, user authenticated:', response.user);
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    };

    const register = async (userData: RegisterData): Promise<void> => {
        try {
            console.log('Attempting registration with:', userData.username);
            const response: AuthResponse = await apiService.register(userData);
            console.log('Registration response:', response);

            if (!response.token || !response.user) {
                throw new Error('Invalid response format from server');
            }

            setToken(response.token);
            setUser(response.user);
            apiService.setToken(response.token);

            // Store in sessionStorage
            sessionStorage.setItem('authToken', response.token);
            sessionStorage.setItem('user', JSON.stringify(response.user));

            console.log('Registration successful, user authenticated:', response.user);
        } catch (error) {
            console.error('Registration error:', error);
            throw error;
        }
    };

    const logout = (): void => {
        console.log('Logging out user');
        setUser(null);
        setToken(null);
        apiService.setToken('');
        sessionStorage.removeItem('authToken');
        sessionStorage.removeItem('user');
    };

    const value: AuthContextType = {
        user,
        token,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!user && !!token,
    };

    console.log('Auth context state:', {
        isAuthenticated: value.isAuthenticated,
        user: user?.username,
        hasToken: !!token
    });

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};