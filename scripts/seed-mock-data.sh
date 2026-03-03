#!/bin/bash
# Seed mock data into Firestore using Firebase CLI
# Usage: bash scripts/seed-mock-data.sh

PROJECT="bettermingle"
PRIMARY_USER="REId2X8lUGXQIHImzlfJnx2poYu2"
NOW_MS=$(date +%s)000
DAY_MS=86400000

# Helper: write a Firestore document via REST using firebase token
write_doc() {
  local collection="$1"
  local doc_id="$2"
  local json_data="$3"
  firebase firestore:delete "$collection/$doc_id" --project "$PROJECT" --force 2>/dev/null
  echo "$json_data" | firebase firestore:set "$collection/$doc_id" --project "$PROJECT" --merge 2>/dev/null
}

echo "Seeding via Node.js script with firebase token..."
node scripts/seed-with-token.js
