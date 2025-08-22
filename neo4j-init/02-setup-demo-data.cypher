// Neo4j Demo Data Setup - Section 3.1 Authentication
// This script creates sample data for development and testing

// === CREATE DEMO USER ===
// Note: In production, users are created through the registration endpoint
// This is only for development/testing purposes

MERGE (u:User {
  email: "demo@realm.local"
})
SET 
  u.id = randomUUID(),
  u.displayName = "Demo User",
  u.passwordHash = "$2a$10$example.hash.for.demo.purposes.only", // Placeholder - real passwords set via API
  u.preferences = {},
  u.createdAt = datetime(),
  u.lastLoginAt = datetime(),
  u.isActive = true;

// === VERIFY DEMO DATA ===

// Count users
MATCH (u:User) 
RETURN count(u) AS total_users;

// Show demo user (without sensitive data)
MATCH (u:User {email: "demo@realm.local"})
RETURN 
  u.id,
  u.email,
  u.displayName,
  u.createdAt,
  u.isActive
AS demo_user;

// Log completion
RETURN "Demo data setup completed successfully" AS status;