#!/bin/bash
set -euo pipefail

SERVICE_ACCOUNT_ID="${GALAXY_SERVICE_ACCOUNT_ID:?Set GALAXY_SERVICE_ACCOUNT_ID}"
PRIVATE_KEYFILE="${GALAXY_PRIVATE_KEYFILE:?Set GALAXY_PRIVATE_KEYFILE}"
CONTENT_ID="${GALAXY_CONTENT_ID:?Set GALAXY_CONTENT_ID}"
WHATS_NEW_FILE="${WHATS_NEW_FILE:-/tmp/galaxystore-whatsnew.txt}"
WHATS_NEW=$(cat "${WHATS_NEW_FILE:?Prepare $WHATS_NEW_FILE with release notes}")

API_BASE="https://devapi.samsungapps.com/seller"
UPLOAD_URL="https://seller.samsungapps.com/galaxyapi/fileUpload"

# 1. Build
echo "=== Building APK ==="
./gradlew clean assembleGoogleStore

APK_PATH=$(find build/outputs/apk/google/store -name "*.apk" | head -1)
echo "APK: $APK_PATH"

# 2. Get access token (JWT RS256)
echo "=== Getting access token ==="
KEYFILE=$(eval echo "${PRIVATE_KEYFILE}")
KEY_CONTENT=$(cat "${KEYFILE}")
if [[ ! "$KEY_CONTENT" =~ "BEGIN" ]]; then
    PEM_KEY="-----BEGIN PRIVATE KEY-----
${KEY_CONTENT}
-----END PRIVATE KEY-----"
else
    PEM_KEY="$KEY_CONTENT"
fi

NOW=$(date +%s)
EXP=$((NOW + 1200))
b64url() { base64 -w 0 | tr '+/' '-_' | tr -d '='; }
JWT_HEADER=$(echo -n '{"alg":"RS256","typ":"JWT"}' | b64url)
JWT_BODY=$(echo -n "{\"iss\":\"${SERVICE_ACCOUNT_ID}\",\"scopes\":[\"publishing\"],\"iat\":${NOW},\"exp\":${EXP}}" | b64url)
JWT_SIGNING="${JWT_HEADER}.${JWT_BODY}"
JWT_SIGNATURE=$(echo -n "${JWT_SIGNING}" | openssl dgst -sha256 -sign <(echo "$PEM_KEY") | b64url)
JWT="${JWT_SIGNING}.${JWT_SIGNATURE}"

TOKEN=$(curl -sf -X POST "https://devapi.samsungapps.com/auth/accessToken" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT}" \
  | jq -r .createdItem.accessToken)
echo "Token obtained"

# 3. Create upload session
echo "=== Creating upload session ==="
SESSION_ID=$(curl -sf -X POST "${API_BASE}/createUploadSessionId" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "service-account-id: ${SERVICE_ACCOUNT_ID}" \
  | jq -r .sessionId)
echo "Session: $SESSION_ID"

# 4. Upload APK
echo "=== Uploading APK ==="
FILE_KEY=$(curl -sf -X POST "${UPLOAD_URL}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "service-account-id: ${SERVICE_ACCOUNT_ID}" \
  -F "sessionId=${SESSION_ID}" \
  -F "file=@${APK_PATH}" \
  | jq -r .fileKey)
echo "File key: $FILE_KEY"

# 5. Create update with whatsNew
echo "=== Creating update ==="
curl -sf -X POST "${API_BASE}/contentUpdate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "service-account-id: ${SERVICE_ACCOUNT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"contentId\":\"${CONTENT_ID}\",\"whatsNew\":\"${WHATS_NEW}\"}"
echo ""
echo "Update created"

# 6. Register binary (JSON body, camelCase fields)
echo "=== Registering binary ==="
BINARY_RESP=$(curl -sf -X POST "${API_BASE}/v2/content/binary" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "service-account-id: ${SERVICE_ACCOUNT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"contentId\":\"${CONTENT_ID}\",\"gms\":\"Y\",\"filekey\":\"${FILE_KEY}\"}")
echo "$BINARY_RESP"
BINARY_SEQ=$(jq -r .data.binarySeq <<< "$BINARY_RESP")
echo "Binary seq: $BINARY_SEQ"

# 7. Submit for review (contentUpdate auto-submits for UPDATING status)
echo "=== Submitting for review ==="
SUBMIT=$(curl -sf -X POST "${API_BASE}/contentSubmit" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "service-account-id: ${SERVICE_ACCOUNT_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"contentId\":\"${CONTENT_ID}\"}" || true)
echo "${SUBMIT:-Already in review (contentUpdate auto-submits)}"
echo "=== Published successfully ==="
