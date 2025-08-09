#!/bin/bash
set -e

# Check if we should skip initialization
if [ "$SKIP_DB_INIT" = "true" ]; then
    echo "Skipping database initialization (SKIP_DB_INIT=true)"
    exit 0
fi

# Check if we should reset the database
if [ "$RESET_DB" = "true" ]; then
    echo "Resetting database..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        DROP SCHEMA IF EXISTS public CASCADE;
        CREATE SCHEMA public;
        GRANT ALL ON SCHEMA public TO postgres;
        GRANT ALL ON SCHEMA public TO public;
EOSQL
fi

# Run schema initialization
echo "Initializing database schema..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" < /docker-entrypoint-initdb.d/schema.sql