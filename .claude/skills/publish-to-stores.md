---
name: publish-to-stores
description: Build and publish to AppGallery, RuStore, or Galaxy Store.
---

# Publish to Stores

Builds APK and publishes to the selected store via REST API.

## Usage

```bash
source .env && node .claude/skills/publish-to-stores/publish-to-stores.mjs <store>
```

Where `<store>` is `appgallery`, `rustore`, or `galaxystore`.

## Prerequisites

| Store | Env vars |
|-------|---------|
| AppGallery | `HUAWEI_CLIENT_ID`, `HUAWEI_CLIENT_SECRET`, `HUAWEI_APP_ID` (optional) |
| RuStore | `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEYFILE`, `WHATS_NEW_RU_FILE` (/tmp/rustore-whatsnew-ru.txt) |
| Galaxy Store | `GALAXY_SERVICE_ACCOUNT_ID`, `GALAXY_PRIVATE_KEYFILE`, `GALAXY_CONTENT_ID`, `WHATS_NEW_FILE` (/tmp/galaxystore-whatsnew.txt) |

Requires Node.js 20+.

## Release Notes

Before publishing, prepare release notes:

```bash
# RuStore (Russian only)
echo "Исправления ошибок и улучшение производительности" > /tmp/rustore-whatsnew-ru.txt

# Galaxy Store (English)
echo "Bug fixes and performance improvements" > /tmp/galaxystore-whatsnew.txt
```

## Structure

```
publish-to-stores/
  publish-to-stores.mjs          # entry: node publish-to-stores.mjs <store>
  lib/
    utils.mjs                    # shared: build, auth helpers, HTTP retry
    publish/
      appgallery.mjs             # Huawei AppGallery
      rustore.mjs                # RuStore (VK)
      galaxystore.mjs            # Samsung Galaxy Store
```
