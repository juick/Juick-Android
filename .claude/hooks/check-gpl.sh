#!/bin/bash
# PostToolUse hook: add/update GPLv3 header on save
INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.tool_response.filePath // .tool_input.file_path')
EXT="${FILE##*.}"
YEAR=$(date +%Y)

[[ "$EXT" =~ ^(kt|java|xml)$ ]] || exit 0
[ -f "$FILE" ] || exit 0

if [ "$EXT" = "xml" ]; then
  HEADER="<!--
  ~ Copyright (C) 2008-${YEAR}, Juick
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as
  ~ published by the Free Software Foundation, either version 3 of the
  ~ License, or (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->
"
  MARKER="Copyright (C)"
else
  HEADER="/*
 * Copyright (C) 2008-${YEAR}, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
"
  MARKER="GNU General Public License"
fi

if grep -q "$MARKER" "$FILE"; then
  # Update copyright year
  sed -i '' "s/Copyright (C) 2008-[0-9]\{4\}/Copyright (C) 2008-${YEAR}/" "$FILE"
  echo "{\"systemMessage\":\"Updated copyright year to ${YEAR} in ${FILE}\"}"
else
  # Add header to new file
  CONTENT=$(cat "$FILE")
  echo "${HEADER}${CONTENT}" > "$FILE"
  echo "{\"systemMessage\":\"Added GPLv3 header to ${FILE}\"}"
fi
