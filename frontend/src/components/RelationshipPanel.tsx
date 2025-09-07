// RelationshipPanel component - Live relationship discovery and backlink management
// Premium panel showing dynamic relationships, backlinks, and connection opportunities

import React, {useEffect, useMemo, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {
    ArrowLeft,
    ArrowRight,
    ChevronDown,
    ChevronRight,
    Clock,
    ExternalLink,
    FileText,
    Hash,
    Network,
    Plus,
    Sparkles,
    TrendingUp,
    Zap,
} from 'lucide-react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';
import {cn} from '../utils/cn';

export interface RelationshipPanelProps {
  note: Note | null;
  className?: string;
  onNoteSelect?: (note: Note) => void;
}

interface Relationship {
  id: string;
  type: 'outgoing' | 'incoming' | 'tag' | 'suggested';
  targetNote: Note;
  strength: number;
  reason?: string;
  createdAt?: Date;
}

interface RelationshipSection {
  id: string;
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  count: number;
  relationships: Relationship[];
  isCollapsed: boolean;
}

const RelationshipPanel: React.FC<RelationshipPanelProps> = ({
  note,
  className,
  onNoteSelect,
}) => {
  const { notes } = useNoteStore();
  const [sections, setSections] = useState<RelationshipSection[]>([]);
  const [selectedRelationship, setSelectedRelationship] = useState<Relationship | null>(null);

  // Calculate relationships when note or notes change
  const relationships = useMemo(() => {
    if (!note || !notes.length) return [];

    const allRelationships: Relationship[] = [];

    // Find outgoing links
    if (note.outgoingLinks) {
      note.outgoingLinks.forEach(link => {
        const targetNote = link.targetNote || notes.find(n => n.id === link.targetNoteId);
        if (targetNote) {
          allRelationships.push({
            id: `out-${link.id}`,
            type: 'outgoing',
            targetNote,
            strength: 1.0,
            reason: 'Directly linked',
          });
        }
      });
    }

    // Find incoming links (backlinks)
    notes.forEach(otherNote => {
      if (otherNote.id !== note.id && otherNote.outgoingLinks?.some(link => 
        (typeof link === 'object' ? link.targetNoteId : link) === note.id
      )) {
        allRelationships.push({
          id: `in-${otherNote.id}`,
          type: 'incoming',
          targetNote: otherNote,
          strength: 1.0,
          reason: 'Links to this note',
        });
      }
    });

    // Find tag-based relationships
    if (note.tags.length > 0) {
      notes.forEach(otherNote => {
        if (otherNote.id !== note.id) {
          const sharedTags = note.tags.filter(tag => otherNote.tags.includes(tag));
          if (sharedTags.length > 0) {
            const strength = sharedTags.length / Math.max(note.tags.length, otherNote.tags.length);
            allRelationships.push({
              id: `tag-${otherNote.id}`,
              type: 'tag',
              targetNote: otherNote,
              strength,
              reason: `Shared tags: ${sharedTags.join(', ')}`,
            });
          }
        }
      });
    }

    // Find content-based suggestions
    if (note.content) {
      const noteWords = note.content.toLowerCase().split(/\\W+/).filter(word => word.length > 3);
      notes.forEach(otherNote => {
        if (otherNote.id !== note.id && otherNote.content) {
          const otherWords = otherNote.content.toLowerCase().split(/\\W+/);
          const commonWords = noteWords.filter(word => otherWords.includes(word));
          if (commonWords.length > 2) {
            const strength = commonWords.length / Math.max(noteWords.length, otherWords.length);
            // Only suggest if strength is meaningful and no existing relationship
            if (strength > 0.1 && !allRelationships.find(r => r.targetNote.id === otherNote.id)) {
              allRelationships.push({
                id: `suggest-${otherNote.id}`,
                type: 'suggested',
                targetNote: otherNote,
                strength,
                reason: `Similar content (${Math.round(strength * 100)}% match)`,
              });
            }
          }
        }
      });
    }

    return allRelationships;
  }, [note, notes]);

  // Organize relationships into sections
  useEffect(() => {
    const outgoing = relationships.filter(r => r.type === 'outgoing');
    const incoming = relationships.filter(r => r.type === 'incoming');
    const tagBased = relationships.filter(r => r.type === 'tag');
    const suggested = relationships.filter(r => r.type === 'suggested')
      .sort((a, b) => b.strength - a.strength)
      .slice(0, 5); // Limit suggestions

    setSections([
      {
        id: 'outgoing',
        title: 'Outgoing Links',
        icon: ArrowRight,
        count: outgoing.length,
        relationships: outgoing,
        isCollapsed: false,
      },
      {
        id: 'incoming',
        title: 'Backlinks',
        icon: ArrowLeft,
        count: incoming.length,
        relationships: incoming,
        isCollapsed: false,
      },
      {
        id: 'tags',
        title: 'Tag Connections',
        icon: Hash,
        count: tagBased.length,
        relationships: tagBased.sort((a, b) => b.strength - a.strength),
        isCollapsed: tagBased.length === 0,
      },
      {
        id: 'suggested',
        title: 'Suggested Connections',
        icon: Sparkles,
        count: suggested.length,
        relationships: suggested,
        isCollapsed: suggested.length === 0,
      },
    ]);
  }, [relationships]);

  const toggleSection = (sectionId: string) => {
    setSections(prev => prev.map(section =>
      section.id === sectionId
        ? { ...section, isCollapsed: !section.isCollapsed }
        : section
    ));
  };

  const handleRelationshipClick = (relationship: Relationship) => {
    if (onNoteSelect) {
      onNoteSelect(relationship.targetNote);
    }
  };

  const handleCreateConnection = (targetNote: Note) => {
    // TODO: Implement connection creation
    console.log('Create connection to:', targetNote.title);
  };

  if (!note) {
    return (
      <motion.div
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        className={cn('h-full flex items-center justify-center', className)}
      >
        <div className="text-center max-w-sm">
          <div className="w-16 h-16 bg-gradient-to-br from-purple-100 to-indigo-100 rounded-2xl flex items-center justify-center mx-auto mb-6">
            <Network className="w-8 h-8 text-purple-600" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Relationships</h3>
          <p className="text-gray-500 text-sm leading-relaxed">
            Select a note to explore its connections and discover relationship patterns.
          </p>
        </div>
      </motion.div>
    );
  }

  const totalRelationships = relationships.length;

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className={cn(
        'h-full flex flex-col bg-gray-50/50 border-l border-gray-200',
        className
      )}
    >
      {/* Panel Header */}
      <div className="p-6 pb-4 bg-white border-b border-gray-100">
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between mb-4"
        >
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-xl flex items-center justify-center">
              <Network className="w-5 h-5 text-white" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Relationships</h2>
              <p className="text-sm text-gray-500">
                {totalRelationships} connection{totalRelationships !== 1 ? 's' : ''} found
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-1">
            <button
              className="w-8 h-8 p-0 rounded-lg hover:bg-gray-100 transition-colors flex items-center justify-center"
              title="Refresh relationships"
            >
              <TrendingUp className="w-4 h-4" />
            </button>
          </div>
        </motion.div>

        {/* Quick Stats */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-2 gap-3"
        >
          <div className="p-3 rounded-xl shadow-none border border-gray-100 text-center">
            <div className="text-xl font-bold text-purple-600">
              {sections.find(s => s.id === 'incoming')?.count || 0}
            </div>
            <div className="text-xs text-gray-500">Backlinks</div>
          </div>
          <div className="p-3 rounded-xl shadow-none border border-gray-100 text-center">
            <div className="text-xl font-bold text-indigo-600">
              {sections.find(s => s.id === 'outgoing')?.count || 0}
            </div>
            <div className="text-xs text-gray-500">Outgoing</div>
          </div>
        </motion.div>
      </div>

      {/* Relationship Sections */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {sections.map((section, index) => (
          <motion.div
            key={section.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + index * 0.05 }}
          >
            <div className="rounded-xl shadow-none border border-gray-100 overflow-hidden">
              <button
                onClick={() => toggleSection(section.id)}
                className="w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center space-x-3">
                  <section.icon className={cn(
                    'w-5 h-5',
                    section.id === 'outgoing' && 'text-indigo-600',
                    section.id === 'incoming' && 'text-purple-600',
                    section.id === 'tags' && 'text-emerald-600',
                    section.id === 'suggested' && 'text-orange-600'
                  )} />
                  <div className="text-left">
                    <h3 className="font-medium text-gray-900">{section.title}</h3>
                    <p className="text-sm text-gray-500">
                      {section.count} connection{section.count !== 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  {section.count > 0 && (
                    <span className="bg-gray-100 text-gray-600 px-2 py-1 rounded-full text-xs font-medium">
                      {section.count}
                    </span>
                  )}
                  {section.isCollapsed ? (
                    <ChevronRight className="w-4 h-4 text-gray-400" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-gray-400" />
                  )}
                </div>
              </button>

              <AnimatePresence>
                {!section.isCollapsed && section.relationships.length > 0 && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    className="overflow-hidden border-t border-gray-100"
                  >
                    <div className="p-4 pt-0 space-y-2">
                      {section.relationships.map((relationship) => (
                        <motion.button
                          key={relationship.id}
                          onClick={() => handleRelationshipClick(relationship)}
                          className="w-full p-3 rounded-lg bg-white hover:bg-gray-50 border border-gray-100 text-left transition-all duration-200 group"
                          whileHover={{ x: 2 }}
                          whileTap={{ scale: 0.98 }}
                        >
                          <div className="flex items-start justify-between">
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center space-x-2 mb-1">
                                <FileText className="w-4 h-4 text-gray-400 shrink-0" />
                                <span className="font-medium text-gray-900 truncate">
                                  {relationship.targetNote.title}
                                </span>
                                {relationship.type === 'suggested' && (
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleCreateConnection(relationship.targetNote);
                                    }}
                                    className="opacity-0 group-hover:opacity-100 transition-opacity hover:bg-gray-100 rounded p-1 flex items-center justify-center"
                                  >
                                    <Plus className="w-3 h-3" />
                                  </button>
                                )}
                              </div>
                              <div className="text-sm text-gray-500">
                                {relationship.reason}
                              </div>
                              {relationship.strength < 1 && (
                                <div className="mt-2">
                                  <div className="w-full bg-gray-200 rounded-full h-1">
                                    <div
                                      className={cn(
                                        'h-1 rounded-full',
                                        section.id === 'tags' && 'bg-emerald-500',
                                        section.id === 'suggested' && 'bg-orange-500',
                                        section.id === 'outgoing' && 'bg-indigo-500',
                                        section.id === 'incoming' && 'bg-purple-500'
                                      )}
                                      style={{ width: `${relationship.strength * 100}%` }}
                                    />
                                  </div>
                                </div>
                              )}
                            </div>
                            <ExternalLink className="w-4 h-4 text-gray-300 group-hover:text-gray-500 shrink-0" />
                          </div>
                        </motion.button>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {!section.isCollapsed && section.relationships.length === 0 && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="p-6 text-center text-gray-500 border-t border-gray-100"
                >
                  <div className="text-sm">No {section.title.toLowerCase()} found</div>
                </motion.div>
              )}
            </div>
          </motion.div>
        ))}

        {totalRelationships === 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="text-center py-12"
          >
            <div className="w-16 h-16 bg-gray-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Network className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="font-medium text-gray-900 mb-2">No connections yet</h3>
            <p className="text-sm text-gray-500 leading-relaxed">
              Start linking this note to others or adding tags to build relationships.
            </p>
          </motion.div>
        )}
      </div>

      {/* Panel Footer */}
      {totalRelationships > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="p-4 bg-white border-t border-gray-100"
        >
          <div className="flex items-center justify-between text-xs text-gray-500">
            <div className="flex items-center space-x-2">
              <Zap className="w-3 h-3 text-indigo-600" />
              <span>Live relationship discovery</span>
            </div>
            <div className="flex items-center space-x-1">
              <Clock className="w-3 h-3" />
              <span>Updated now</span>
            </div>
          </div>
        </motion.div>
      )}
    </motion.div>
  );
};

export { RelationshipPanel };