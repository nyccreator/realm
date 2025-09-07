// Authentication Context for Section 3.1 - Single-User Authentication
// Updated for Redis session-based authentication (no JWT tokens)

import React, {createContext, ReactNode, useCallback, useContext, useEffect, useState} from 'react';
import {AuthContextType, User} from '../types/auth';
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
  const [loading, setLoading] = useState(true);
  const authService = AuthService.getInstance();

  const clearAuthState = useCallback(() => {
    console.log('Clearing authentication state...');
    authService.clearAllStoredData();
    setUser(null);
    console.log('Authentication state cleared - user should be logged out');
  }, [authService]);

  useEffect(() => {
    // Check for existing session on app startup
    const initializeAuth = async () => {
      const storedUser = authService.getStoredUser();
      
      if (storedUser) {
        // Check if this is a demo user (skip backend validation)
        if (storedUser.email === 'demo@realm.dev') {
          console.log('Demo mode detected, skipping backend validation');
          setUser(storedUser);
          setLoading(false);
          return;
        }
        
        // We have stored user data, validate the session with backend
        console.log('Validating stored user session with backend...');
        try {
          const validationResponse = await authService.validateSession();
          if (validationResponse.valid && validationResponse.user) {
            // Session is valid, set auth state
            setUser(validationResponse.user);
            // Update stored user data in case it changed
            authService.storeUser(validationResponse.user);
            console.log('Session validation successful');
          } else {
            // Session is invalid, clear auth state
            console.log('Session validation failed, clearing auth state');
            clearAuthState();
          }
        } catch (error) {
          console.error('Session validation error:', error);
          // On validation error, clear auth state
          clearAuthState();
        }
      } else {
        // No stored user data, try to validate session anyway (in case of page reload)
        try {
          const validationResponse = await authService.validateSession();
          if (validationResponse.valid && validationResponse.user) {
            // Unexpectedly valid session, restore user data
            setUser(validationResponse.user);
            authService.storeUser(validationResponse.user);
            console.log('Restored session from server validation');
          } else {
            // No valid session, ensure clean state
            clearAuthState();
          }
        } catch (error) {
          // No valid session, ensure clean state
          clearAuthState();
        }
      }
      
      setLoading(false);
    };

    initializeAuth();
  }, [authService, clearAuthState]);

  const login = async (email: string, password: string): Promise<void> => {
    try {
      const response = await authService.login({ email, password });
      
      // Store user data locally for UI persistence
      authService.storeUser(response.user);
      
      // Update state (session cookie set automatically by server)
      setUser(response.user);
    } catch (error) {
      // Re-throw error to be handled by the component
      throw error;
    }
  };

  const register = async (email: string, password: string, displayName: string): Promise<void> => {
    try {
      const response = await authService.register({ email, password, displayName });
      
      // Store user data locally for UI persistence
      authService.storeUser(response.user);
      
      // Update state (session cookie set automatically by server)
      setUser(response.user);
    } catch (error) {
      // Re-throw error to be handled by the component
      throw error;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      // Call logout endpoint to destroy session on server
      await authService.logout();
    } catch (error) {
      console.error('Server logout failed:', error);
      // Continue with client-side cleanup regardless
    }
    
    // Clear client-side authentication data
    clearAuthState();
  };

  const validateSession = async (): Promise<boolean> => {
    try {
      const validationResponse = await authService.validateSession();
      if (validationResponse.valid && validationResponse.user) {
        // Update user data if session is valid
        setUser(validationResponse.user);
        authService.storeUser(validationResponse.user);
        return true;
      } else {
        // Session invalid, clear state
        clearAuthState();
        return false;
      }
    } catch (error) {
      console.error('Session validation failed:', error);
      clearAuthState();
      return false;
    }
  };

  const contextValue: AuthContextType = {
    user,
    isAuthenticated: !!user,
    loading,
    login,
    register,
    logout,
    validateSession,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};