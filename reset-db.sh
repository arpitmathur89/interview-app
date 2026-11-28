#!/usr/bin/env bash
# Stop the app first. Deletes ALL dev data in the file-based H2 DB (~/.interview-app, see application.properties);
# the schema is recreated from entities on next start.
rm -rf "$HOME/.interview-app" && echo "Dev database deleted. Start the app to recreate it."
