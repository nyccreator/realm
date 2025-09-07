import React, {useEffect, useRef, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {
    ChevronDown,
    Command,
    FileText,
    LogOut,
    Monitor,
    Moon,
    Search,
    Settings,
    Share2,
    Sun,
    User,
    Zap,
} from 'lucide-react';
import {useAuth} from '../contexts/AuthContext';
import {useTheme} from '../contexts/ThemeContext';
import {Input} from './ui/Input';
import {cn} from '../utils/cn';

export interface HeaderProps {
  activeView: 'notes' | 'graph';
  onViewChange: (view: 'notes' | 'graph') => void;
  onSearchFocus?: () => void;
}

const Header: React.FC<HeaderProps> = ({
  activeView,
  onViewChange,
  onSearchFocus,
}) => {
  const { user, logout } = useAuth();
  const { theme, setTheme, resolvedTheme } = useTheme();
  const [searchValue, setSearchValue] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isThemeMenuOpen, setIsThemeMenuOpen] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const themeMenuRef = useRef<HTMLDivElement>(null);

  // Global search shortcut (Ctrl+K / Cmd+K)
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
        event.preventDefault();
        searchRef.current?.focus();
        onSearchFocus?.();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onSearchFocus]);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
      if (themeMenuRef.current && !themeMenuRef.current.contains(event.target as Node)) {
        setIsThemeMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    setIsUserMenuOpen(false);
  };

  const themeIcons = {
    light: Sun,
    dark: Moon,
    system: Monitor,
  };

  const ThemeIcon = themeIcons[theme];

  return (
    <motion.header
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-xl border-b border-gray-200/50 transition-colors"
      style={{
        backgroundColor: 'var(--bg-overlay)',
        borderColor: 'var(--border-muted)',
      }}
    >
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <motion.div
            className="flex items-center space-x-3"
            whileHover={{ scale: 1.02 }}
            transition={{ type: 'spring', stiffness: 400, damping: 17 }}
          >
            <div className="relative">
              <motion.div
                className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg"
                whileHover={{ rotate: 5, scale: 1.05 }}
                transition={{ type: 'spring', stiffness: 400, damping: 17 }}
              >
                <Zap className="w-5 h-5 text-white" />
              </motion.div>
              <motion.div
                className="absolute -top-1 -right-1 w-3 h-3 bg-emerald-400 rounded-full border-2 border-white"
                animate={{ scale: [1, 1.2, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
              />
            </div>
            <div className="hidden sm:block">
              <h1 className="text-xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent">
                Realm
              </h1>
              <p className="text-xs text-gray-500 leading-none">
                Graph-based PKM
              </p>
            </div>
          </motion.div>

          {/* Global Search Bar */}
          <div className="flex-1 max-w-xl mx-8">
            <div className="relative">
              <motion.div
                className={cn(
                  'relative transition-all duration-300 ease-out',
                  isSearchFocused
                    ? 'transform scale-105 shadow-lg shadow-indigo-500/10'
                    : 'shadow-sm'
                )}
                whileFocus={{ scale: 1.02 }}
              >
                <Input
                  ref={searchRef}
                  value={searchValue}
                  onChange={(e) => setSearchValue(e.target.value)}
                  onFocus={() => setIsSearchFocused(true)}
                  onBlur={() => setIsSearchFocused(false)}
                  placeholder={`Search notes, tags, connections... (${
                    navigator.platform.includes('Mac') ? '⌘' : 'Ctrl'
                  }+K)`}
                  className="pl-10 pr-20 h-11 rounded-xl border-gray-200/70 bg-gray-50/50 placeholder:text-gray-400 text-gray-700 focus:bg-white focus:border-indigo-300"
                  leftIcon={<Search className="w-4 h-4" />}
                />
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center space-x-1">
                  <kbd className="hidden sm:inline-flex items-center px-2 py-0.5 text-xs font-medium text-gray-400 bg-gray-100 border border-gray-200 rounded">
                    <Command className="w-3 h-3 mr-1" />
                    K
                  </kbd>
                </div>
              </motion.div>

              {/* Search Results Dropdown */}
              <AnimatePresence>
                {isSearchFocused && searchValue && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 8, scale: 0.96 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                    className="absolute top-full mt-2 left-0 right-0 bg-white rounded-2xl shadow-xl border border-gray-200/70 overflow-hidden z-50"
                  >
                    <div className="p-4">
                      <div className="text-sm text-gray-500 mb-3">
                        Search results for "{searchValue}"
                      </div>
                      <div className="space-y-2">
                        <div className="p-3 rounded-xl bg-gray-50 hover:bg-gray-100 cursor-pointer transition-colors">
                          <div className="font-medium text-gray-900">
                            No results found
                          </div>
                          <div className="text-sm text-gray-500 mt-1">
                            Try adjusting your search terms
                          </div>
                        </div>
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* Right Actions */}
          <div className="flex items-center space-x-2">
            {/* View Toggle */}
            <div className="flex items-center bg-gray-100 rounded-xl p-1">
              <motion.button
                onClick={() => onViewChange('notes')}
                className={cn(
                  'flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                  activeView === 'notes'
                    ? 'bg-white text-indigo-700 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                )}
                whileHover={{ scale: activeView !== 'notes' ? 1.05 : 1 }}
                whileTap={{ scale: 0.95 }}
              >
                <FileText className="w-4 h-4 mr-2" />
                <span className="hidden sm:inline">Notes</span>
              </motion.button>
              <motion.button
                onClick={() => onViewChange('graph')}
                className={cn(
                  'flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                  activeView === 'graph'
                    ? 'bg-white text-indigo-700 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                )}
                whileHover={{ scale: activeView !== 'graph' ? 1.05 : 1 }}
                whileTap={{ scale: 0.95 }}
              >
                <Share2 className="w-4 h-4 mr-2" />
                <span className="hidden sm:inline">Graph</span>
              </motion.button>
            </div>

            {/* Theme Toggle */}
            <div className="relative" ref={themeMenuRef}>
              <button
                onClick={() => setIsThemeMenuOpen(!isThemeMenuOpen)}
                className="w-10 h-10 p-0 rounded-xl flex items-center justify-center hover:bg-gray-100 transition-colors"
              >
                <ThemeIcon className="w-4 h-4" />
              </button>

              <AnimatePresence>
                {isThemeMenuOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 8, scale: 0.96 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                    className="absolute right-0 top-full mt-2 w-48 bg-white rounded-2xl shadow-xl border border-gray-200/70 py-2 z-50"
                  >
                    {[
                      { key: 'light', icon: Sun, label: 'Light' },
                      { key: 'dark', icon: Moon, label: 'Dark' },
                      { key: 'system', icon: Monitor, label: 'System' },
                    ].map(({ key, icon: Icon, label }) => (
                      <motion.button
                        key={key}
                        onClick={() => {
                          setTheme(key as any);
                          setIsThemeMenuOpen(false);
                        }}
                        className={cn(
                          'w-full flex items-center px-4 py-2 text-sm transition-colors',
                          theme === key
                            ? 'text-indigo-600 bg-indigo-50'
                            : 'text-gray-700 hover:bg-gray-50'
                        )}
                        whileHover={{ backgroundColor: theme === key ? undefined : 'rgb(249 250 251)' }}
                      >
                        <Icon className="w-4 h-4 mr-3" />
                        {label}
                        {theme === key && (
                          <motion.div
                            className="ml-auto w-2 h-2 bg-indigo-600 rounded-full"
                            layoutId="theme-indicator"
                          />
                        )}
                      </motion.button>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* User Menu */}
            <div className="relative" ref={userMenuRef}>
              <button
                onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                className="flex items-center space-x-2 h-10 px-3 rounded-xl hover:bg-gray-100 transition-colors"
              >
                <div className="w-7 h-7 bg-gradient-to-br from-indigo-400 to-purple-500 rounded-lg flex items-center justify-center">
                  <User className="w-4 h-4 text-white" />
                </div>
                <span className="hidden md:inline text-sm font-medium text-gray-700">
                  {user?.displayName}
                </span>
                <ChevronDown className="w-4 h-4 text-gray-400" />
              </button>

              <AnimatePresence>
                {isUserMenuOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 8, scale: 0.96 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                    className="absolute right-0 top-full mt-2 w-56 bg-white rounded-2xl shadow-xl border border-gray-200/70 py-2 z-50"
                  >
                    <div className="px-4 py-3 border-b border-gray-100">
                      <div className="font-medium text-gray-900">
                        {user?.displayName}
                      </div>
                      <div className="text-sm text-gray-500">
                        {user?.email}
                      </div>
                    </div>
                    
                    <div className="py-2">
                      <button className="w-full flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors">
                        <Settings className="w-4 h-4 mr-3" />
                        Settings
                      </button>
                      <button 
                        onClick={handleLogout}
                        className="w-full flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <LogOut className="w-4 h-4 mr-3" />
                        Sign Out
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>
        </div>
      </div>
    </motion.header>
  );
};

export { Header };