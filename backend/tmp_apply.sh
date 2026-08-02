#!/bin/bash
# Apply migrations sequentially to detect real failures
cd /migrations
for f in $(ls V*.sql | sort -t V -k2 -n); do
  echo "=== $f ==="
  PGPASSWORD=postgres psql -U postgres -d crm_test -v ON_ERROR_STOP=1 -f "/migrations/$f" > /tmp/out.log 2>&1
  if [ $? -eq 0 ]; then
    echo "OK"
  else
    echo "FAIL:"
    grep -iE 'ERROR|FATAL' /tmp/out.log | head -5
  fi
done
