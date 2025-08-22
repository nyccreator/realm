// Neo4j Database Initialization - Section 3.1 Authentication Schema
// This script sets up the core constraints and indexes for the Realm PKM system

// === USER CONSTRAINTS AND INDEXES ===

// Ensure unique users by email (authentication requirement)
CREATE CONSTRAINT user_email_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.email IS UNIQUE;

// Ensure user ID uniqueness
CREATE CONSTRAINT user_id_unique IF NOT EXISTS  
FOR (u:User) REQUIRE u.id IS UNIQUE;

// Performance indexes for user queries
CREATE INDEX user_email_index IF NOT EXISTS
FOR (u:User) ON (u.email);

CREATE INDEX user_display_name_index IF NOT EXISTS
FOR (u:User) ON (u.displayName);

CREATE INDEX user_created_at_index IF NOT EXISTS
FOR (u:User) ON (u.createdAt);

CREATE INDEX user_active_index IF NOT EXISTS
FOR (u:User) ON (u.isActive);

// === NOTE CONSTRAINTS AND INDEXES (for future sections) ===

// Ensure note ID uniqueness  
CREATE CONSTRAINT note_id_unique IF NOT EXISTS
FOR (n:Note) REQUIRE n.id IS UNIQUE;

// Performance indexes for note queries
CREATE INDEX note_title_index IF NOT EXISTS
FOR (n:Note) ON (n.title);

CREATE INDEX note_created_at_index IF NOT EXISTS
FOR (n:Note) ON (n.createdAt);

CREATE INDEX note_updated_at_index IF NOT EXISTS
FOR (n:Note) ON (n.updatedAt);

// === VERIFICATION QUERIES ===

// Verify constraints were created
SHOW CONSTRAINTS;

// Verify indexes were created  
SHOW INDEXES;

// Log completion
RETURN "Neo4j schema initialization completed successfully" AS status;