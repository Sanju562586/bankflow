-- Initialize multiple databases for Bankflow microservices
CREATE DATABASE bankflow_accounts;
CREATE DATABASE bankflow_payments;

GRANT ALL PRIVILEGES ON DATABASE bankflow_accounts TO bankflow;
GRANT ALL PRIVILEGES ON DATABASE bankflow_payments TO bankflow;
