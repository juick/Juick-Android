---
name: publish-galaxystore
description: Build and publish to Samsung Galaxy Store. Google variant APK upload via Content Publish API.
---

# Publish to Samsung Galaxy Store

Builds `googleStore` APK and uploads via Galaxy Store Content Publish API.

## Prerequisites

```
GALAXY_SERVICE_ACCOUNT_ID=<your service account id>
GALAXY_PRIVATE_KEYFILE=<path to RSA private key file>
GALAXY_CONTENT_ID=<your app content id>
```

Release notes via temp file:
- `WHATS_NEW_FILE` → `/tmp/galaxystore-whatsnew.txt` (English)

Generate credentials: Galaxy Store Seller Portal → Assistance → API Service.
Requires `jq` and `openssl`.

## Usage

```bash
echo "Bug fixes and performance improvements" > /tmp/galaxystore-whatsnew.txt
source .env && bash .claude/skills/publish-galaxystore.sh
```

## API Flow

1. `./gradlew clean assembleGoogleStore`
2. JWT `RS256` → POST `/accessToken` → bearer token
3. POST `/createUploadSessionId` → sessionId
4. POST `fileUpload` (multipart APK + sessionId) → fileKey
5. POST `/contentUpdate` → create update with whatsNew
6. POST `/v2/content/binary` → register binary
7. POST `/contentSubmit` → submit for review

API base: `https://devapi.samsungapps.com/seller`. All calls pass `Authorization: Bearer` + `service-account-id` headers.
