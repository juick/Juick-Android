#!/bin/bash
INPUT=$(cat)
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // ""')

# Block sed/python on project source files
if echo "$CMD" | grep -qE '\b(sed|python3?)\b.*\b(src|build\.gradle|\.kt|\.xml|\.java)\b'; then
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Use Edit tool instead."}}'
  exit 0
fi
