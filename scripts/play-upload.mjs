/**
 * Nahraje AAB do Google Play a vydá ho ve zvoleném kanálu.
 *
 * Nahrání i vydání proběhne v jedné rozpracované úpravě, takže se nemůže
 * stát, že by v obchodě zůstal nahraný balíček, který nikam nepatří.
 * Bez --commit se úprava na konci zahodí a v obchodě se nezmění nic.
 *
 *   node scripts/play-upload.mjs <aab> <poznámky.txt> [--track=production] [--commit]
 */
import { createSign } from 'node:crypto';
import { readFileSync, statSync } from 'node:fs';

const KEY_PATH = 'C:/Users/lidmi/Documents/bettermingle-play-key.json';
const PACKAGE = 'com.bettermingle.app';
const API = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}`;
const UPLOAD = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${PACKAGE}`;

const b64 = (x) => Buffer.from(x).toString('base64url');

async function accessToken() {
  const key = JSON.parse(readFileSync(KEY_PATH, 'utf8'));
  const now = Math.floor(Date.now() / 1000);
  const header = b64(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claims = b64(
    JSON.stringify({
      iss: key.client_email,
      scope: 'https://www.googleapis.com/auth/androidpublisher',
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600,
    })
  );
  const signer = createSign('RSA-SHA256');
  signer.update(`${header}.${claims}`);
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${header}.${claims}.${signer.sign(key.private_key, 'base64url')}`,
    }),
  });
  const json = await res.json();
  if (!json.access_token) throw new Error(JSON.stringify(json).slice(0, 200));
  return json.access_token;
}

let token = '';
async function call(url, { method = 'GET', body, raw, contentType } = {}) {
  if (!token) token = await accessToken();
  const res = await fetch(url, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': contentType ?? 'application/json',
    },
    body: raw ?? (body ? JSON.stringify(body) : undefined),
  });
  const text = await res.text();
  const json = text ? JSON.parse(text) : {};
  if (!res.ok) throw new Error(`${res.status} ${JSON.stringify(json.error ?? json).slice(0, 400)}`);
  return json;
}

/** Rozebere soubor s bloky <jazyk>…</jazyk> na seznam pro Play. */
function parseNotes(path) {
  const raw = readFileSync(path, 'utf8');
  const notes = [];
  for (const m of raw.matchAll(/<([a-zA-Z-]+)>\r?\n([\s\S]*?)\r?\n<\/\1>/g)) {
    notes.push({ language: m[1], text: m[2].trim() });
  }
  return notes;
}

const [aabPath, notesPath, ...flags] = process.argv.slice(2);
if (!aabPath || !notesPath) {
  console.error('použití: <aab> <poznámky.txt> [--track=production] [--commit]');
  process.exit(1);
}

const track = (flags.find((f) => f.startsWith('--track=')) ?? '--track=production').split('=')[1];
const releaseNotes = parseNotes(notesPath);
const megabytes = (statSync(aabPath).size / 1024 / 1024).toFixed(1);

console.log(`balíček : ${aabPath} (${megabytes} MB)`);
console.log(`kanál   : ${track}`);
console.log(`poznámky: ${releaseNotes.length} jazyků`);

const edit = await call(`${API}/edits`, { method: 'POST' });
console.log(`\nrozpracovaná úprava: ${edit.id}`);

console.log('nahrávám…');
const bundle = await call(`${UPLOAD}/edits/${edit.id}/bundles?uploadType=media`, {
  method: 'POST',
  raw: readFileSync(aabPath),
  contentType: 'application/octet-stream',
});
console.log(`✓ nahráno jako versionCode ${bundle.versionCode}`);

await call(`${API}/edits/${edit.id}/tracks/${track}`, {
  method: 'PUT',
  body: {
    track,
    releases: [
      {
        name: String(bundle.versionCode),
        versionCodes: [String(bundle.versionCode)],
        status: 'completed',
        releaseNotes,
      },
    ],
  },
});
console.log(`✓ zapsáno do kanálu ${track}`);

if (flags.includes('--commit')) {
  await call(`${API}/edits/${edit.id}:commit`, { method: 'POST' });
  console.log('\nZVEŘEJNĚNO — verze jde k uživatelům');
} else {
  await call(`${API}/edits/${edit.id}`, { method: 'DELETE' }).catch(() => undefined);
  console.log('\n[zkouška] úprava zahozena, v obchodě se nic nezměnilo');
}
