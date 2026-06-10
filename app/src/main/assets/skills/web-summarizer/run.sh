#!/bin/sh
# Web Summarizer skill - fetches URL and strips HTML to plain text
set -e

if [ -z "$URL" ]; then
    echo "Error: URL environment variable not set"
    exit 1
fi

echo "Fetching: $URL"

# Use curl to fetch the page
CONTENT=$(curl -sL --max-time 15 \
    -H "User-Agent: Mozilla/5.0 (Android 15) AppleWebKit/537.36" \
    "$URL" 2>/dev/null)

if [ -z "$CONTENT" ]; then
    echo "Error: Failed to fetch URL or empty response"
    exit 1
fi

# Strip HTML tags using sed (basic)
CLEAN=$(echo "$CONTENT" | \
    sed 's/<script[^>]*>.*<\/script>//g' | \
    sed 's/<style[^>]*>.*<\/style>//g' | \
    sed 's/<[^>]*>//g' | \
    sed '/^[[:space:]]*$/d' | \
    head -100)

echo "=== Content from $URL ==="
echo "$CLEAN"
