#!/bin/bash
#===============================================================================
# status.sh - Status script for Microservice Architecture
#
# This script displays the current status of all microservices.
#
# Usage: ./status.sh [options]
#   Options:
#     -v, --verbose  Show detailed status including logs
#     -j, --json     Output status in JSON format
#     -h, --help     Show this help message
#===============================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default settings
VERBOSE=false
JSON_OUTPUT=false

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# PID files
PID_DIR="$PROJECT_ROOT/pids"
SHOP_PID_FILE="$PID_DIR/shop-management.pid"
PRODUCT_PID_FILE="$PID_DIR/product-stock.pid"

# Log directory
LOG_DIR="$PROJECT_ROOT/logs"

# Service ports
SHOP_PORT=8081
PRODUCT_PORT=8082

#-------------------------------------------------------------------------------
# Functions
#-------------------------------------------------------------------------------

print_banner() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║        Microservice Architecture - Status                      ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

show_help() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -v, --verbose  Show detailed status including resource usage"
    echo "  -j, --json     Output status in JSON format"
    echo "  -h, --help     Show this help message"
}

get_pid() {
    local pid_file=$1
    
    if [ -f "$pid_file" ]; then
        cat "$pid_file"
    else
        echo ""
    fi
}

is_process_running() {
    local pid=$1
    
    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        return 0  # Running
    fi
    return 1  # Not running
}

check_health() {
    local port=$1
    local timeout=5
    
    if curl -s --connect-timeout $timeout "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        local health_response=$(curl -s --connect-timeout $timeout "http://localhost:$port/actuator/health")
        local status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        echo "$status"
    else
        echo "UNREACHABLE"
    fi
}

get_memory_usage() {
    local pid=$1
    
    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        # Get RSS in KB, convert to MB
        local rss=$(ps -o rss= -p "$pid" 2>/dev/null | tr -d ' ')
        if [ -n "$rss" ]; then
            local mb=$((rss / 1024))
            echo "${mb}MB"
        else
            echo "N/A"
        fi
    else
        echo "N/A"
    fi
}

get_cpu_usage() {
    local pid=$1
    
    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        local cpu=$(ps -o %cpu= -p "$pid" 2>/dev/null | tr -d ' ')
        if [ -n "$cpu" ]; then
            echo "${cpu}%"
        else
            echo "N/A"
        fi
    else
        echo "N/A"
    fi
}

get_uptime() {
    local pid=$1
    
    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        local elapsed=$(ps -o etimes= -p "$pid" 2>/dev/null | tr -d ' ')
        if [ -n "$elapsed" ]; then
            local hours=$((elapsed / 3600))
            local minutes=$(((elapsed % 3600) / 60))
            local seconds=$((elapsed % 60))
            printf "%02dh %02dm %02ds" $hours $minutes $seconds
        else
            echo "N/A"
        fi
    else
        echo "N/A"
    fi
}

print_service_status() {
    local service_name=$1
    local pid_file=$2
    local port=$3
    
    local pid=$(get_pid "$pid_file")
    local running=false
    local status_color=$RED
    local status_text="STOPPED"
    
    if is_process_running "$pid"; then
        running=true
        local health=$(check_health "$port")
        
        case $health in
            UP)
                status_color=$GREEN
                status_text="RUNNING (UP)"
                ;;
            DOWN)
                status_color=$RED
                status_text="RUNNING (DOWN)"
                ;;
            UNREACHABLE)
                status_color=$YELLOW
                status_text="RUNNING (Starting...)"
                ;;
            *)
                status_color=$YELLOW
                status_text="RUNNING ($health)"
                ;;
        esac
    fi
    
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Service:${NC} $service_name"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${BLUE}Status:${NC}     ${status_color}$status_text${NC}"
    
    if [ "$running" = true ]; then
        echo -e "  ${BLUE}PID:${NC}        $pid"
        echo -e "  ${BLUE}Port:${NC}       $port"
        
        if [ "$VERBOSE" = true ]; then
            echo -e "  ${BLUE}Memory:${NC}     $(get_memory_usage $pid)"
            echo -e "  ${BLUE}CPU:${NC}        $(get_cpu_usage $pid)"
            echo -e "  ${BLUE}Uptime:${NC}     $(get_uptime $pid)"
        fi
        
        echo ""
        echo -e "  ${BLUE}Endpoints:${NC}"
        echo "    - REST API:  http://localhost:$port/api/v1"
        echo "    - Swagger:   http://localhost:$port/swagger-ui.html"
        echo "    - GraphQL:   http://localhost:$port/graphql"
        echo "    - Health:    http://localhost:$port/actuator/health"
        
        if [ "$service_name" = "Product-Stock" ]; then
            echo "    - SOAP WSDL: http://localhost:$port/ws/stockAvailability.wsdl"
        fi
    else
        echo ""
        echo -e "  ${YELLOW}Service is not running. Start with: ./start.sh${NC}"
    fi
}

print_json_status() {
    local shop_pid=$(get_pid "$SHOP_PID_FILE")
    local product_pid=$(get_pid "$PRODUCT_PID_FILE")
    
    local shop_running=false
    local product_running=false
    local shop_health="STOPPED"
    local product_health="STOPPED"
    
    if is_process_running "$shop_pid"; then
        shop_running=true
        shop_health=$(check_health "$SHOP_PORT")
    fi
    
    if is_process_running "$product_pid"; then
        product_running=true
        product_health=$(check_health "$PRODUCT_PORT")
    fi
    
    cat << EOF
{
  "timestamp": "$(date -Iseconds)",
  "services": {
    "shop-management": {
      "running": $shop_running,
      "pid": ${shop_pid:-null},
      "port": $SHOP_PORT,
      "health": "$shop_health",
      "memory": "$(get_memory_usage $shop_pid)",
      "cpu": "$(get_cpu_usage $shop_pid)"
    },
    "product-stock": {
      "running": $product_running,
      "pid": ${product_pid:-null},
      "port": $PRODUCT_PORT,
      "health": "$product_health",
      "memory": "$(get_memory_usage $product_pid)",
      "cpu": "$(get_cpu_usage $product_pid)"
    }
  }
}
EOF
}

print_summary() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Commands:"
    echo "  Start services:    ./start.sh"
    echo "  Stop services:     ./stop.sh"
    echo "  View logs:         tail -f $LOG_DIR/<service>.log"
    echo ""
}

#-------------------------------------------------------------------------------
# Main Script
#-------------------------------------------------------------------------------

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -j|--json)
            JSON_OUTPUT=true
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
if [ "$JSON_OUTPUT" = true ]; then
    print_json_status
else
    print_banner
    print_service_status "Shop-Management" "$SHOP_PID_FILE" "$SHOP_PORT"
    print_service_status "Product-Stock" "$PRODUCT_PID_FILE" "$PRODUCT_PORT"
    print_summary
fi
