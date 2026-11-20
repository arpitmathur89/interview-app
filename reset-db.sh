#!/usr/bin/env bash
# Stop the app first. Deletes the file-based H2 DB; the schema is recreated from entities on next start.
cd "$(dirname "$0")" && rm -rf ./data && echo "Dev database deleted. Start the app to recreate it."
