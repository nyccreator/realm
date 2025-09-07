/**
 * Design System Tokens - Comprehensive design foundation
 * 
 * Provides consistent design tokens for:
 * - Colors with semantic meaning and accessibility
 * - Typography scale with fluid responsive sizing
 * - Spacing system with golden ratio progression
 * - Animation timing and easing functions
 * - Breakpoints and layout grid
 * - Shadows and elevation system
 * - Border radius and layout tokens
 */

// Color System - Semantic and accessible color palette
export const colors = {
  // Primary brand colors
  primary: {
    50: '#f0f9ff',
    100: '#e0f2fe', 
    200: '#bae6fd',
    300: '#7dd3fc',
    400: '#38bdf8',
    500: '#0ea5e9', // Main brand color
    600: '#0284c7',
    700: '#0369a1',
    800: '#075985',
    900: '#0c4a6e',
    950: '#082f49'
  },
  
  // Neutral grays for UI elements  
  neutral: {
    0: '#ffffff',
    50: '#fafafa',
    100: '#f5f5f5',
    200: '#e5e5e5', 
    300: '#d4d4d4',
    400: '#a3a3a3',
    500: '#737373',
    600: '#525252',
    700: '#404040',
    800: '#262626',
    900: '#171717',
    950: '#0a0a0a'
  },
  
  // Semantic colors for states
  semantic: {
    success: {
      50: '#f0fdf4',
      100: '#dcfce7',
      500: '#22c55e',
      600: '#16a34a',
      900: '#14532d'
    },
    warning: {
      50: '#fffbeb',
      100: '#fef3c7', 
      500: '#f59e0b',
      600: '#d97706',
      900: '#78350f'
    },
    danger: {
      50: '#fef2f2',
      100: '#fee2e2',
      500: '#ef4444', 
      600: '#dc2626',
      900: '#7f1d1d'
    },
    info: {
      50: '#f0f9ff',
      100: '#e0f2fe',
      500: '#0ea5e9',
      600: '#0284c7', 
      900: '#0c4a6e'
    }
  },
  
  // Graph visualization colors
  graph: {
    node: {
      default: '#0ea5e9',
      selected: '#0369a1',
      hovered: '#38bdf8',
      cluster1: '#22c55e',
      cluster2: '#f59e0b',
      cluster3: '#ef4444',
      cluster4: '#8b5cf6',
      cluster5: '#f472b6'
    },
    edge: {
      default: '#94a3b8',
      strong: '#475569',
      weak: '#cbd5e1',
      selected: '#0ea5e9'
    }
  }
} as const;

// Typography System - Fluid responsive scale
export const typography = {
  // Font families
  fontFamily: {
    sans: ['Inter', 'system-ui', 'sans-serif'],
    mono: ['JetBrains Mono', 'Menlo', 'Monaco', 'monospace'],
    display: ['Inter Display', 'Inter', 'system-ui', 'sans-serif']
  },
  
  // Font sizes with fluid scaling
  fontSize: {
    xs: ['0.75rem', { lineHeight: '1rem' }],
    sm: ['0.875rem', { lineHeight: '1.25rem' }],
    base: ['1rem', { lineHeight: '1.5rem' }],
    lg: ['1.125rem', { lineHeight: '1.75rem' }],
    xl: ['1.25rem', { lineHeight: '1.75rem' }],
    '2xl': ['1.5rem', { lineHeight: '2rem' }],
    '3xl': ['1.875rem', { lineHeight: '2.25rem' }], 
    '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
    '5xl': ['3rem', { lineHeight: '1' }],
    '6xl': ['3.75rem', { lineHeight: '1' }],
    '7xl': ['4.5rem', { lineHeight: '1' }],
    '8xl': ['6rem', { lineHeight: '1' }],
    '9xl': ['8rem', { lineHeight: '1' }]
  },
  
  // Font weights
  fontWeight: {
    thin: 100,
    extralight: 200,
    light: 300,
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
    extrabold: 800,
    black: 900
  },
  
  // Letter spacing
  letterSpacing: {
    tighter: '-0.05em',
    tight: '-0.025em', 
    normal: '0em',
    wide: '0.025em',
    wider: '0.05em',
    widest: '0.1em'
  }
} as const;

// Spacing System - Golden ratio based progression
export const spacing = {
  px: '1px',
  0: '0px',
  0.5: '0.125rem', // 2px
  1: '0.25rem',    // 4px
  1.5: '0.375rem', // 6px
  2: '0.5rem',     // 8px
  2.5: '0.625rem', // 10px
  3: '0.75rem',    // 12px
  3.5: '0.875rem', // 14px
  4: '1rem',       // 16px
  5: '1.25rem',    // 20px
  6: '1.5rem',     // 24px
  7: '1.75rem',    // 28px
  8: '2rem',       // 32px
  9: '2.25rem',    // 36px
  10: '2.5rem',    // 40px
  11: '2.75rem',   // 44px
  12: '3rem',      // 48px
  14: '3.5rem',    // 56px
  16: '4rem',      // 64px
  20: '5rem',      // 80px
  24: '6rem',      // 96px
  28: '7rem',      // 112px
  32: '8rem',      // 128px
  36: '9rem',      // 144px
  40: '10rem',     // 160px
  44: '11rem',     // 176px
  48: '12rem',     // 192px
  52: '13rem',     // 208px
  56: '14rem',     // 224px
  60: '15rem',     // 240px
  64: '16rem',     // 256px
  72: '18rem',     // 288px
  80: '20rem',     // 320px
  96: '24rem'      // 384px
} as const;

// Animation System
export const animation = {
  // Duration tokens
  duration: {
    instant: '0ms',
    fast: '150ms',
    normal: '300ms',
    slow: '500ms',
    slower: '750ms',
    slowest: '1000ms'
  },
  
  // Easing functions
  easing: {
    linear: 'linear',
    easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
    easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
    easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
    
    // Custom easing for different interactions
    bounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
    smooth: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)',
    sharp: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)'
  },
  
  // Predefined animations
  keyframes: {
    fadeIn: {
      '0%': { opacity: 0 },
      '100%': { opacity: 1 }
    },
    slideInUp: {
      '0%': { transform: 'translateY(20px)', opacity: 0 },
      '100%': { transform: 'translateY(0)', opacity: 1 }
    },
    slideInDown: {
      '0%': { transform: 'translateY(-20px)', opacity: 0 },
      '100%': { transform: 'translateY(0)', opacity: 1 }
    },
    scaleIn: {
      '0%': { transform: 'scale(0.9)', opacity: 0 },
      '100%': { transform: 'scale(1)', opacity: 1 }
    },
    pulse: {
      '0%, 100%': { opacity: 1 },
      '50%': { opacity: 0.5 }
    }
  }
} as const;

// Elevation System - Material Design inspired shadows
export const elevation = {
  none: 'none',
  sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
  base: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
  md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
  lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
  '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.25)',
  
  // Interactive shadows for buttons and cards
  hover: '0 8px 25px -8px rgb(0 0 0 / 0.15)',
  active: '0 2px 8px -2px rgb(0 0 0 / 0.15)',
  
  // Colored shadows for primary actions
  primaryHover: '0 8px 25px -8px rgb(14 165 233 / 0.35)',
  successHover: '0 8px 25px -8px rgb(34 197 94 / 0.35)',
  dangerHover: '0 8px 25px -8px rgb(239 68 68 / 0.35)'
} as const;

// Border Radius System
export const borderRadius = {
  none: '0px',
  sm: '0.125rem',  // 2px
  base: '0.25rem',  // 4px
  md: '0.375rem',   // 6px
  lg: '0.5rem',     // 8px
  xl: '0.75rem',    // 12px
  '2xl': '1rem',    // 16px
  '3xl': '1.5rem',  // 24px
  full: '9999px'
} as const;

// Breakpoints for responsive design
export const breakpoints = {
  xs: '475px',
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px'
} as const;

// Z-index system for layering
export const zIndex = {
  hide: -1,
  auto: 'auto',
  base: 0,
  docked: 10,
  dropdown: 1000,
  sticky: 1100,
  banner: 1200,
  overlay: 1300,
  modal: 1400,
  popover: 1500,
  skipLink: 1600,
  toast: 1700,
  tooltip: 1800
} as const;

// Component-specific tokens
export const components = {
  // Button variants
  button: {
    height: {
      sm: '2rem',    // 32px
      md: '2.5rem',  // 40px
      lg: '3rem'     // 48px
    },
    padding: {
      sm: '0.5rem 0.75rem',   // 8px 12px
      md: '0.625rem 1rem',    // 10px 16px
      lg: '0.75rem 1.25rem'   // 12px 20px
    }
  },
  
  // Input variants
  input: {
    height: {
      sm: '2rem',    // 32px
      md: '2.5rem',  // 40px
      lg: '3rem'     // 48px
    },
    padding: '0.5rem 0.75rem'
  },
  
  // Card variants
  card: {
    padding: {
      sm: '1rem',     // 16px
      md: '1.5rem',   // 24px
      lg: '2rem'      // 32px
    },
    borderRadius: borderRadius.lg
  }
} as const;

// Export all tokens as a comprehensive design system
export const designTokens = {
  colors,
  typography,
  spacing,
  animation,
  elevation,
  borderRadius,
  breakpoints,
  zIndex,
  components
} as const;

export default designTokens;