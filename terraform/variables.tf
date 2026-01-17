#===============================================================================
# Terraform Variables for Microservice Architecture
#===============================================================================

#-------------------------------------------------------------------------------
# General Settings
#-------------------------------------------------------------------------------

variable "project_name" {
  description = "Name of the project, used for resource naming"
  type        = string
  default     = "microservice-arch"
}

variable "environment" {
  description = "Environment name (development, staging, production)"
  type        = string
  default     = "development"
  
  validation {
    condition     = contains(["development", "staging", "production"], var.environment)
    error_message = "Environment must be development, staging, or production."
  }
}

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

#-------------------------------------------------------------------------------
# VPC Settings
#-------------------------------------------------------------------------------

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets"
  type        = bool
  default     = false
}

#-------------------------------------------------------------------------------
# EC2 Settings
#-------------------------------------------------------------------------------

variable "ec2_instance_type" {
  description = "EC2 instance type for microservices"
  type        = string
  default     = "t3.medium"
}

variable "key_pair_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
}

variable "ssh_allowed_cidrs" {
  description = "CIDR blocks allowed to SSH to EC2 instances"
  type        = list(string)
  default     = []
}

variable "java_opts" {
  description = "JVM options for the microservices"
  type        = string
  default     = "-Xms512m -Xmx1024m -XX:+UseG1GC"
}

#-------------------------------------------------------------------------------
# RDS Settings
#-------------------------------------------------------------------------------

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "db_engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "15.4"
}

variable "db_name" {
  description = "Name of the database"
  type        = string
  default     = "microservices"
}

variable "db_username" {
  description = "Master username for the database"
  type        = string
  default     = "dbadmin"
  sensitive   = true
}

variable "db_password" {
  description = "Master password for the database"
  type        = string
  sensitive   = true
}

variable "db_allocated_storage" {
  description = "Allocated storage for RDS instance (GB)"
  type        = number
  default     = 20
}

variable "db_max_allocated_storage" {
  description = "Maximum allocated storage for RDS autoscaling (GB)"
  type        = number
  default     = 100
}

variable "db_multi_az" {
  description = "Enable Multi-AZ deployment for RDS"
  type        = bool
  default     = false
}

variable "db_backup_retention_period" {
  description = "Backup retention period in days"
  type        = number
  default     = 7
}

#-------------------------------------------------------------------------------
# Logging Settings
#-------------------------------------------------------------------------------

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 30
}
#-------------------------------------------------------------------------------
# Git Repository Settings (for server-side build)
#-------------------------------------------------------------------------------

variable "git_repo_url" {
  description = "GitHub repository URL for cloning and building on EC2"
  type        = string
  default     = "https://github.com/Gagan577/Microservice-Architecture.git"
}

variable "git_branch" {
  description = "Git branch to checkout for deployment"
  type        = string
  default     = "main"
}