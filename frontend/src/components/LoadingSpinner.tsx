// LoadingSpinner component - reusable loading indicators
// Provides various loading states and spinner types

import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  variant?: 'primary' | 'secondary' | 'white';
  className?: string;
  text?: string;
  inline?: boolean;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  variant = 'primary',
  className = '',
  text,
  inline = false
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8',
    xl: 'h-12 w-12'
  };

  const colorClasses = {
    primary: 'border-blue-600 border-t-transparent',
    secondary: 'border-gray-300 border-t-transparent',
    white: 'border-white border-t-transparent'
  };

  const textSizeClasses = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base',
    xl: 'text-lg'
  };

  const spinner = (
    <div
      className={`animate-spin rounded-full border-2 ${sizeClasses[size]} ${colorClasses[variant]} ${className}`}
      role="status"
      aria-label="Loading"
    >
      <span className="sr-only">Loading...</span>
    </div>
  );

  if (inline) {
    return (
      <div className="inline-flex items-center space-x-2">
        {spinner}
        {text && (
          <span className={`text-gray-600 ${textSizeClasses[size]}`}>
            {text}
          </span>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center space-y-3">
      {spinner}
      {text && (
        <p className={`text-gray-600 ${textSizeClasses[size]}`}>
          {text}
        </p>
      )}
    </div>
  );
};

// Page-level loading component
export const PageLoading: React.FC<{ message?: string }> = ({ 
  message = 'Loading...' 
}) => {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <LoadingSpinner size="xl" text={message} />
      </div>
    </div>
  );
};

// Content loading skeleton
export const ContentSkeleton: React.FC = () => {
  return (
    <div className="animate-pulse">
      <div className="space-y-4">
        {/* Header skeleton */}
        <div className="h-8 bg-gray-200 rounded w-3/4"></div>
        
        {/* Content skeleton */}
        <div className="space-y-2">
          <div className="h-4 bg-gray-200 rounded"></div>
          <div className="h-4 bg-gray-200 rounded w-5/6"></div>
          <div className="h-4 bg-gray-200 rounded w-4/6"></div>
        </div>
        
        {/* Action skeleton */}
        <div className="flex space-x-2 pt-4">
          <div className="h-8 bg-gray-200 rounded w-20"></div>
          <div className="h-8 bg-gray-200 rounded w-16"></div>
        </div>
      </div>
    </div>
  );
};

// List item loading skeleton
export const ListItemSkeleton: React.FC = () => {
  return (
    <div className="animate-pulse p-4 border-b border-gray-100">
      <div className="flex items-start space-x-3">
        <div className="h-4 w-4 bg-gray-200 rounded"></div>
        <div className="flex-1 space-y-2">
          <div className="h-4 bg-gray-200 rounded w-3/4"></div>
          <div className="h-3 bg-gray-200 rounded w-1/2"></div>
          <div className="flex space-x-1">
            <div className="h-5 bg-gray-200 rounded-full w-12"></div>
            <div className="h-5 bg-gray-200 rounded-full w-16"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Button loading state
export const ButtonLoading: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'white';
  text?: string;
}> = ({ size = 'sm', variant = 'white', text = 'Loading...' }) => {
  return (
    <div className="inline-flex items-center space-x-2">
      <LoadingSpinner size={size} variant={variant} />
      <span>{text}</span>
    </div>
  );
};

// Loading overlay
export const LoadingOverlay: React.FC<{
  message?: string;
  visible: boolean;
}> = ({ message = 'Loading...', visible }) => {
  if (!visible) return null;

  return (
    <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-50">
      <div className="text-center">
        <LoadingSpinner size="lg" text={message} />
      </div>
    </div>
  );
};