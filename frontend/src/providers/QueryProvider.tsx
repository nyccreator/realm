// TanStack Query Provider for Section 3.2 - Rich Text Editing & Manual Linking
// Provides React Query context for caching and synchronization

import React from 'react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';

// Create a client with optimized settings for note management
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Stale time for notes - consider data fresh for 5 minutes
      staleTime: 5 * 60 * 1000,
      // Cache time - keep inactive data for 10 minutes
      gcTime: 10 * 60 * 1000,
      // Retry failed requests 2 times
      retry: 2,
      // Retry delay increases exponentially
      retryDelay: attemptIndex => Math.min(1000 * 2 ** attemptIndex, 30000),
      // Refetch on window focus for real-time updates
      refetchOnWindowFocus: true,
      // Refetch when reconnecting
      refetchOnReconnect: true,
    },
    mutations: {
      // Retry mutations once on failure
      retry: 1,
      // Retry delay for mutations
      retryDelay: 1000,
    },
  },
});

interface QueryProviderProps {
  children: React.ReactNode;
}

export const QueryProvider: React.FC<QueryProviderProps> = ({ children }) => {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {/* Show React Query DevTools in development */}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  );
};