import React, {useEffect, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {
    Calendar,
    ChevronDown,
    ChevronRight,
    Clock,
    FileText,
    Hash,
    Plus,
    Search,
    Star,
    Tag,
    TrendingUp,
} from 'lucide-react';
import {cn} from '../utils/cn';
import {Input} from './ui/Input';
import {Note} from '../types/note';

export interface SidebarProps {
  isOpen: boolean;
  onToggle: () => void;
  currentNote?: Note | null;
  notes: Note[];
  onNoteSelect: (note: Note) => void;
  onCreateNote?: () => void;
  className?: string;
}

interface SidebarSection {
  id: string;
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  isCollapsed: boolean;
  items: any[];
}

const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onToggle,
  currentNote,
  notes,
  onNoteSelect,
  onCreateNote,
  className,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredNotes, setFilteredNotes] = useState<Note[]>(notes);
  const [sections, setSections] = useState<SidebarSection[]>([
    {
      id: 'recent',
      title: 'Recent Notes',
      icon: Clock,
      isCollapsed: false,
      items: [],
    },
    {
      id: 'favorites',
      title: 'Favorites',
      icon: Star,
      isCollapsed: false,
      items: [],
    },
    {
      id: 'tags',
      title: 'Tags',
      icon: Hash,
      isCollapsed: false,
      items: [],
    },
  ]);

  // Filter notes based on search query
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredNotes(notes);
    } else {
      const filtered = notes.filter(note =>
        note.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        note.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
        note.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
      );
      setFilteredNotes(filtered);
    }
  }, [searchQuery, notes]);

  // Update sections when notes change
  useEffect(() => {
    const recentNotes = [...notes]
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      .slice(0, 5);

    // Extract all unique tags
    const allTags = Array.from(
      new Set(notes.flatMap(note => note.tags))
    ).map(tag => ({
      name: tag,
      count: notes.filter(note => note.tags.includes(tag)).length,
    }));

    setSections(prev => prev.map(section => {
      switch (section.id) {
        case 'recent':
          return { ...section, items: recentNotes };
        case 'tags':
          return { ...section, items: allTags };
        default:
          return section;
      }
    }));
  }, [notes]);

  const toggleSection = (sectionId: string) => {
    setSections(prev => prev.map(section =>
      section.id === sectionId
        ? { ...section, isCollapsed: !section.isCollapsed }
        : section
    ));
  };

  const handleNoteClick = (note: Note) => {
    onNoteSelect(note);
  };

  const handleTagClick = (tagName: string) => {
    setSearchQuery(`#${tagName}`);
  };

  const sidebarVariants = {
    open: {
      x: 0,
      transition: { type: 'spring' as const, stiffness: 300, damping: 30 }
    },
    closed: {
      x: -320,
      transition: { type: 'spring' as const, stiffness: 300, damping: 30 }
    }
  } as any;

  return (
    <motion.aside
      initial={isOpen ? 'open' : 'closed'}
      animate={isOpen ? 'open' : 'closed'}
      variants={sidebarVariants}
      className={cn(
        'fixed left-0 top-16 bottom-8 z-30 w-80 border-r backdrop-blur-xl transition-colors lg:relative lg:top-0 lg:bottom-0',
        'bg-white/95 border-gray-200/70',
        className
      )}
      style={{
        backgroundColor: 'var(--bg-overlay)',
        borderColor: 'var(--border-muted)',
      }}
    >
      <div className="h-full flex flex-col">
        {/* Sidebar Header */}
        <div className="p-6 pb-4 border-b border-gray-100/70">
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            {/* Quick Actions */}
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-900">
                Notes
              </h2>
              <button
                onClick={onCreateNote}
                className="w-9 h-9 p-0 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 text-white hover:from-indigo-600 hover:to-purple-700 shadow-sm flex items-center justify-center transition-all"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>

            {/* Quick Search */}
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search notes..."
              className="h-10 rounded-xl bg-gray-50/80 border-gray-200/70 focus:bg-white"
              leftIcon={<Search className="w-4 h-4" />}
            />
          </motion.div>
        </div>

        {/* Sidebar Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-1">
          {/* Quick Stats */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="grid grid-cols-2 gap-3 mb-6"
          >
            <div className="p-3 rounded-xl shadow-none border border-gray-100 text-center">
              <div className="text-2xl font-bold text-indigo-600">
                {notes.length}
              </div>
              <div className="text-xs text-gray-500">Notes</div>
            </div>
            <div className="p-3 rounded-xl shadow-none border border-gray-100 text-center">
              <div className="text-2xl font-bold text-purple-600">
                {Array.from(new Set(notes.flatMap(note => note.tags))).length}
              </div>
              <div className="text-xs text-gray-500">Tags</div>
            </div>
          </motion.div>

          {/* All Notes Section */}
          {searchQuery && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="mb-6"
            >
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-gray-900">
                  Search Results ({filteredNotes.length})
                </h3>
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="text-gray-400 hover:text-gray-600 px-2 py-1 text-xs hover:bg-gray-100 rounded transition-colors"
                  >
                    Clear
                  </button>
                )}
              </div>
              <div className="space-y-1">
                {filteredNotes.map((note) => (
                  <motion.button
                    key={note.id}
                    onClick={() => handleNoteClick(note)}
                    className={cn(
                      'w-full text-left p-3 rounded-xl transition-all duration-200 group',
                      currentNote?.id === note.id
                        ? 'bg-indigo-50 border border-indigo-200 text-indigo-900'
                        : 'hover:bg-gray-50 text-gray-700'
                    )}
                    whileHover={{ x: 2 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <div className="flex items-start space-x-3">
                      <FileText className={cn(
                        'w-4 h-4 mt-0.5 shrink-0',
                        currentNote?.id === note.id ? 'text-indigo-600' : 'text-gray-400'
                      )} />
                      <div className="flex-1 min-w-0">
                        <div className="font-medium truncate">
                          {note.title}
                        </div>
                        <div className="text-xs text-gray-500 mt-1 truncate">
                          {note.content.substring(0, 100)}...
                        </div>
                        <div className="flex items-center mt-2 text-xs text-gray-400">
                          <Calendar className="w-3 h-3 mr-1" />
                          {new Date(note.updatedAt).toLocaleDateString()}
                        </div>
                      </div>
                    </div>
                  </motion.button>
                ))}
                {filteredNotes.length === 0 && (
                  <div className="text-center py-8 text-gray-500">
                    <Search className="w-8 h-8 mx-auto mb-2 opacity-40" />
                    <div className="text-sm">No notes found</div>
                  </div>
                )}
              </div>
            </motion.div>
          )}

          {/* Dynamic Sections */}
          {!searchQuery && sections.map((section) => (
            <motion.div
              key={section.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 + sections.indexOf(section) * 0.1 }}
              className="mb-6"
            >
              <button
                onClick={() => toggleSection(section.id)}
                className="w-full flex items-center justify-between mb-3 p-2 rounded-lg hover:bg-gray-50 transition-colors group"
              >
                <div className="flex items-center space-x-2">
                  <section.icon className="w-4 h-4 text-gray-600" />
                  <h3 className="text-sm font-medium text-gray-900">
                    {section.title}
                  </h3>
                  <span className="text-xs text-gray-500">
                    ({section.items.length})
                  </span>
                </div>
                {section.isCollapsed ? (
                  <ChevronRight className="w-4 h-4 text-gray-400 group-hover:text-gray-600" />
                ) : (
                  <ChevronDown className="w-4 h-4 text-gray-400 group-hover:text-gray-600" />
                )}
              </button>

              <AnimatePresence>
                {!section.isCollapsed && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    className="overflow-hidden"
                  >
                    <div className="space-y-1 pl-2">
                      {section.id === 'recent' && section.items.map((note: Note) => (
                        <motion.button
                          key={note.id}
                          onClick={() => handleNoteClick(note)}
                          className={cn(
                            'w-full text-left p-2 rounded-lg transition-all duration-200',
                            currentNote?.id === note.id
                              ? 'bg-indigo-50 text-indigo-900 border border-indigo-200'
                              : 'hover:bg-gray-50 text-gray-700'
                          )}
                          whileHover={{ x: 2 }}
                          whileTap={{ scale: 0.98 }}
                        >
                          <div className="flex items-center space-x-2">
                            <FileText className="w-3 h-3 text-gray-400" />
                            <span className="text-sm font-medium truncate">
                              {note.title}
                            </span>
                          </div>
                        </motion.button>
                      ))}

                      {section.id === 'tags' && section.items.map((tag: any) => (
                        <motion.button
                          key={tag.name}
                          onClick={() => handleTagClick(tag.name)}
                          className="w-full text-left p-2 rounded-lg hover:bg-gray-50 transition-colors group"
                          whileHover={{ x: 2 }}
                          whileTap={{ scale: 0.98 }}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-2">
                              <Tag className="w-3 h-3 text-gray-400" />
                              <span className="text-sm text-gray-700">
                                {tag.name}
                              </span>
                            </div>
                            <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">
                              {tag.count}
                            </span>
                          </div>
                        </motion.button>
                      ))}

                      {section.items.length === 0 && (
                        <div className="text-center py-4 text-gray-400 text-sm">
                          No {section.title.toLowerCase()} yet
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>

        {/* Sidebar Footer */}
        <div className="p-4 border-t border-gray-100/70">
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="flex items-center justify-between text-xs text-gray-500"
          >
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-3 h-3" />
              <span>Knowledge Graph</span>
            </div>
            <span className="font-mono">
              {notes.length} nodes
            </span>
          </motion.div>
        </div>
      </div>

      {/* Mobile overlay close button */}
      {isOpen && (
        <button
          onClick={onToggle}
          className="lg:hidden absolute -right-12 top-4 w-10 h-10 bg-white/90 backdrop-blur-sm border border-gray-200 rounded-xl flex items-center justify-center text-gray-600 hover:text-gray-900 transition-colors"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      )}
    </motion.aside>
  );
};

export { Sidebar };