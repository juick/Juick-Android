import { readFileSync } from 'fs';
import { createSign } from 'crypto';
import { fetchWithRetry, requireEnv, requireFile, loadPemKey, buildApk } from '../utils.mjs';

const API_BASE = 'https://public-api.rustore.ru/public/v1';
const AUTH_URL = 'https://public-api.rustore.ru/public/auth/';

export async function publish() {
  requireEnv(['RUSTORE_KEY_ID', 'RUSTORE_PRIVATE_KEYFILE']);
  const keyId = process.env.RUSTORE_KEY_ID;
  const keyFile = process.env.RUSTORE_PRIVATE_KEYFILE;
  const pkg = process.env.RUSTORE_PACKAGE || 'com.juick';
  const whatsNewFile = process.env.WHATS_NEW_RU_FILE || '/tmp/rustore-whatsnew-ru.txt';
  const whatsNew = requireFile('Russian release notes', whatsNewFile);

  // 1. Build
  const apkPath = buildApk('google', 'google', 'store');

  // 2. Auth
  console.log('=== Getting access token ===');
  const pem = loadPemKey(keyFile);
  const timestamp = new Date().toISOString().replace(/\.\d{3}Z$/, '.000+00:00');
  const msg = keyId + timestamp;
  const sign = createSign('SHA512');
  sign.update(msg);
  const signature = sign.sign(pem, 'base64');

  const authResp = await fetchWithRetry(AUTH_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ keyId, timestamp, signature }),
  });
  const token = authResp.body.jwe;
  console.log('Token obtained');

  // 3. Create draft
  console.log('=== Creating draft ===');
  const draft = await fetchWithRetry(`${API_BASE}/application/${pkg}/version`, {
    method: 'POST',
    headers: { 'Public-Token': token, 'Content-Type': 'application/json' },
    body: JSON.stringify({ publishType: 'INSTANTLY', whatsNew }),
  });
  const versionId = draft.body;
  console.log(`Version ID: ${versionId}`);

  // 4. Upload APK
  console.log('=== Uploading APK ===');
  const formData = new FormData();
  formData.append('file', new Blob([readFileSync(apkPath)]), apkPath.split('/').pop());

  await fetchWithRetry(
    `${API_BASE}/application/${pkg}/version/${versionId}/apk?isMainApk=true&servicesType=Unknown`,
    { method: 'POST', headers: { 'Public-Token': token }, body: formData }
  );
  console.log('Upload done');

  // 5. Submit
  console.log('=== Submitting for review ===');
  const submit = await fetchWithRetry(`${API_BASE}/application/${pkg}/version/${versionId}/commit`, {
    method: 'POST',
    headers: { 'Public-Token': token, 'Content-Type': 'application/json' },
  });
  console.log(JSON.stringify(submit));

  if (submit.code === 'OK') {
    console.log('=== Submitted successfully ===');
  } else {
    console.log('=== Submit failed ===');
    process.exit(1);
  }
}
