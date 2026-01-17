#!/bin/bash
#===============================================================================
# start.sh - Start script for Microservice Architecture
#
# This script starts both Shop-Management and Product-Stock microservices.
# Services are started as background processes with proper logging.
#
# Usage: ./start.sh [options] [service]
#   Options:
#     -p, --profile PROFILE  Spring profile (default: default)
#     -m, --memory SIZE      JVM heap size (default: 512m)
#     -d, --debug            Enable remote debugging
#     -h, --help             Show this help message
#   
#   Service (optional):
#     shop       Start only Shop-Management service
#     product    Start only Product-Stock service
#     (none)     Start both services
#===============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default settings
PROFILE="default"
MEMORY="512m"
DEBUG_MODE=false
DEBUG_PORT_SHOP=5005
DEBUG_PORT_PRODUCT=5006

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Service configuration
SHOP_SERVICE_JAR="$PROJECT_ROOT/shop-management/target/shop-management-1.0.0.jar"
PRODUCT_SERVICE_JAR="$PROJECT_ROOT/product-stock/target/product-stock-1.0.0.jar"
SHOP_PORT=8081
PRODUCT_PORT=8082

# PID files
PID_DIR="$PROJECT_ROOT/pids"
SHOP_PID_FILE="$PID_DIR/shop-management.pid"
PRODUCT_PID_FILE="$PID_DIR/product-stock.pid"

# Log directory
LOG_DIR="$PROJECT_ROOT/logs"

#-------------------------------------------------------------------------------
# Functions
#-------------------------------------------------------------------------------

print_banner() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║        Microservice Architecture - Start Script                ║"
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
}

show_help() {
    echo "Usage: $0 [options] [service]"
    echo ""
    echo "Options:"
    echo "  -p, --profile PROFILE  Spring profile (default: default)"
    echo "  -m, --memory SIZE      JVM heap size (default: 512m)"
    echo "  -d, --debug            Enable remote debugging"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Service (optional):"
    echo "  shop       Start only Shop-Management service"
    echo "  product    Start only Product-Stock service"
    echo "  (none)     Start both services"
    echo ""
    echo "Examples:"
    echo "  $0                         # Start both services"
    echo "  $0 shop                    # Start only Shop-Management"
    echo "  $0 -p prod -m 1g           # Production profile with 1GB heap"
    echo "  $0 -d                      # Start with remote debugging"
}

check_prerequisites() {
    log INFO "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log ERROR "Java is not installed."
        exit 1
    fi
    
    # Create directories
    mkdir -p "$PID_DIR"
    mkdir -p "$LOG_DIR"
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

wait_for_service() {
    local service_name=$1
    local port=$2
    local max_wait=60
    local waited=0
    
    log INFO "Waiting for $service_name to be ready on port $port..."
    
    while [ $waited -lt $max_wait ]; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            log INFO "$service_name is ready! ✓"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
        echo -n "."
    done
    
    echo ""
    log WARN "$service_name did not become ready within ${max_wait}s"
    return 1
}

start_service() {
    local service_name=$1
    local jar_file=$2
    local port=$3
    local pid_file=$4
    local debug_port=$5
    
    log INFO "Starting $service_name..."
    
    # Check if JAR exists
    if [ ! -f "$jar_file" ]; then
        log ERROR "JAR file not found: $jar_file"
        log ERROR "Please run ./build.sh first"
        return 1
    fi
    
    # Check if already running
    if is_service_running "$pid_file"; then
        local existing_pid=$(cat "$pid_file")
        log WARN "$service_name is already running (PID: $existing_pid)"
        return 0
    fi
    
    # Build JVM options
    JVM_OPTS="-Xms$MEMORY -Xmx$MEMORY"
    JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
    JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=$LOG_DIR"
    JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=$PROFILE"
    JVM_OPTS="$JVM_OPTS -Dlogging.file.path=$LOG_DIR"
    
    # Add debug options if enabled
    if [ "$DEBUG_MODE" = true ]; then
        JVM_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$debug_port"
        log INFO "Remote debugging enabled on port $debug_port"
    fi
    
    # Start the service
    local log_file="$LOG_DIR/${service_name}.log"
    
    nohup java $JVM_OPTS -jar "$jar_file" > "$log_file" 2>&1 &
    local pid=$!
    
    echo $pid > "$pid_file"
    
    log INFO "$service_name started with PID: $pid"
    log INFO "Log file: $log_file"
    
    return 0
}

start_shop_service() {
    start_service "shop-management" "$SHOP_SERVICE_JAR" "$SHOP_PORT" "$SHOP_PID_FILE" "$DEBUG_PORT_SHOP"
}

start_product_service() {
    start_service "product-stock" "$PRODUCT_SERVICE_JAR" "$PRODUCT_PORT" "$PRODUCT_PID_FILE" "$DEBUG_PORT_PRODUCT"
}

print_status() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}Services Started!${NC}"
    echo ""
    echo "Service URLs:"
    echo "  Shop-Management:"
    echo "    - REST API:  http://localhost:$SHOP_PORT/api/v1"
    echo "    - Swagger:   http://localhost:$SHOP_PORT/swagger-ui.html"
    echo "    - GraphQL:   http://localhost:$SHOP_PORT/graphql"
    echo "    - Health:    http://localhost:$SHOP_PORT/actuator/health"
    echo ""
    echo "  Product-Stock:"
    echo "    - REST API:  http://localhost:$PRODUCT_PORT/api/v1"
    echo "    - Swagger:   http://localhost:$PRODUCT_PORT/swagger-ui.html"
    echo "    - GraphQL:   http://localhost:$PRODUCT_PORT/graphql"
    echo "    - SOAP WSDL: http://localhost:$PRODUCT_PORT/ws/stockAvailability.wsdl"
    echo "    - Health:    http://localhost:$PRODUCT_PORT/actuator/health"
    echo ""
    echo "Log files: $LOG_DIR"
    echo ""
    echo "To check status: ./status.sh"
    echo "To stop services: ./stop.sh"
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
}

#-------------------------------------------------------------------------------
# Main Script
#-------------------------------------------------------------------------------

START_SHOP=false
START_PRODUCT=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -m|--memory)
            MEMORY="$2"
            shift 2
            ;;
        -d|--debug)
            DEBUG_MODE=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        shop)
            START_SHOP=true
            shift
            ;;
        product)
            START_PRODUCT=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# If no specific service selected, start both
if [ "$START_SHOP" = false ] && [ "$START_PRODUCT" = false ]; then
    START_SHOP=true
    START_PRODUCT=true
fi

# Main execution
print_banner
check_prerequisites

log INFO "Configuration:"
log INFO "  Profile: $PROFILE"
log INFO "  Memory:  $MEMORY"
log INFO "  Debug:   $DEBUG_MODE"
echo ""

# Start services
if [ "$START_SHOP" = true ]; then
    start_shop_service
fi

if [ "$START_PRODUCT" = true ]; then
    start_product_service
fi

# Wait for services to be ready
echo ""
if [ "$START_SHOP" = true ]; then
    wait_for_service "Shop-Management" "$SHOP_PORT" &
fi

if [ "$START_PRODUCT" = true ]; then
    wait_for_service "Product-Stock" "$PRODUCT_PORT" &
fi

wait

print_status
