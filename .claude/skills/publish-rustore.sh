#!/bin/bash
set -euo pipefail

KEY_ID="${RUSTORE_KEY_ID:?Set RUSTORE_KEY_ID}"
PRIVATE_KEYFILE="${RUSTORE_PRIVATE_KEYFILE:?Set RUSTORE_PRIVATE_KEYFILE}"
PACKAGE="${RUSTORE_PACKAGE:-com.juick}"
PUBLISH_TYPE="${RUSTORE_PUBLISH_TYPE:-INSTANTLY}"
WHATS_NEW_RU_FILE="${WHATS_NEW_RU_FILE:-/tmp/rustore-whatsnew-ru.txt}"
WHATS_NEW_RU=$(cat "${WHATS_NEW_RU_FILE:?Prepare $WHATS_NEW_RU_FILE with Russian release notes}")

API_BASE="https://public-api.rustore.ru/public/v1"
AUTH_URL="https://public-api.rustore.ru/public/auth/"

# 1. Build
echo "=== Building APK ==="
./gradlew clean assembleGoogleStore

APK_PATH=$(find build/outputs/apk/google/store -name "*.apk" | head -1)
echo "APK: $APK_PATH"

# 2. Auth: keyId + timestamp → SHA-512 → RSA sign → JWE token
echo "=== Getting access token ==="
KEYFILE=$(eval echo "${PRIVATE_KEYFILE}")
KEY_CONTENT=$(cat "${KEYFILE}")
# Add PEM headers if missing
if [[ ! "$KEY_CONTENT" =~ "BEGIN" ]]; then
    PEM_KEY="-----BEGIN PRIVATE KEY-----
${KEY_CONTENT}
-----END PRIVATE KEY-----"
else
    PEM_KEY="$KEY_CONTENT"
fi

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.000+00:00")
SIGN_MSG="${KEY_ID}${TIMESTAMP}"
SIGN_BASE64=$(echo -n "$SIGN_MSG" | openssl dgst -sha512 -sign <(echo "$PEM_KEY") | base64 -w 0)

AUTH_RESP=$(curl -sf -X POST "$AUTH_URL" \
  -H "Content-Type: application/json" \
  -d "{\"keyId\":\"${KEY_ID}\",\"timestamp\":\"${TIMESTAMP}\",\"signature\":\"${SIGN_BASE64}\"}")
TOKEN=$(jq -r .body.jwe <<< "$AUTH_RESP")
echo "Token obtained"

# 3. Create draft version
echo "=== Creating draft ==="
VERSION_BODY="{\"publishType\":\"${PUBLISH_TYPE}\",\"whatsNew\":\"${WHATS_NEW_RU}\"}"

DRAFT=$(curl -sf -X POST "${API_BASE}/application/${PACKAGE}/version" \
  -H "Public-Token: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "$VERSION_BODY")
VERSION_ID=$(jq -r .body <<< "$DRAFT")
echo "Version ID: $VERSION_ID"

# 4. Upload APK
echo "=== Uploading APK ==="
curl -sf -X POST "${API_BASE}/application/${PACKAGE}/version/${VERSION_ID}/apk?isMainApk=true&servicesType=Unknown" \
  -H "Public-Token: ${TOKEN}" \
  -F "file=@${APK_PATH}"
echo ""
echo "Upload done"

# 5. Submit for moderation
echo "=== Submitting for review ==="
SUBMIT=$(curl -sf -X POST "${API_BASE}/application/${PACKAGE}/version/${VERSION_ID}/commit" \
  -H "Public-Token: ${TOKEN}" \
  -H "Content-Type: application/json")
echo "$SUBMIT"

CODE=$(jq -r .code <<< "$SUBMIT" 2>/dev/null || echo "OK")
if [ "$CODE" = "OK" ]; then
  echo "=== Submitted successfully ==="
else
  echo "=== Submit result: $SUBMIT ==="
fi
