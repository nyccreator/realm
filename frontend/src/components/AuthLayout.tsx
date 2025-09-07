// Authentication Layout Component for Section 3.1 - Single-User Authentication
// Provides a unified layout for login and registration pages

import React, {useState} from 'react';
import {LoginForm} from './LoginForm';
import {RegisterForm} from './RegisterForm';

type AuthMode = 'login' | 'register';

interface AuthLayoutProps {
  initialMode?: AuthMode;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string, displayName: string) => Promise<void>;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ 
  initialMode = 'login',
  onLogin,
  onRegister
}) => {
  const [mode, setMode] = useState<AuthMode>(initialMode);

  const handleAuthSuccess = () => {
    // Navigation will be handled by the App component when user state changes
  };

  const switchToLogin = () => setMode('login');
  const switchToRegister = () => setMode('register');

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Logo/Brand Section */}
        <div className="text-center mb-8">
          <div className="mx-auto h-16 w-16 bg-blue-600 rounded-full flex items-center justify-center mb-4">
            <svg 
              className="h-8 w-8 text-white" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M13 10V3L4 14h7v7l9-11h-7z" 
              />
            </svg>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">
            Realm
          </h1>
          <p className="text-sm text-gray-600 mt-2">
            Your Personal Knowledge Graph
          </p>
        </div>

        {/* Authentication Forms */}
        <div className="bg-white shadow-lg rounded-lg overflow-hidden">
          {mode === 'login' ? (
            <div className="p-8">
              <LoginForm 
                onLogin={onLogin}
                onSuccess={handleAuthSuccess}
                onSwitchToRegister={switchToRegister}
              />
            </div>
          ) : (
            <div className="p-8">
              <RegisterForm 
                onRegister={onRegister}
                onSuccess={handleAuthSuccess}
                onSwitchToLogin={switchToLogin}
              />
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-8 text-center">
          <p className="text-xs text-gray-500">
            By signing in, you agree to our{' '}
            <button type="button" className="underline hover:text-gray-700">
              Terms of Service
            </button>{' '}
            and{' '}
            <button type="button" className="underline hover:text-gray-700">
              Privacy Policy
            </button>
          </p>
        </div>
      </div>

      {/* Background Decoration */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-32 w-80 h-80 bg-blue-200 rounded-full opacity-20"></div>
        <div className="absolute -bottom-40 -left-32 w-80 h-80 bg-indigo-200 rounded-full opacity-20"></div>
      </div>
    </div>
  );
};

// Simple Auth Page Component that uses AuthLayout with useAuth
export const AuthPage: React.FC = () => {
  const handleLogin = async (email: string, password: string) => {
    // This will be handled by the forms themselves using useAuth
    console.log('Login handled by form');
  };

  const handleRegister = async (email: string, password: string, displayName: string) => {
    // This will be handled by the forms themselves using useAuth
    console.log('Register handled by form');
  };

  return (
    <AuthLayout 
      onLogin={handleLogin}
      onRegister={handleRegister}
    />
  );
};