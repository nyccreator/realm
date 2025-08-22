// Authentication Context for Section 3.1 - Single-User Authentication
// Provides authentication state management and token persistence

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { User, AuthContextType } from '../types/auth';
import AuthService from '../services/authService';

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
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
  const authService = AuthService.getInstance();

  useEffect(() => {
    // Check for existing token on app startup
    const initializeAuth = async () => {
      const storedToken = authService.getStoredAccessToken();
      const storedUser = authService.getStoredUser();
      
      if (storedToken && storedUser) {
        try {
          // Validate token with the backend
          const validationResponse = await authService.validateToken(storedToken);
          
          if (validationResponse.valid && validationResponse.user) {
            // Token is valid, set authentication state
            setToken(storedToken);
            setUser(validationResponse.user);
            // Update stored user data in case it changed
            authService.storeUser(validationResponse.user);
          } else {
            // Token is invalid, try to refresh
            const refreshToken = authService.getStoredRefreshToken();
            if (refreshToken) {
              const refreshSuccess = await attemptTokenRefresh(refreshToken);
              if (!refreshSuccess) {
                // Refresh failed, clear all data
                clearAuthState();
              }
            } else {
              // No refresh token, clear all data
              clearAuthState();
            }
          }
        } catch (error) {
          console.error('Error validating token:', error);
          // Try to refresh token on validation error
          const refreshToken = authService.getStoredRefreshToken();
          if (refreshToken) {
            const refreshSuccess = await attemptTokenRefresh(refreshToken);
            if (!refreshSuccess) {
              clearAuthState();
            }
          } else {
            clearAuthState();
          }
        }
      } else {
        // No stored token or user, clear any partial data
        clearAuthState();
      }
      
      setLoading(false);
    };

    initializeAuth();
  }, []);

  const attemptTokenRefresh = async (refreshToken: string): Promise<boolean> => {
    try {
      const refreshResponse = await authService.refreshToken({ refreshToken });
      
      // Store new tokens and user data
      authService.storeTokens(refreshResponse.accessToken, refreshResponse.refreshToken);
      authService.storeUser(refreshResponse.user);
      
      // Update state
      setToken(refreshResponse.accessToken);
      setUser(refreshResponse.user);
      
      return true;
    } catch (error) {
      console.error('Token refresh failed:', error);
      return false;
    }
  };

  const clearAuthState = () => {
    authService.clearAllStoredData();
    setToken(null);
    setUser(null);
  };

  const login = async (email: string, password: string): Promise<void> => {
    try {
      const response = await authService.login({ email, password });
      
      // Store tokens and user data
      authService.storeTokens(response.accessToken, response.refreshToken);
      authService.storeUser(response.user);
      
      // Update state
      setToken(response.accessToken);
      setUser(response.user);
    } catch (error) {
      // Re-throw error to be handled by the component
      throw error;
    }
  };

  const register = async (email: string, password: string, displayName: string): Promise<void> => {
    try {
      const response = await authService.register({ email, password, displayName });
      
      // Store tokens and user data
      authService.storeTokens(response.accessToken, response.refreshToken);
      authService.storeUser(response.user);
      
      // Update state
      setToken(response.accessToken);
      setUser(response.user);
    } catch (error) {
      // Re-throw error to be handled by the component
      throw error;
    }
  };

  const logout = () => {
    // Clear all authentication data
    clearAuthState();
  };

  const refreshTokenMethod = async (): Promise<boolean> => {
    const refreshToken = authService.getStoredRefreshToken();
    if (!refreshToken) {
      return false;
    }
    
    return await attemptTokenRefresh(refreshToken);
  };

  const contextValue: AuthContextType = {
    user,
    token,
    isAuthenticated: !!user && !!token,
    loading,
    login,
    register,
    logout,
    refreshToken: refreshTokenMethod,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};