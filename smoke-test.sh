#!/usr/bin/env bash
# Run while the app is up: ./smoke-test.sh   (exits non-zero on the first unexpected status)
BASE=${BASE:-http://localhost:8080}
NAME="smoke-$(date +%s)"
fail=0
check() { # expected method path [json]
  local expected=$1 method=$2 path=$3 data=$4
  local code
  if [ -n "$data" ]; then
    code=$(curl -s -o /tmp/smoke.out -w "%{http_code}" -X "$method" "$BASE$path" -H 'Content-Type: application/json' -d "$data")
  else
    code=$(curl -s -o /tmp/smoke.out -w "%{http_code}" -X "$method" "$BASE$path")
  fi
  if [ "$code" = "$expected" ]; then echo "PASS $code $method $path"; else echo "FAIL $code (want $expected) $method $path: $(cat /tmp/smoke.out)"; fail=1; fi
}

check 201 POST /api/items "{\"name\":\"$NAME\",\"description\":\"smoke\"}"
ID=$(sed -E 's/.*"id":([0-9]+).*/\1/' /tmp/smoke.out)
check 200 GET  "/api/items/$ID"
check 200 GET  "/api/items?status=PENDING"
check 409 POST /api/items "{\"name\":\"$NAME\"}"
check 422 POST /api/items '{"name":""}'
check 404 GET  /api/items/999999
check 400 GET  /api/items/abc
check 200 PATCH "/api/items/$ID/status" '{"status":"IN_REVIEW","reason":"smoke"}'
check 400 PATCH "/api/items/$ID/status" '{"status":"NOT_A_STATUS"}'
check 200 PATCH "/api/items/$ID/status" '{"status":"APPROVED"}'
check 409 PATCH "/api/items/$ID/status" '{"status":"REJECTED"}'
check 200 GET  "/api/items/$ID/events"
check 204 DELETE "/api/items/$ID"
check 404 GET  "/api/items/$ID"
exit $fail
