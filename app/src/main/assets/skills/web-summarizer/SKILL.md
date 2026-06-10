name: Web Summarizer
description: Fetch a URL and return a clean text summary of the page content
version: 1.0.0
entry_points: run.sh
author: Origami Built-ins

## Usage
Pass URL as environment variable:
  URL="https://example.com" sh run.sh

## Output
Returns cleaned page text, suitable for feeding back to the model as context.
