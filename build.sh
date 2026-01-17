#!/bin/bash
#===============================================================================
# build.sh - Build script for Microservice Architecture
#
# This script builds both Shop-Management and Product-Stock microservices
# using Maven.
#
# Usage: ./build.sh [options]
#   Options:
#     -c, --clean     Clean build (removes target directories)
#     -s, --skip-tests Skip running tests during build
#     -p, --parallel   Build services in parallel
#     -h, --help       Show this help message
#===============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default settings
CLEAN_BUILD=false
SKIP_TESTS=false
PARALLEL=false

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Service directories
SHOP_SERVICE_DIR="$PROJECT_ROOT/shop-management"
PRODUCT_SERVICE_DIR="$PROJECT_ROOT/product-stock"

# Log file
LOG_FILE="$PROJECT_ROOT/build.log"

#-------------------------------------------------------------------------------
# Functions
#-------------------------------------------------------------------------------

print_banner() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║        Microservice Architecture - Build Script                ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

log() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
    esac
    
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

show_help() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -c, --clean       Clean build (removes target directories)"
    echo "  -s, --skip-tests  Skip running tests during build"
    echo "  -p, --parallel    Build services in parallel"
    echo "  -h, --help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Standard build"
    echo "  $0 -c                 # Clean build"
    echo "  $0 -c -s              # Clean build, skip tests"
    echo "  $0 --parallel         # Build both services in parallel"
}

check_prerequisites() {
    log INFO "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log ERROR "Java is not installed. Please install Java 21 or later."
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        log ERROR "Java 21 or later is required. Found version: $JAVA_VERSION"
        exit 1
    fi
    log INFO "Java version: $JAVA_VERSION ✓"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log ERROR "Maven is not installed. Please install Maven 3.9 or later."
        exit 1
    fi
    
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | awk '{print $3}')
    log INFO "Maven version: $MVN_VERSION ✓"
}

build_service() {
    local service_name=$1
    local service_dir=$2
    
    log INFO "Building $service_name..."
    
    if [ ! -d "$service_dir" ]; then
        log ERROR "Service directory not found: $service_dir"
        return 1
    fi
    
    cd "$service_dir"
    
    # Build Maven command
    MVN_CMD="mvn"
    
    if [ "$CLEAN_BUILD" = true ]; then
        MVN_CMD="$MVN_CMD clean"
    fi
    
    MVN_CMD="$MVN_CMD package"
    
    if [ "$SKIP_TESTS" = true ]; then
        MVN_CMD="$MVN_CMD -DskipTests"
    fi
    
    # Execute build
    log INFO "Executing: $MVN_CMD"
    
    if $MVN_CMD >> "$LOG_FILE" 2>&1; then
        log INFO "$service_name build successful ✓"
        return 0
    else
        log ERROR "$service_name build failed ✗"
        log ERROR "Check $LOG_FILE for details"
        return 1
    fi
}

build_parallel() {
    log INFO "Building services in parallel..."
    
    # Start both builds in background
    (build_service "Shop-Management" "$SHOP_SERVICE_DIR") &
    pid1=$!
    
    (build_service "Product-Stock" "$PRODUCT_SERVICE_DIR") &
    pid2=$!
    
    # Wait for both to complete
    wait $pid1
    result1=$?
    
    wait $pid2
    result2=$?
    
    if [ $result1 -ne 0 ] || [ $result2 -ne 0 ]; then
        return 1
    fi
    
    return 0
}

build_sequential() {
    build_service "Shop-Management" "$SHOP_SERVICE_DIR" || return 1
    build_service "Product-Stock" "$PRODUCT_SERVICE_DIR" || return 1
    return 0
}

print_summary() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}Build Complete!${NC}"
    echo ""
    echo "Artifacts:"
    
    if [ -f "$SHOP_SERVICE_DIR/target/shop-management-1.0.0.jar" ]; then
        echo -e "  ${GREEN}✓${NC} shop-management/target/shop-management-1.0.0.jar"
    else
        echo -e "  ${RED}✗${NC} shop-management JAR not found"
    fi
    
    if [ -f "$PRODUCT_SERVICE_DIR/target/product-stock-1.0.0.jar" ]; then
        echo -e "  ${GREEN}✓${NC} product-stock/target/product-stock-1.0.0.jar"
    else
        echo -e "  ${RED}✗${NC} product-stock JAR not found"
    fi
    
    echo ""
    echo "To start services:"
    echo "  ./start.sh"
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
}

#-------------------------------------------------------------------------------
# Main Script
#-------------------------------------------------------------------------------

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -p|--parallel)
            PARALLEL=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main execution
print_banner

# Clear log file
> "$LOG_FILE"

check_prerequisites

START_TIME=$(date +%s)

if [ "$PARALLEL" = true ]; then
    build_parallel
else
    build_sequential
fi

BUILD_RESULT=$?

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

if [ $BUILD_RESULT -eq 0 ]; then
    log INFO "Total build time: ${DURATION}s"
    print_summary
    exit 0
else
    log ERROR "Build failed after ${DURATION}s"
    log ERROR "Check $LOG_FILE for details"
    exit 1
fi
