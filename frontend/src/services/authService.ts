// Authentication service for Section 3.1 - Single-User Authentication
// Updated for Redis session-based authentication (no JWT tokens)

import {AuthResponse, LoginRequest, RegisterRequest, SessionValidationResponse, User} from '../types/auth';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const AUTH_BASE_URL = `${API_BASE_URL}/auth`;

class AuthService {
  private static instance: AuthService;
  private csrfToken: string | null = null;
  
  public static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  /**
   * Get CSRF token from server
   */
  private async getCsrfToken(): Promise<string> {
    if (this.csrfToken) {
      return this.csrfToken;
    }

    const response = await fetch(`${AUTH_BASE_URL}/csrf`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error('Failed to get CSRF token');
    }

    const data = await response.json();
    this.csrfToken = data.token;
    return data.token; // Return the token directly to ensure it's a string
  }

  /**
   * Make authenticated request with CSRF token
   */
  private async authenticatedRequest(url: string, options: RequestInit): Promise<Response> {
    const csrfToken = await this.getCsrfToken();
    
    return fetch(url, {
      ...options,
      credentials: 'include',
      headers: {
        ...options.headers,
        'X-XSRF-TOKEN': csrfToken,
      },
    });
  }

  /**
   * Register a new user with session creation
   */
  async register(registerData: RegisterRequest): Promise<AuthResponse> {
    const response = await this.authenticatedRequest(`${AUTH_BASE_URL}/register`, {
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
   * Authenticate user (login) and create session
   */
  async login(loginData: LoginRequest): Promise<AuthResponse> {
    const response = await this.authenticatedRequest(`${AUTH_BASE_URL}/login`, {
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
   * Validate current session
   */
  async validateSession(): Promise<SessionValidationResponse> {
    const response = await fetch(`${AUTH_BASE_URL}/validate`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // Important: Include cookies for session validation
    });

    const data = await response.json();

    // Return the validation response regardless of status
    // The component will handle valid/invalid states based on the valid field
    return data as SessionValidationResponse;
  }

  /**
   * Logout and destroy session
   */
  async logout(): Promise<void> {
    try {
      await this.authenticatedRequest(`${AUTH_BASE_URL}/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      // Note: We don't throw on logout errors to ensure client-side cleanup happens
    } catch (error) {
      console.error('Logout request failed:', error);
      // Continue with client-side cleanup regardless of server response
    } finally {
      // Clear cached CSRF token on logout
      this.csrfToken = null;
    }
  }

  // Removed all JWT token storage methods - sessions are managed server-side with cookies

  /**
   * Store user data in localStorage (for UI persistence across page reloads)
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
   * Clear all stored authentication data (user data only - sessions handled by server)
   */
  clearAllStoredData(): void {
    this.clearStoredUser();
    this.csrfToken = null; // Clear cached CSRF token
  }
}

export default AuthService;