// Authentication types for Section 3.1 - Single-User Authentication
// These types match the backend API responses from AuthController

export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
  lastLoginAt?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  message: string;
  token: string; // For backward compatibility
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface ValidationResponse {
  valid: boolean;
  user?: User;
  tokenExpiry?: string;
  remainingTime?: number;
  error?: string;
  message?: string;
  status?: number;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<boolean>;
}