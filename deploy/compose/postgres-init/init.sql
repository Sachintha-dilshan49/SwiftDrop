-- deploy/compose/postgres-init/init.sql
-- SwiftDrop - PostgreSQL Database Initialization Script
-- Creates dedicated databases and users for all microservices.
-- Flyway handles schema migrations and table creation in each service.
-- Database passwords here match the defaults in deploy/compose/.env.example.

-- Auth service (Role 2)
CREATE USER auth WITH PASSWORD 'auth_pass';
CREATE DATABASE auth_db OWNER auth;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth;

-- Pricing service (Role 1)
CREATE USER pricing WITH PASSWORD 'pricing_pass';
CREATE DATABASE pricing_db OWNER pricing;
GRANT ALL PRIVILEGES ON DATABASE pricing_db TO pricing;

-- Parcel service (Role 3)
CREATE USER parcel WITH PASSWORD 'parcel_pass';
CREATE DATABASE parcel_db OWNER parcel;
GRANT ALL PRIVILEGES ON DATABASE parcel_db TO parcel;

-- Dispatch service (Role 4)
CREATE USER dispatch WITH PASSWORD 'dispatch_pass';
CREATE DATABASE dispatch_db OWNER dispatch;
GRANT ALL PRIVILEGES ON DATABASE dispatch_db TO dispatch;

-- Merchant service (Role 5)
CREATE USER merchant WITH PASSWORD 'merchant_pass';
CREATE DATABASE merchant_db OWNER merchant;
GRANT ALL PRIVILEGES ON DATABASE merchant_db TO merchant;

-- Notification service (Role 6)
CREATE USER notif WITH PASSWORD 'notif_pass';
CREATE DATABASE notification_db OWNER notif;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO notif;

-- Fallback user matching notification-service application.yml default username
CREATE USER swiftdrop WITH PASSWORD 'swiftdrop';
GRANT ALL PRIVILEGES ON DATABASE notification_db TO swiftdrop;
