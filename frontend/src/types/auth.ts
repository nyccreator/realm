// Authentication types for Section 3.1 - Single-User Authentication
// Updated for Redis session-based authentication (no JWT tokens)

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
  user: User;
  sessionId?: string; // Optional session metadata
}

export interface SessionValidationResponse {
  valid: boolean;
  user?: User;
  sessionExpiry?: string;
  remainingTime?: number;
  error?: string;
  message?: string;
  status?: number;
}

// Removed RefreshTokenRequest - not needed for session-based auth

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
  validateSession: () => Promise<boolean>;
}