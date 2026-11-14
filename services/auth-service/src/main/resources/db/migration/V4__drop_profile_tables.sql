-- ADR-011: auth_db holds identity only. Merchant details live in merchant_db and courier details in dispatch_db;
-- no code reads these tables.
DROP TABLE IF EXISTS merchant_profiles;
DROP TABLE IF EXISTS courier_profiles;
