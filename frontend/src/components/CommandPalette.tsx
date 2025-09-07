import React, {useCallback, useEffect, useRef, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {Dialog} from '@headlessui/react';
import {ArrowRight, Command, FileText, Plus, Search, Settings, Share2,} from 'lucide-react';
import {cn} from '../utils/cn';

export interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate?: (path: string) => void;
  onCreateNote?: () => void;
  onSearch?: (query: string) => void;
}

interface Command {
  id: string;
  title: string;
  description?: string;
  icon: React.ComponentType<{ className?: string }>;
  action: () => void;
  category: 'navigation' | 'creation' | 'settings' | 'search';
  shortcut?: string;
}

const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  onClose,
  onNavigate,
  onCreateNote,
  onSearch,
}) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const commands: Command[] = [
    {
      id: 'create-note',
      title: 'Create New Note',
      description: 'Start writing a new note',
      icon: Plus,
      action: () => {
        onCreateNote?.();
        onClose();
      },
      category: 'creation',
      shortcut: 'Ctrl+N',
    },
    {
      id: 'view-notes',
      title: 'View Notes',
      description: 'Browse all your notes',
      icon: FileText,
      action: () => {
        onNavigate?.('notes');
        onClose();
      },
      category: 'navigation',
    },
    {
      id: 'view-graph',
      title: 'View Graph',
      description: 'Explore knowledge graph',
      icon: Share2,
      action: () => {
        onNavigate?.('graph');
        onClose();
      },
      category: 'navigation',
    },
    {
      id: 'search-notes',
      title: 'Search Notes',
      description: 'Find notes by content',
      icon: Search,
      action: () => {
        if (query) {
          onSearch?.(query);
        }
        onClose();
      },
      category: 'search',
      shortcut: 'Ctrl+F',
    },
    {
      id: 'settings',
      title: 'Settings',
      description: 'Configure your workspace',
      icon: Settings,
      action: () => {
        // TODO: Navigate to settings
        onClose();
      },
      category: 'settings',
    },
  ];

  // Filter commands based on query
  const filteredCommands = commands.filter(command =>
    command.title.toLowerCase().includes(query.toLowerCase()) ||
    command.description?.toLowerCase().includes(query.toLowerCase())
  );

  // Group commands by category
  const groupedCommands = filteredCommands.reduce((groups, command) => {
    const category = command.category;
    if (!groups[category]) {
      groups[category] = [];
    }
    groups[category].push(command);
    return groups;
  }, {} as Record<string, Command[]>);

  const categoryLabels = {
    navigation: 'Navigation',
    creation: 'Create',
    search: 'Search',
    settings: 'Settings',
  };

  // Handle keyboard navigation
  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      if (!isOpen) return;

      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          setSelectedIndex(prev => Math.min(prev + 1, filteredCommands.length - 1));
          break;
        case 'ArrowUp':
          event.preventDefault();
          setSelectedIndex(prev => Math.max(prev - 1, 0));
          break;
        case 'Enter':
          event.preventDefault();
          if (filteredCommands[selectedIndex]) {
            filteredCommands[selectedIndex].action();
          }
          break;
        case 'Escape':
          onClose();
          break;
      }
    },
    [isOpen, filteredCommands, selectedIndex, onClose]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Reset selection when query changes
  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  // Focus input when opened
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // Reset state when closed
  useEffect(() => {
    if (!isOpen) {
      setQuery('');
      setSelectedIndex(0);
    }
  }, [isOpen]);

  return (
    <AnimatePresence>
      {isOpen && (
        <Dialog
          as={motion.div}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          open={isOpen}
          onClose={onClose}
          className="fixed inset-0 z-[100] overflow-y-auto"
        >
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/20 backdrop-blur-sm"
          />

          {/* Command Palette */}
          <div className="fixed inset-0 flex items-start justify-center pt-[15vh] px-4">
            <Dialog.Panel
              as={motion.div}
              initial={{ opacity: 0, scale: 0.95, y: -20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: -20 }}
              transition={{ 
                type: 'spring', 
                stiffness: 400, 
                damping: 25 
              } as any}
              className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-gray-200/70 overflow-hidden"
            >
              {/* Search Input */}
              <div className="flex items-center px-6 py-4 border-b border-gray-100">
                <Search className="w-5 h-5 text-gray-400 mr-3" />
                <input
                  ref={inputRef}
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Type a command or search..."
                  className="flex-1 text-lg placeholder:text-gray-400 outline-none bg-transparent"
                />
                <div className="flex items-center space-x-2 text-xs text-gray-400">
                  <kbd className="px-2 py-1 bg-gray-100 rounded border">↑</kbd>
                  <kbd className="px-2 py-1 bg-gray-100 rounded border">↓</kbd>
                  <span>to navigate</span>
                </div>
              </div>

              {/* Commands List */}
              <div className="max-h-96 overflow-y-auto">
                {filteredCommands.length > 0 ? (
                  <div className="py-2">
                    {Object.entries(groupedCommands).map(([category, categoryCommands]) => (
                      <div key={category} className="mb-4 last:mb-0">
                        <div className="px-6 py-2">
                          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            {categoryLabels[category as keyof typeof categoryLabels]}
                          </h3>
                        </div>
                        <div className="space-y-1 px-2">
                          {categoryCommands.map((command, index) => {
                            const globalIndex = filteredCommands.indexOf(command);
                            return (
                              <motion.button
                                key={command.id}
                                onClick={command.action}
                                className={cn(
                                  'w-full flex items-center px-4 py-3 rounded-xl text-left transition-all duration-200',
                                  globalIndex === selectedIndex
                                    ? 'bg-indigo-50 text-indigo-900 border border-indigo-200'
                                    : 'hover:bg-gray-50 text-gray-700'
                                )}
                                whileHover={{ x: 2 }}
                                transition={{ type: 'spring', stiffness: 400, damping: 17 }}
                              >
                                <command.icon
                                  className={cn(
                                    'w-5 h-5 mr-3',
                                    globalIndex === selectedIndex
                                      ? 'text-indigo-600'
                                      : 'text-gray-400'
                                  )}
                                />
                                <div className="flex-1 min-w-0">
                                  <div className="font-medium">{command.title}</div>
                                  {command.description && (
                                    <div className="text-sm text-gray-500 mt-0.5">
                                      {command.description}
                                    </div>
                                  )}
                                </div>
                                <div className="flex items-center space-x-2">
                                  {command.shortcut && (
                                    <kbd className="hidden sm:inline-block px-2 py-1 text-xs bg-gray-100 border border-gray-200 rounded">
                                      {command.shortcut}
                                    </kbd>
                                  )}
                                  <ArrowRight
                                    className={cn(
                                      'w-4 h-4',
                                      globalIndex === selectedIndex
                                        ? 'text-indigo-400'
                                        : 'text-gray-300'
                                    )}
                                  />
                                </div>
                              </motion.button>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="py-12 text-center">
                    <Search className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                    <div className="text-gray-500 font-medium">No commands found</div>
                    <div className="text-sm text-gray-400 mt-1">
                      Try adjusting your search query
                    </div>
                  </div>
                )}
              </div>

              {/* Footer */}
              <div className="flex items-center justify-between px-6 py-3 border-t border-gray-100 bg-gray-50/50">
                <div className="flex items-center space-x-4 text-xs text-gray-500">
                  <div className="flex items-center space-x-1">
                    <Command className="w-3 h-3" />
                    <span>+</span>
                    <kbd className="px-1 py-0.5 bg-white border border-gray-200 rounded">K</kbd>
                    <span>to open</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <kbd className="px-1 py-0.5 bg-white border border-gray-200 rounded">ESC</kbd>
                    <span>to close</span>
                  </div>
                </div>
                <div className="text-xs text-gray-400">
                  Realm Command Palette
                </div>
              </div>
            </Dialog.Panel>
          </div>
        </Dialog>
      )}
    </AnimatePresence>
  );
};

export { CommandPalette };