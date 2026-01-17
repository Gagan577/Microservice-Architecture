#!/bin/bash
#===============================================================================
# EC2 User Data Script for Microservice Deployment
#
# This script is executed when an EC2 instance is first launched.
# It installs Java 21, Maven, clones the repository, builds the service,
# and starts the application.
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
#   - git_repo_url: GitHub repository URL
#   - git_branch: Git branch to checkout (default: main)
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
    java-21-amazon-corretto-devel \
    amazon-cloudwatch-agent \
    jq \
    curl \
    wget \
    git

# Install Maven
echo "Installing Maven..."
MAVEN_VERSION="3.9.6"
cd /tmp
wget https://archive.apache.org/dist/maven/maven-3/$${MAVEN_VERSION}/binaries/apache-maven-$${MAVEN_VERSION}-bin.tar.gz
tar -xzf apache-maven-$${MAVEN_VERSION}-bin.tar.gz
mv apache-maven-$${MAVEN_VERSION} /opt/maven
ln -s /opt/maven/bin/mvn /usr/bin/mvn

# Set JAVA_HOME
echo "export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto" >> /etc/profile.d/java.sh
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile.d/java.sh
source /etc/profile.d/java.sh

# Verify installations
java -version
mvn -version

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
mkdir -p /opt/${service_name}/source
mkdir -p /var/log/${service_name}

#-------------------------------------------------------------------------------
# Clone Repository and Build Application
#-------------------------------------------------------------------------------

echo "Cloning repository..."
cd /opt/${service_name}/source

GIT_REPO="${git_repo_url:-https://github.com/Gagan577/Microservice-Architecture.git}"
GIT_BRANCH="${git_branch:-main}"

git clone --branch $${GIT_BRANCH} --single-branch $${GIT_REPO} .

echo "Building ${service_name} service..."
cd /opt/${service_name}/source/${service_name}

# Build the service (skip tests for faster deployment)
mvn clean package -DskipTests -q

# Copy JAR to deployment directory
JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
cp $${JAR_FILE} /opt/${service_name}/${service_name}.jar

echo "Build completed successfully!"
ls -la /opt/${service_name}/${service_name}.jar

#-------------------------------------------------------------------------------
# Configure Application Properties (Override for Production)
#-------------------------------------------------------------------------------

echo "Creating production application configuration..."

# Determine schema based on service
if [ "${service_name}" == "shop-management" ]; then
    DB_SCHEMA="shop_schema"
else
    DB_SCHEMA="product_schema"
fi

cat > /opt/${service_name}/config/application-${spring_profile}.yml << EOF
# ============================================
# ${service_name} - Production Configuration
# ============================================

spring:
  application:
    name: ${service_name}

  datasource:
    url: jdbc:postgresql://${db_host}/${db_name}?currentSchema=$${DB_SCHEMA}
    username: ${db_username}
    password: ${db_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 30000

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true
    baseline-on-migrate: true
    schemas: $${DB_SCHEMA}

server:
  port: ${service_port}
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.enterprise: DEBUG
  file:
    path: /var/log/${service_name}
    name: /var/log/${service_name}/${service_name}.log

%{ if product_service_url != "" ~}
product:
  service:
    base-url: ${product_service_url}
%{ endif ~}
EOF

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

Environment="JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto"
Environment="JAVA_OPTS=${java_opts}"
Environment="SPRING_PROFILES_ACTIVE=${spring_profile}"

ExecStart=/usr/bin/java \$JAVA_OPTS -jar /opt/${service_name}/${service_name}.jar --spring.config.additional-location=file:/opt/${service_name}/config/

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
                    },
                    {
                        "file_path": "/var/log/user-data.log",
                        "log_group_name": "/aws/ec2/microservice-arch/${service_name}-deployment",
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
# Create Update/Redeploy Script
#-------------------------------------------------------------------------------

echo "Creating redeploy script..."

cat > /opt/${service_name}/redeploy.sh << 'REDEPLOY_SCRIPT'
#!/bin/bash
# Redeploy script - Pull latest code and rebuild

set -e
echo "Starting redeploy at $(date)"

SERVICE_NAME="${service_name}"
SOURCE_DIR="/opt/$${SERVICE_NAME}/source"
DEPLOY_DIR="/opt/$${SERVICE_NAME}"

# Stop the service
echo "Stopping $${SERVICE_NAME}..."
systemctl stop $${SERVICE_NAME} || true

# Pull latest code
echo "Pulling latest code..."
cd $${SOURCE_DIR}
git pull origin main

# Rebuild
echo "Building..."
cd $${SOURCE_DIR}/$${SERVICE_NAME}
mvn clean package -DskipTests -q

# Deploy new JAR
echo "Deploying..."
JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
cp $${JAR_FILE} $${DEPLOY_DIR}/$${SERVICE_NAME}.jar

# Start the service
echo "Starting $${SERVICE_NAME}..."
systemctl start $${SERVICE_NAME}

# Wait for health check
echo "Waiting for service to be healthy..."
sleep 30
curl -s "http://localhost:${service_port}/actuator/health" | jq .

echo "Redeploy completed at $(date)"
REDEPLOY_SCRIPT
chmod +x /opt/${service_name}/redeploy.sh

#-------------------------------------------------------------------------------
# Set Permissions
#-------------------------------------------------------------------------------

echo "Setting permissions..."
chown -R appuser:appuser /opt/${service_name}
chown -R appuser:appuser /var/log/${service_name}
chmod 750 /opt/${service_name}
chmod 640 /opt/${service_name}/config/* || true
chmod +x /opt/${service_name}/redeploy.sh

#-------------------------------------------------------------------------------
# Enable and Start Services
#-------------------------------------------------------------------------------

echo "Enabling services..."

# Enable CloudWatch Agent
systemctl enable amazon-cloudwatch-agent
systemctl start amazon-cloudwatch-agent

# Reload systemd
systemctl daemon-reload

# Enable and start application service
systemctl enable ${service_name}
systemctl start ${service_name}

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
# Wait for Service to Start
#-------------------------------------------------------------------------------

echo "Waiting for service to start..."
sleep 60

# Check service status
systemctl status ${service_name} --no-pager || true

# Run health check
/opt/${service_name}/health-check.sh || echo "Health check pending..."

#-------------------------------------------------------------------------------
# Final Status
#-------------------------------------------------------------------------------

echo "=========================================="
echo "EC2 User Data Script Completed"
echo "Timestamp: $(date)"
echo ""
echo "Service: ${service_name}"
echo "Port: ${service_port}"
echo "Status: $(systemctl is-active ${service_name})"
echo ""
echo "Useful Commands:"
echo "  - Check status: sudo systemctl status ${service_name}"
echo "  - View logs: sudo journalctl -u ${service_name} -f"
echo "  - App logs: sudo tail -f /var/log/${service_name}/${service_name}.log"
echo "  - Redeploy: sudo /opt/${service_name}/redeploy.sh"
echo "=========================================="
