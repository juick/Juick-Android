---
name: publish-rustore
description: Build and publish to RuStore. Google variant APK upload via REST API.
---

# Publish to RuStore

Builds `googleStore` APK and uploads via RuStore REST API.

## Prerequisites

```
RUSTORE_KEY_ID=<your key id>
RUSTORE_PRIVATE_KEYFILE=<path to RSA private key file>
RUSTORE_PACKAGE=com.juick           # optional
RUSTORE_PUBLISH_TYPE=INSTANTLY      # optional: INSTANTLY, MANUAL, DELAYED
```

Release notes via temp file (default):
- `WHATS_NEW_RU_FILE` → `/tmp/rustore-whatsnew-ru.txt` (Russian)

Generate credentials: RuStore Console → API keys.
Requires `jq` and `openssl`.

## Usage

```bash
# Prepare Russian release notes before each release
echo "..." > /tmp/rustore-whatsnew-ru.txt

# Publish
source .env && bash .claude/skills/publish-rustore.sh
```

## API Flow

1. `./gradlew clean assembleGoogleStore`
2. RSA sign `keyId + timestamp` → POST `/public/auth/` → JWE token
3. POST `/application/{package}/version` → create draft (with whatsNew from temp files)
4. POST `/application/{package}/version/{id}/apk` → upload APK
5. POST `/application/{package}/version/{id}/commit` → submit for review

API base: `https://public-api.rustore.ru/public`. Auth via `Public-Token` header.
