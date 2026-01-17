#===============================================================================
# Terraform Outputs for Microservice Architecture
#===============================================================================

#-------------------------------------------------------------------------------
# VPC Outputs
#-------------------------------------------------------------------------------

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}

#-------------------------------------------------------------------------------
# EC2 Outputs
#-------------------------------------------------------------------------------

output "shop_management_instance_id" {
  description = "Instance ID of the Shop-Management service"
  value       = aws_instance.shop_management.id
}

output "shop_management_public_ip" {
  description = "Public IP of the Shop-Management service"
  value       = aws_instance.shop_management.public_ip
}

output "shop_management_private_ip" {
  description = "Private IP of the Shop-Management service"
  value       = aws_instance.shop_management.private_ip
}

output "product_stock_instance_id" {
  description = "Instance ID of the Product-Stock service"
  value       = aws_instance.product_stock.id
}

output "product_stock_public_ip" {
  description = "Public IP of the Product-Stock service"
  value       = aws_instance.product_stock.public_ip
}

output "product_stock_private_ip" {
  description = "Private IP of the Product-Stock service"
  value       = aws_instance.product_stock.private_ip
}

#-------------------------------------------------------------------------------
# RDS Outputs
#-------------------------------------------------------------------------------

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.main.port
}

output "rds_database_name" {
  description = "Name of the database"
  value       = aws_db_instance.main.db_name
}

#-------------------------------------------------------------------------------
# Load Balancer Outputs
#-------------------------------------------------------------------------------

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

#-------------------------------------------------------------------------------
# Service URLs
#-------------------------------------------------------------------------------

output "shop_management_url" {
  description = "URL for Shop-Management service via ALB"
  value       = "http://${aws_lb.main.dns_name}/api/v1/shops"
}

output "product_stock_url" {
  description = "URL for Product-Stock service via ALB"
  value       = "http://${aws_lb.main.dns_name}/api/v1/products"
}

output "shop_management_direct_url" {
  description = "Direct URL for Shop-Management service"
  value       = "http://${aws_instance.shop_management.public_ip}:8081"
}

output "product_stock_direct_url" {
  description = "Direct URL for Product-Stock service"
  value       = "http://${aws_instance.product_stock.public_ip}:8082"
}

#-------------------------------------------------------------------------------
# SSH Commands
#-------------------------------------------------------------------------------

output "ssh_shop_management" {
  description = "SSH command for Shop-Management instance"
  value       = "ssh -i ${var.key_pair_name}.pem ec2-user@${aws_instance.shop_management.public_ip}"
}

output "ssh_product_stock" {
  description = "SSH command for Product-Stock instance"
  value       = "ssh -i ${var.key_pair_name}.pem ec2-user@${aws_instance.product_stock.public_ip}"
}

#-------------------------------------------------------------------------------
# Connection String
#-------------------------------------------------------------------------------

output "database_connection_string" {
  description = "Database connection string (without password)"
  value       = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${aws_db_instance.main.db_name}?user=${var.db_username}"
  sensitive   = false
}
