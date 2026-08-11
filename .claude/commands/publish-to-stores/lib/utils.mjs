import { execSync } from 'child_process';
import { readFileSync } from 'fs';
import { homedir } from 'os';

export function exec(cmd, opts = {}) {
  return execSync(cmd, { encoding: 'utf-8', stdio: ['pipe', 'pipe', 'inherit'], ...opts }).trim();
}

export async function fetchWithRetry(url, opts = {}, retries = 3) {
  const { body, headers, method = 'GET' } = opts;
  const fetchOpts = { method, headers };
  if (body) fetchOpts.body = typeof body === 'string' ? body : JSON.stringify(body);

  let lastError;
  for (let i = 0; i < retries; i++) {
    try {
      const res = await fetch(url, fetchOpts);
      const text = await res.text();
      if (!res.ok) throw new Error(`HTTP ${res.status}: ${text}`);
      return text ? JSON.parse(text) : {};
    } catch (e) {
      lastError = e;
      if (i < retries - 1) await new Promise(r => setTimeout(r, 5000));
    }
  }
  throw lastError;
}

export function readFileOrFail(path, name) {
  try {
    return readFileSync(path, 'utf-8').trim();
  } catch {
    console.error(`Missing ${name}: ${path}`);
    process.exit(1);
  }
}

export function loadPemKey(keyfile) {
  const raw = readFileSync(keyfile.replace(/^~/, homedir()), 'utf-8').trim();
  if (raw.includes('BEGIN')) return raw;
  return `-----BEGIN PRIVATE KEY-----\n${raw}\n-----END PRIVATE KEY-----`;
}

export function b64url(buf) {
  return Buffer.from(buf).toString('base64')
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function buildApk(variant, buildType = 'store') {
  console.log('=== Building APK ===');
  const v = variant.charAt(0).toUpperCase() + variant.slice(1);
  const t = buildType.charAt(0).toUpperCase() + buildType.slice(1);
  exec(`./gradlew assemble${v}${t}`);
  const dir = `build/outputs/apk/${variant}/${buildType}`;
  const path = exec(`ls ${dir}/*.apk | head -1`);
  console.log(`APK: ${path}`);
  return path;
}

export function requireEnv(names) {
  for (const name of names) {
    if (!process.env[name]) {
      console.error(`Set ${name}`);
      process.exit(1);
    }
  }
}

export function requireFile(name, path) {
  const content = readFileOrFail(path, name);
  if (!content) {
    console.error(`Prepare ${path}`);
    process.exit(1);
  }
  return content;
}
