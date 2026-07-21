#!/bin/bash
set -euo pipefail

CLIENT_ID="${HUAWEI_CLIENT_ID:?Set HUAWEI_CLIENT_ID}"
CLIENT_SECRET="${HUAWEI_CLIENT_SECRET:?Set HUAWEI_CLIENT_SECRET}"
APP_ID="${HUAWEI_APP_ID:-}"

API_BASE="https://connect-api.cloud.huawei.com/api"

# 1. Build
echo "=== Building APK ==="
./gradlew clean assembleHuaweiStore

APK_PATH=$(find build/outputs/apk/huawei/store -name "*.apk" | head -1)
echo "APK: $APK_PATH"

# 2. Get OAuth token
echo "=== Getting access token ==="
TOKEN=$(curl -sf -X POST "${API_BASE}/oauth2/v1/token" \
  -H "Content-Type: application/json" \
  -d "{\"client_id\":\"${CLIENT_ID}\",\"grant_type\":\"client_credentials\",\"client_secret\":\"${CLIENT_SECRET}\"}" \
  | jq -r .access_token)
echo "Token obtained"

# 3. Resolve app ID if not provided
if [ -z "$APP_ID" ]; then
  echo "=== Resolving app ID ==="
  APP_ID=$(curl -sf -G "${API_BASE}/publish/v2/appid-list" \
    --data-urlencode "packageName=com.juick" \
    -H "client_id: ${CLIENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}" \
    | jq -r .appids[0].value)
  echo "App ID: $APP_ID"
fi

# 4. Get upload URL
echo "=== Getting upload URL ==="
FILE_SIZE=$(wc -c < "$APK_PATH" | tr -d ' ')
UPLOAD_INFO=$(curl -sf -G "${API_BASE}/publish/v2/upload-url/for-obs" \
  --data-urlencode "appId=${APP_ID}" \
  --data-urlencode "fileName=release.apk" \
  --data-urlencode "contentLength=${FILE_SIZE}" \
  --data-urlencode "suffix=apk" \
  -H "client_id: ${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json")

UPLOAD_URL=$(jq -r .urlInfo.url <<< "$UPLOAD_INFO")
OBJECT_ID=$(jq -r .urlInfo.objectId <<< "$UPLOAD_INFO")
AUTH_HEADER=$(jq -r '.urlInfo.headers.Authorization' <<< "$UPLOAD_INFO")
CONTENT_TYPE_HEADER=$(jq -r '.urlInfo.headers."Content-Type"' <<< "$UPLOAD_INFO")
X_AMZ_DATE=$(jq -r '.urlInfo.headers."x-amz-date"' <<< "$UPLOAD_INFO")
X_AMZ_SHA256=$(jq -r '.urlInfo.headers."x-amz-content-sha256"' <<< "$UPLOAD_INFO")

# 5. Upload APK to OBS
echo "=== Uploading APK ==="
curl -sf -X PUT "$UPLOAD_URL" \
  -H "Authorization: ${AUTH_HEADER}" \
  -H "Content-Type: ${CONTENT_TYPE_HEADER}" \
  -H "x-amz-date: ${X_AMZ_DATE}" \
  -H "x-amz-content-sha256: ${X_AMZ_SHA256}" \
  --data-binary "@${APK_PATH}" \
  -o /dev/null -w "%{http_code}"
echo ""
echo "Upload done"

# 6. Save file info
echo "=== Saving file info ==="
curl -sf -X PUT "${API_BASE}/publish/v2/app-file-info?appId=${APP_ID}" \
  -H "client_id: ${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"fileType\":5,\"files\":[{\"fileName\":\"release.apk\",\"fileDestUrl\":\"${OBJECT_ID}\"}]}"
echo ""
echo "File info saved"

# 7. Submit for review
echo "=== Submitting for review ==="
SUBMIT_RESPONSE=$(curl -sf -X POST "${API_BASE}/publish/v2/app-submit?appId=${APP_ID}" \
  -H "client_id: ${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json")
echo "$SUBMIT_RESPONSE"

CODE=$(jq -r .ret.code <<< "$SUBMIT_RESPONSE")
if [ "$CODE" = "0" ]; then
  echo "=== Submitted successfully ==="
elif [ "$CODE" = "204144660" ]; then
  echo "=== Build still processing, waiting 2 min ==="
  sleep 120
  curl -sf -X POST "${API_BASE}/publish/v2/app-submit?appId=${APP_ID}" \
    -H "client_id: ${CLIENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json"
  echo ""
  echo "=== Retry done ==="
else
  echo "=== Submit failed ==="
  exit 1
fi
