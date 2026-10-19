#!/bin/sh
set -eu

# Run schema.sql and data.sql only when creating the database for the first time.
# Re-running these scripts against an existing H2 file would fail on duplicate
# tables and rows.
if [ -f /data/fee-calculator.mv.db ]; then
    init_mode=never
else
    init_mode=always
fi

exec java -jar /app/app.jar "--spring.sql.init.mode=${init_mode}"
