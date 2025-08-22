// Authentication service for Section 3.1 - Single-User Authentication
// Handles all API calls to the backend authentication endpoints

import { 
  User, 
  RegisterRequest, 
  LoginRequest, 
  AuthResponse, 
  ValidationResponse, 
  RefreshTokenRequest
} from '../types/auth';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const AUTH_BASE_URL = `${API_BASE_URL}/auth`;

class AuthService {
  private static instance: AuthService;
  
  public static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  /**
   * Register a new user
   */
  async register(registerData: RegisterRequest): Promise<AuthResponse> {
    const response = await fetch(`${AUTH_BASE_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerData),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Registration failed');
    }

    return data as AuthResponse;
  }

  /**
   * Authenticate user (login)
   */
  async login(loginData: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(`${AUTH_BASE_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(loginData),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Login failed');
    }

    return data as AuthResponse;
  }

  /**
   * Validate JWT token
   */
  async validateToken(token: string): Promise<ValidationResponse> {
    const response = await fetch(`${AUTH_BASE_URL}/validate`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    const data = await response.json();

    // Return the validation response regardless of status
    // The component will handle valid/invalid states based on the valid field
    return data as ValidationResponse;
  }

  /**
   * Refresh access token using refresh token
   */
  async refreshToken(refreshTokenData: RefreshTokenRequest): Promise<AuthResponse> {
    const response = await fetch(`${AUTH_BASE_URL}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(refreshTokenData),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Token refresh failed');
    }

    return data as AuthResponse;
  }

  /**
   * Store tokens in localStorage
   */
  storeTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem('realm_access_token', accessToken);
    localStorage.setItem('realm_refresh_token', refreshToken);
  }

  /**
   * Get stored access token
   */
  getStoredAccessToken(): string | null {
    return localStorage.getItem('realm_access_token');
  }

  /**
   * Get stored refresh token
   */
  getStoredRefreshToken(): string | null {
    return localStorage.getItem('realm_refresh_token');
  }

  /**
   * Clear all stored tokens
   */
  clearStoredTokens(): void {
    localStorage.removeItem('realm_access_token');
    localStorage.removeItem('realm_refresh_token');
  }

  /**
   * Store user data in localStorage
   */
  storeUser(user: User): void {
    localStorage.setItem('realm_user', JSON.stringify(user));
  }

  /**
   * Get stored user data
   */
  getStoredUser(): User | null {
    const userData = localStorage.getItem('realm_user');
    if (userData) {
      try {
        return JSON.parse(userData) as User;
      } catch (error) {
        console.error('Error parsing stored user data:', error);
        localStorage.removeItem('realm_user');
        return null;
      }
    }
    return null;
  }

  /**
   * Clear stored user data
   */
  clearStoredUser(): void {
    localStorage.removeItem('realm_user');
  }

  /**
   * Clear all stored authentication data
   */
  clearAllStoredData(): void {
    this.clearStoredTokens();
    this.clearStoredUser();
  }
}

export default AuthService;