import { readFileSync } from 'fs';
import { createSign } from 'crypto';
import { fetchWithRetry, requireEnv, requireFile, loadPemKey, b64url, buildApk } from '../utils.mjs';

const API_BASE = 'https://devapi.samsungapps.com/seller';
const UPLOAD_URL = 'https://seller.samsungapps.com/galaxyapi/fileUpload';
const AUTH_URL = 'https://devapi.samsungapps.com/auth/accessToken';

export async function publish() {
  requireEnv(['GALAXY_SERVICE_ACCOUNT_ID', 'GALAXY_PRIVATE_KEYFILE', 'GALAXY_CONTENT_ID']);
  const serviceAccountId = process.env.GALAXY_SERVICE_ACCOUNT_ID;
  const keyFile = process.env.GALAXY_PRIVATE_KEYFILE;
  const contentId = process.env.GALAXY_CONTENT_ID;
  const whatsNewFile = process.env.WHATS_NEW_FILE || '/tmp/galaxystore-whatsnew.txt';
  const whatsNew = requireFile('release notes', whatsNewFile);

  // 1. Build
  const apkPath = buildApk('google', 'google', 'store');

  // 2. Auth
  console.log('=== Getting access token ===');
  const pem = loadPemKey(keyFile);
  const now = Math.floor(Date.now() / 1000);
  const exp = now + 1200;
  const jwtHeader = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const jwtBody = b64url(JSON.stringify({ iss: serviceAccountId, scopes: ['publishing'], iat: now, exp }));
  const signingInput = `${jwtHeader}.${jwtBody}`;
  const sign = createSign('SHA256');
  sign.update(signingInput);
  const jwtSig = b64url(sign.sign(pem));
  const jwt = `${signingInput}.${jwtSig}`;

  const authResp = await fetchWithRetry(AUTH_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${jwt}` },
  });
  const token = authResp.createdItem.accessToken;
  console.log('Token obtained');

  // 3. Upload session
  console.log('=== Creating upload session ===');
  const sessionResp = await fetchWithRetry(`${API_BASE}/createUploadSessionId`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'service-account-id': serviceAccountId },
  });
  const sessionId = sessionResp.sessionId;
  console.log(`Session: ${sessionId}`);

  // 4. Upload APK
  console.log('=== Uploading APK ===');
  const formData = new FormData();
  formData.append('sessionId', sessionId);
  formData.append('file', new Blob([readFileSync(apkPath)]), apkPath.split('/').pop());

  const fileResp = await fetchWithRetry(UPLOAD_URL, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'service-account-id': serviceAccountId },
    body: formData,
  });
  const fileKey = fileResp.fileKey;
  console.log(`File key: ${fileKey}`);

  // 5. Create update
  console.log('=== Creating update ===');
  await fetchWithRetry(`${API_BASE}/contentUpdate`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'service-account-id': serviceAccountId, 'Content-Type': 'application/json' },
    body: JSON.stringify({ contentId, whatsNew }),
  });
  console.log('Update created');

  // 6. Register binary
  console.log('=== Registering binary ===');
  const binaryResp = await fetchWithRetry(`${API_BASE}/v2/content/binary`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'service-account-id': serviceAccountId, 'Content-Type': 'application/json' },
    body: JSON.stringify({ contentId, gms: 'Y', filekey: fileKey }),
  });
  console.log(JSON.stringify(binaryResp));

  // 7. Verify status
  console.log('=== Verifying submission ===');
  const info = await fetchWithRetry(`${API_BASE}/contentInfo?contentId=${contentId}`, {
    headers: { Authorization: `Bearer ${token}`, 'service-account-id': serviceAccountId },
  });
  const status = info[0]?.contentStatus;
  console.log(`Status: ${status}`);
  if (status === 'UPDATING' || status === 'REGISTERING') {
    console.log('=== Published successfully ===');
  } else {
    console.log(`=== Unexpected status: ${status} ===`);
    process.exit(1);
  }
}
