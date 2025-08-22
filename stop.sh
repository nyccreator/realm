#!/bin/bash

# Realm PKM Stop/Cleanup Script
# Section 3.1 - Clean shutdown of all services

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

# Stop services gracefully
stop_services() {
    local compose_file="${1:-compose.yaml}"
    
    log_info "Stopping services with $compose_file"
    
    if [ -f "$compose_file" ]; then
        docker-compose -f "$compose_file" down --remove-orphans
        log_success "Services stopped"
    else
        log_warning "Compose file $compose_file not found"
    fi
}

# Clean up containers, networks, and images
cleanup() {
    local clean_level="${1:-basic}"
    
    case $clean_level in
        "basic")
            log_info "Performing basic cleanup..."
            docker system prune -f
            ;;
        "full")
            log_info "Performing full cleanup (including volumes and images)..."
            log_warning "This will remove all data including Neo4j database!"
            read -p "Are you sure? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                docker-compose -f compose.yaml down -v --remove-orphans
                docker-compose -f compose.prod.yaml down -v --remove-orphans 2>/dev/null || true
                docker system prune -a -f --volumes
                log_success "Full cleanup completed"
            else
                log_info "Full cleanup cancelled"
            fi
            ;;
        *)
            log_error "Unknown cleanup level: $clean_level"
            log_info "Usage: $0 [environment] [basic|full]"
            exit 1
            ;;
    esac
}

# Show current Docker status
show_status() {
    log_info "Current Docker Status:"
    echo ""
    
    log_info "Running containers:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    
    log_info "Docker system info:"
    docker system df
    echo ""
}

# Main function
main() {
    local environment="${1:-development}"
    local cleanup_level="${2:-basic}"
    local compose_file
    
    case $environment in
        "development" | "dev")
            compose_file="compose.yaml"
            ;;
        "production" | "prod")
            compose_file="compose.prod.yaml"
            ;;
        *)
            log_error "Unknown environment: $environment"
            log_info "Usage: $0 [development|production] [basic|full]"
            exit 1
            ;;
    esac
    
    log_info "Stopping Realm PKM for $environment environment"
    
    # Stop services
    stop_services "$compose_file"
    
    # Cleanup if requested
    if [ "$cleanup_level" != "none" ]; then
        cleanup "$cleanup_level"
    fi
    
    # Show final status
    show_status
    
    log_success "Realm PKM stopped successfully!"
}

# Run main function with all arguments
main "$@"