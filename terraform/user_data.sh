#!/bin/bash
#===============================================================================
# EC2 User Data Script for Microservice Deployment
#
# This script is executed when an EC2 instance is first launched.
# It installs Java 21, downloads the service JAR, and starts the application.
#
# Template variables (provided by Terraform):
#   - service_name: Name of the service (shop-management or product-stock)
#   - service_port: Port the service listens on
#   - db_host: RDS endpoint
#   - db_name: Database name
#   - db_username: Database username
#   - db_password: Database password
#   - product_service_url: URL of product-stock service (for shop-management)
#   - java_opts: JVM options
#   - spring_profile: Spring profile to activate
#===============================================================================

set -e
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

echo "=========================================="
echo "Starting EC2 User Data Script"
echo "Service: ${service_name}"
echo "Timestamp: $(date)"
echo "=========================================="

#-------------------------------------------------------------------------------
# System Updates and Package Installation
#-------------------------------------------------------------------------------

echo "Updating system packages..."
dnf update -y

echo "Installing required packages..."
dnf install -y \
    java-21-amazon-corretto \
    amazon-cloudwatch-agent \
    jq \
    curl \
    wget

# Verify Java installation
java -version

#-------------------------------------------------------------------------------
# Create Service User and Directories
#-------------------------------------------------------------------------------

echo "Creating service user and directories..."

# Create service user
useradd -r -s /sbin/nologin appuser || true

# Create directories
mkdir -p /opt/${service_name}
mkdir -p /opt/${service_name}/logs
mkdir -p /opt/${service_name}/config
mkdir -p /var/log/${service_name}

#-------------------------------------------------------------------------------
# Configure Application Properties
#-------------------------------------------------------------------------------

echo "Creating application configuration..."

# Determine schema based on service
if [ "${service_name}" == "shop-management" ]; then
    DB_SCHEMA="shop_schema"
else
    DB_SCHEMA="product_schema"
fi

cat > /opt/${service_name}/config/application-${spring_profile}.properties << EOF
# Server Configuration
server.port=${service_port}
spring.application.name=${service_name}

# Database Configuration
spring.datasource.url=jdbc:postgresql://${db_host}/${db_name}?currentSchema=$${DB_SCHEMA}
spring.datasource.username=${db_username}
spring.datasource.password=${db_password}
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Configuration
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
spring.datasource.hikari.connection-timeout=30000

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.schemas=$${DB_SCHEMA}

# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Logging Configuration
logging.level.root=INFO
logging.level.com.enterprise=DEBUG
logging.file.path=/var/log/${service_name}
logging.file.name=/var/log/${service_name}/${service_name}.log

# Service URLs
%{ if product_service_url != "" ~}
product.service.url=${product_service_url}
%{ endif ~}
EOF

#-------------------------------------------------------------------------------
# Download Service JAR
#-------------------------------------------------------------------------------

echo "Downloading service JAR..."

# In production, you would download from S3 or another artifact repository
# For now, we'll create a placeholder for the JAR location
JAR_FILE="/opt/${service_name}/${service_name}.jar"

# Example: Download from S3
# aws s3 cp s3://your-artifact-bucket/${service_name}/${service_name}-1.0.0.jar $JAR_FILE

# For initial setup, create a placeholder script that waits for JAR
cat > /opt/${service_name}/wait-for-jar.sh << 'WAIT_SCRIPT'
#!/bin/bash
JAR_FILE="/opt/${service_name}/${service_name}.jar"
MAX_WAIT=300
WAITED=0

while [ ! -f "$JAR_FILE" ] && [ $WAITED -lt $MAX_WAIT ]; do
    echo "Waiting for JAR file: $JAR_FILE"
    sleep 10
    WAITED=$((WAITED + 10))
done

if [ -f "$JAR_FILE" ]; then
    echo "JAR file found!"
    exit 0
else
    echo "JAR file not found after ${MAX_WAIT}s"
    exit 1
fi
WAIT_SCRIPT
chmod +x /opt/${service_name}/wait-for-jar.sh

#-------------------------------------------------------------------------------
# Create Systemd Service
#-------------------------------------------------------------------------------

echo "Creating systemd service..."

cat > /etc/systemd/system/${service_name}.service << EOF
[Unit]
Description=${service_name} Microservice
After=network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/${service_name}

Environment="JAVA_OPTS=${java_opts}"
Environment="SPRING_PROFILES_ACTIVE=${spring_profile}"

ExecStart=/usr/bin/java \$JAVA_OPTS -jar /opt/${service_name}/${service_name}.jar --spring.config.location=file:/opt/${service_name}/config/

Restart=always
RestartSec=10

# Logging
StandardOutput=append:/var/log/${service_name}/${service_name}.log
StandardError=append:/var/log/${service_name}/${service_name}-error.log

# Resource Limits
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
EOF

#-------------------------------------------------------------------------------
# Configure CloudWatch Agent
#-------------------------------------------------------------------------------

echo "Configuring CloudWatch Agent..."

cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << EOF
{
    "agent": {
        "metrics_collection_interval": 60,
        "run_as_user": "root"
    },
    "logs": {
        "logs_collected": {
            "files": {
                "collect_list": [
                    {
                        "file_path": "/var/log/${service_name}/${service_name}.log",
                        "log_group_name": "/aws/ec2/microservice-arch/${service_name}",
                        "log_stream_name": "{instance_id}",
                        "timezone": "UTC"
                    },
                    {
                        "file_path": "/var/log/${service_name}/${service_name}-error.log",
                        "log_group_name": "/aws/ec2/microservice-arch/${service_name}-errors",
                        "log_stream_name": "{instance_id}",
                        "timezone": "UTC"
                    }
                ]
            }
        }
    },
    "metrics": {
        "namespace": "Microservices/${service_name}",
        "metrics_collected": {
            "cpu": {
                "measurement": ["cpu_usage_idle", "cpu_usage_user", "cpu_usage_system"],
                "metrics_collection_interval": 60
            },
            "disk": {
                "measurement": ["used_percent"],
                "metrics_collection_interval": 60,
                "resources": ["/"]
            },
            "mem": {
                "measurement": ["mem_used_percent"],
                "metrics_collection_interval": 60
            }
        }
    }
}
EOF

#-------------------------------------------------------------------------------
# Set Permissions
#-------------------------------------------------------------------------------

echo "Setting permissions..."
chown -R appuser:appuser /opt/${service_name}
chown -R appuser:appuser /var/log/${service_name}
chmod 750 /opt/${service_name}
chmod 640 /opt/${service_name}/config/*

#-------------------------------------------------------------------------------
# Enable and Start Services
#-------------------------------------------------------------------------------

echo "Enabling services..."

# Enable CloudWatch Agent
systemctl enable amazon-cloudwatch-agent
systemctl start amazon-cloudwatch-agent

# Enable application service (will start when JAR is available)
systemctl enable ${service_name}

# Reload systemd
systemctl daemon-reload

#-------------------------------------------------------------------------------
# Create Health Check Script
#-------------------------------------------------------------------------------

echo "Creating health check script..."

cat > /opt/${service_name}/health-check.sh << 'HEALTH_SCRIPT'
#!/bin/bash
PORT=${service_port}
MAX_RETRIES=30
RETRY_INTERVAL=10

for i in $(seq 1 $MAX_RETRIES); do
    if curl -s "http://localhost:$PORT/actuator/health" | grep -q '"status":"UP"'; then
        echo "Service is healthy!"
        exit 0
    fi
    echo "Waiting for service to be healthy... (attempt $i/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

echo "Service health check failed!"
exit 1
HEALTH_SCRIPT
chmod +x /opt/${service_name}/health-check.sh

#-------------------------------------------------------------------------------
# Final Status
#-------------------------------------------------------------------------------

echo "=========================================="
echo "EC2 User Data Script Completed"
echo "Timestamp: $(date)"
echo ""
echo "Next Steps:"
echo "1. Upload JAR to /opt/${service_name}/${service_name}.jar"
echo "2. Run: sudo systemctl start ${service_name}"
echo "3. Check status: sudo systemctl status ${service_name}"
echo "4. View logs: sudo tail -f /var/log/${service_name}/${service_name}.log"
echo "=========================================="
