import { readFileSync, statSync } from 'fs';
import { fetchWithRetry, requireEnv, buildApk } from '../utils.mjs';

const API_BASE = 'https://connect-api.cloud.huawei.com/api';

export async function publish() {
  requireEnv(['HUAWEI_CLIENT_ID', 'HUAWEI_CLIENT_SECRET']);
  const clientId = process.env.HUAWEI_CLIENT_ID;
  const clientSecret = process.env.HUAWEI_CLIENT_SECRET;
  let appId = process.env.HUAWEI_APP_ID;

  // 1. Build
  const apkPath = buildApk('huawei', 'huawei', 'store');

  // 2. Auth
  console.log('=== Getting access token ===');
  const auth = await fetchWithRetry(`${API_BASE}/oauth2/v1/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client_id: clientId, grant_type: 'client_credentials', client_secret: clientSecret }),
  });
  const token = auth.access_token;
  console.log('Token obtained');

  // 3. Resolve app ID
  if (!appId) {
    console.log('=== Resolving app ID ===');
    const list = await fetchWithRetry(`${API_BASE}/publish/v2/appid-list?packageName=com.juick`, {
      headers: { client_id: clientId, Authorization: `Bearer ${token}` },
    });
    appId = list.appids[0].value;
    console.log(`App ID: ${appId}`);
  }

  // 4. Upload
  console.log('=== Uploading APK ===');
  const fileSize = statSync(apkPath).size;
  const uploadInfo = await fetchWithRetry(
    `${API_BASE}/publish/v2/upload-url/for-obs?appId=${appId}&fileName=release.apk&contentLength=${fileSize}&suffix=apk`,
    { headers: { client_id: clientId, Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } }
  );
  const { url, objectId, headers: uploadHeaders } = uploadInfo.urlInfo;

  await fetch(url, { method: 'PUT', headers: uploadHeaders, body: readFileSync(apkPath) });
  console.log('Upload done');

  // 5. Save file info
  console.log('=== Saving file info ===');
  await fetchWithRetry(`${API_BASE}/publish/v2/app-file-info?appId=${appId}`, {
    method: 'PUT',
    headers: { client_id: clientId, Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileType: 5, files: [{ fileName: 'release.apk', fileDestUrl: objectId }] }),
  });
  console.log('File info saved');

  // 6. Submit
  console.log('=== Submitting for review ===');
  try {
    const result = await fetchWithRetry(`${API_BASE}/publish/v2/app-submit?appId=${appId}`, {
      method: 'POST',
      headers: { client_id: clientId, Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    });
    console.log(JSON.stringify(result));
    if (result.ret?.code === 0) {
      console.log('=== Submitted successfully ===');
    } else if (result.ret?.code === 204144660) {
      console.log('=== Build still processing, waiting 2 min ===');
      await new Promise(r => setTimeout(r, 120000));
      const retry = await fetchWithRetry(`${API_BASE}/publish/v2/app-submit?appId=${appId}`, {
        method: 'POST',
        headers: { client_id: clientId, Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      });
      console.log(JSON.stringify(retry));
      console.log('=== Retry done ===');
    } else {
      console.log('=== Submit failed ===');
      process.exit(1);
    }
  } catch (e) {
    console.error(e.message);
    process.exit(1);
  }
}
