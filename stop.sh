#!/bin/bash
#===============================================================================
# stop.sh - Stop script for Microservice Architecture
#
# This script stops the Shop-Management and Product-Stock microservices.
#
# Usage: ./stop.sh [options] [service]
#   Options:
#     -f, --force    Force kill (SIGKILL instead of SIGTERM)
#     -h, --help     Show this help message
#   
#   Service (optional):
#     shop       Stop only Shop-Management service
#     product    Stop only Product-Stock service
#     (none)     Stop both services
#===============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default settings
FORCE_KILL=false

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# PID files
PID_DIR="$PROJECT_ROOT/pids"
SHOP_PID_FILE="$PID_DIR/shop-management.pid"
PRODUCT_PID_FILE="$PID_DIR/product-stock.pid"

#-------------------------------------------------------------------------------
# Functions
#-------------------------------------------------------------------------------

print_banner() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║        Microservice Architecture - Stop Script                 ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

log() {
    local level=$1
    local message=$2
    
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
}

show_help() {
    echo "Usage: $0 [options] [service]"
    echo ""
    echo "Options:"
    echo "  -f, --force    Force kill (SIGKILL instead of SIGTERM)"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Service (optional):"
    echo "  shop       Stop only Shop-Management service"
    echo "  product    Stop only Product-Stock service"
    echo "  (none)     Stop both services"
    echo ""
    echo "Examples:"
    echo "  $0                 # Stop both services gracefully"
    echo "  $0 shop            # Stop only Shop-Management"
    echo "  $0 -f              # Force kill all services"
}

is_service_running() {
    local pid_file=$1
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0  # Running
        fi
    fi
    return 1  # Not running
}

stop_service() {
    local service_name=$1
    local pid_file=$2
    
    log INFO "Stopping $service_name..."
    
    if [ ! -f "$pid_file" ]; then
        log WARN "$service_name PID file not found"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    
    if ! ps -p "$pid" > /dev/null 2>&1; then
        log WARN "$service_name is not running (stale PID file)"
        rm -f "$pid_file"
        return 0
    fi
    
    # Send signal
    if [ "$FORCE_KILL" = true ]; then
        log INFO "Force killing $service_name (PID: $pid)..."
        kill -9 "$pid" 2>/dev/null || true
    else
        log INFO "Sending SIGTERM to $service_name (PID: $pid)..."
        kill -15 "$pid" 2>/dev/null || true
        
        # Wait for graceful shutdown
        local max_wait=30
        local waited=0
        
        while ps -p "$pid" > /dev/null 2>&1 && [ $waited -lt $max_wait ]; do
            sleep 1
            waited=$((waited + 1))
            echo -n "."
        done
        echo ""
        
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            log WARN "$service_name did not stop gracefully, force killing..."
            kill -9 "$pid" 2>/dev/null || true
        fi
    fi
    
    # Clean up PID file
    rm -f "$pid_file"
    
    log INFO "$service_name stopped ✓"
    return 0
}

stop_shop_service() {
    stop_service "shop-management" "$SHOP_PID_FILE"
}

stop_product_service() {
    stop_service "product-stock" "$PRODUCT_PID_FILE"
}

print_status() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}Services Stopped!${NC}"
    echo ""
    
    local shop_status="Stopped"
    local product_status="Stopped"
    
    if is_service_running "$SHOP_PID_FILE"; then
        shop_status="${RED}Still Running${NC}"
    else
        shop_status="${GREEN}Stopped${NC}"
    fi
    
    if is_service_running "$PRODUCT_PID_FILE"; then
        product_status="${RED}Still Running${NC}"
    else
        product_status="${GREEN}Stopped${NC}"
    fi
    
    echo -e "  Shop-Management:  $shop_status"
    echo -e "  Product-Stock:    $product_status"
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
}

#-------------------------------------------------------------------------------
# Main Script
#-------------------------------------------------------------------------------

STOP_SHOP=false
STOP_PRODUCT=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE_KILL=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        shop)
            STOP_SHOP=true
            shift
            ;;
        product)
            STOP_PRODUCT=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# If no specific service selected, stop both
if [ "$STOP_SHOP" = false ] && [ "$STOP_PRODUCT" = false ]; then
    STOP_SHOP=true
    STOP_PRODUCT=true
fi

# Main execution
print_banner

if [ "$FORCE_KILL" = true ]; then
    log WARN "Force kill mode enabled"
fi
echo ""

# Stop services
if [ "$STOP_PRODUCT" = true ]; then
    stop_product_service
fi

if [ "$STOP_SHOP" = true ]; then
    stop_shop_service
fi

print_status
