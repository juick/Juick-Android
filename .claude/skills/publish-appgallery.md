---
name: publish-appgallery
description: Build and publish to Huawei AppGallery Connect. Replaces old fastlane publish_huawei lane.
---

# Publish to Huawei AppGallery

Builds `huaweiStore` APK and uploads via AppGallery Connect REST API.

## Prerequisites

```
HUAWEI_CLIENT_ID=<your client id>
HUAWEI_CLIENT_SECRET=<your client secret>
HUAWEI_APP_ID=<your app id>   # optional, auto-resolved from com.juick
```

Get credentials: AppGallery Connect console → Users and permissions → API key.

Requires `jq` (https://jqlang.github.io/jq/). Windows: `winget install jqlang.jq`.

## Usage

```bash
source .env && bash .claude/skills/publish-appgallery.sh
```

## API Flow

1. `./gradlew clean assembleHuaweiStore`
2. OAuth2 client credentials → bearer token
3. GET `/publish/v2/upload-url/for-obs` → signed OBS upload URL
4. PUT binary APK to signed URL
5. PUT `/publish/v2/app-file-info` (fileType: 5)
6. POST `/publish/v2/app-submit` → submit for review

API base: `https://connect-api.cloud.huawei.com/api`. All calls pass `client_id` header + `Authorization: Bearer <token>`.
