// Craft-inspired PKM App with Clean Authentication Flow
// Completely rebuilt for stability and beautiful design

import React, {useEffect, useState} from 'react';
import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import AuthService from './services/authService';
import {User} from './types/auth';
import {AuthLayout} from './components/AuthLayout';
import {CraftApp} from './components/CraftApp';
import './App.css';
import './styles/design-tokens.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const authService = AuthService.getInstance();

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const storedUser = authService.getStoredUser();
        
        if (storedUser) {
          // Validate session with backend
          try {
            const validation = await authService.validateSession();
            if (validation.valid && validation.user) {
              setUser(validation.user);
              authService.storeUser(validation.user);
            } else {
              console.log('Session validation failed, clearing auth state');
              authService.clearAllStoredData();
              setUser(null);
            }
          } catch (error) {
            console.log('Session validation error, clearing auth state');
            authService.clearAllStoredData();
            setUser(null);
          }
        } else {
          setUser(null);
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, [authService]);

  const handleLogin = async (email: string, password: string) => {
    try {
      const response = await authService.login({ email, password });
      authService.storeUser(response.user);
      setUser(response.user);
    } catch (error) {
      throw error;
    }
  };

  const handleRegister = async (email: string, password: string, displayName: string) => {
    try {
      const response = await authService.register({ email, password, displayName });
      authService.storeUser(response.user);
      setUser(response.user);
    } catch (error) {
      throw error;
    }
  };

  const handleLogout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      authService.clearAllStoredData();
      setUser(null);
    }
  };

  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading Realm...</p>
        </div>
      </div>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="App">
          <Routes>
            <Route 
              path="/auth" 
              element={
                user ? (
                  <Navigate to="/" replace />
                ) : (
                  <AuthLayout 
                    onLogin={handleLogin}
                    onRegister={handleRegister}
                  />
                )
              } 
            />
            
            <Route 
              path="/*" 
              element={
                user ? (
                  <CraftApp user={user} onLogout={handleLogout} />
                ) : (
                  <Navigate to="/auth" replace />
                )
              } 
            />
          </Routes>
        </div>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
