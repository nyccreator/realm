# Realm Frontend - Section 3.1 Single-User Authentication

This is the React TypeScript frontend for the Realm Personal Knowledge Management system, implementing Section 3.1 authentication functionality.

## Features Implemented

### Section 3.1 - Single-User Authentication
- **Complete authentication system** with login/register forms
- **JWT token management** with automatic refresh and persistence
- **Protected routing** with automatic redirects
- **TypeScript interfaces** for type-safe API integration
- **Form validation** with user-friendly error handling
- **Responsive design** with custom CSS utility classes
- **Authentication context** for global state management

### Components
- `AuthProvider` - Authentication context and state management
- `LoginForm` - User login with validation
- `RegisterForm` - User registration with validation
- `ProtectedRoute` - Route protection wrapper
- `PublicRoute` - Public route wrapper (redirects if authenticated)
- `AuthLayout` - Authentication page layout
- `Dashboard` - Protected dashboard showing user info

### Services
- `AuthService` - API integration for authentication endpoints
- Token persistence in localStorage
- Automatic token validation and refresh

## API Integration

The frontend integrates with the following backend endpoints:
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/validate` - Token validation
- `POST /api/auth/refresh` - Token refresh

## Technology Stack

- **React 18** with TypeScript
- **React Router** for routing
- **Custom CSS** utility classes (Tailwind-style)
- **localStorage** for token persistence

## Development

```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build

# Run tests
npm test
```

## Environment Configuration

Create a `.env` file with:
```
REACT_APP_API_URL=http://localhost:8080/api
```

## Success Criteria Met

✅ User can successfully register with email and password
✅ User can login and receive a valid JWT token  
✅ Token is properly stored and persists across browser sessions
✅ Protected routes redirect unauthenticated users to login
✅ Authentication state is properly managed in React frontend
✅ Clean, production-ready TypeScript code
✅ Form validation and error handling
✅ Responsive design with proper UI/UX

## Next Steps

Ready for Section 3.2 implementation: Rich Text Editing & Manual Linking.

---

## Create React App Scripts

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

### `npm start`
Runs the app in development mode at [http://localhost:3000](http://localhost:3000)

### `npm test`
Launches the test runner in interactive watch mode

### `npm run build`
Builds the app for production to the `build` folder

### `npm run eject`
**Note: this is a one-way operation. Once you `eject`, you can't go back!**
