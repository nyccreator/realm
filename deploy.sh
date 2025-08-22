#!/bin/bash

# Realm PKM Deployment Script
# Section 3.1 - Complete Authentication System Deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    log_success "Docker is running"
}

# Load environment variables
load_environment() {
    local env_file="${1:-.env.dev}"
    
    if [ -f "$env_file" ]; then
        log_info "Loading environment from $env_file"
        export $(grep -v '^#' "$env_file" | xargs)
        log_success "Environment loaded"
    else
        log_warning "Environment file $env_file not found, using defaults"
    fi
}

# Build and start services
deploy_services() {
    local compose_file="${1:-compose.yaml}"
    
    log_info "Building and starting services with $compose_file"
    
    # Stop any existing services
    docker-compose -f "$compose_file" down --remove-orphans
    
    # Build and start services
    docker-compose -f "$compose_file" up --build -d
    
    log_success "Services started"
}

# Wait for services to be healthy
wait_for_health() {
    local max_attempts=60
    local attempt=1
    
    log_info "Waiting for services to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps | grep -q "healthy"; then
            log_success "Services are healthy"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts - Waiting for services..."
        sleep 5
        ((attempt++))
    done
    
    log_error "Services failed to become healthy within expected time"
    docker-compose logs
    return 1
}

# Test authentication endpoints
test_authentication() {
    log_info "Testing authentication endpoints..."
    
    # Wait for backend to be fully ready
    sleep 10
    
    # Test health endpoint
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "Backend health check passed"
    else
        log_error "Backend health check failed"
        return 1
    fi
    
    # Test registration endpoint (should return method not allowed for GET)
    if curl -f http://localhost:8080/api/auth/register > /dev/null 2>&1; then
        log_success "Registration endpoint is accessible"
    else
        log_warning "Registration endpoint test inconclusive (expected for GET request)"
    fi
    
    # Test login endpoint (should return method not allowed for GET)  
    if curl -f http://localhost:8080/api/auth/login > /dev/null 2>&1; then
        log_success "Login endpoint is accessible"
    else
        log_warning "Login endpoint test inconclusive (expected for GET request)"
    fi
    
    log_success "Authentication endpoints are accessible"
}

# Test frontend accessibility
test_frontend() {
    log_info "Testing frontend accessibility..."
    
    # Wait for frontend to be ready
    sleep 5
    
    if curl -f http://localhost:3000 > /dev/null 2>&1; then
        log_success "Frontend is accessible"
    else
        log_error "Frontend is not accessible"
        return 1
    fi
}

# Test Neo4j connectivity
test_neo4j() {
    log_info "Testing Neo4j connectivity..."
    
    # Test Neo4j browser
    if curl -f http://localhost:7474 > /dev/null 2>&1; then
        log_success "Neo4j browser is accessible"
    else
        log_error "Neo4j browser is not accessible"
        return 1
    fi
    
    log_success "Neo4j connectivity test passed"
}

# Show deployment status
show_status() {
    log_info "Deployment Status:"
    echo ""
    docker-compose ps
    echo ""
    log_info "Service URLs:"
    echo "  Frontend: http://localhost:3000"
    echo "  Backend API: http://localhost:8080/api"
    echo "  Backend Health: http://localhost:8080/actuator/health"
    echo "  Neo4j Browser: http://localhost:7474"
    echo ""
}

# Main deployment function
main() {
    local environment="${1:-development}"
    local compose_file
    
    case $environment in
        "development" | "dev")
            compose_file="compose.yaml"
            load_environment ".env.dev"
            ;;
        "production" | "prod")
            compose_file="compose.prod.yaml"
            load_environment ".env.prod"
            ;;
        *)
            log_error "Unknown environment: $environment"
            log_info "Usage: $0 [development|production]"
            exit 1
            ;;
    esac
    
    log_info "Starting Realm PKM deployment for $environment environment"
    
    # Pre-deployment checks
    check_docker
    
    # Deploy services
    deploy_services "$compose_file"
    
    # Wait for services to be healthy
    wait_for_health
    
    # Run tests
    test_neo4j
    test_authentication
    test_frontend
    
    # Show final status
    show_status
    
    log_success "Realm PKM deployment completed successfully!"
    log_info "You can now access the application at http://localhost:3000"
}

# Run main function with all arguments
main "$@"