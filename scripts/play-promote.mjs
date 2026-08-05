/**
 * Povýšení verze z interního testování do produkce, včetně poznámek k vydání.
 *
 * Play pracuje s „edity": otevře se rozpracovaná úprava, do ní se zapíše
 * vydání a nakonec se potvrdí. Bez potvrzení se nic nezveřejní, takže
 * `--dry-run` je bezpečné pouštět kdykoli.
 *
 * Použití:
 *   node tools-play-promote.mjs <versionCode> <souborSPoznamkami> [--commit]
 */
import { createSign } from 'crypto';
import { readFileSync } from 'fs';

const KEY_PATH = 'C:/Users/lidmi/Documents/bettermingle-play-key.json';
const PACKAGE = 'com.bettermingle.app';
const BASE = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}`;

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
async function call(path, { method = 'GET', body } = {}) {
  if (!token) token = await accessToken();
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
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

const [versionCodeArg, notesPath, ...flags] = process.argv.slice(2);
if (!versionCodeArg || !notesPath) {
  console.error('použití: <versionCode> <souborSPoznamkami> [--commit]');
  process.exit(1);
}
const versionCode = String(versionCodeArg);
const releaseNotes = parseNotes(notesPath);
console.log(`poznámky: ${releaseNotes.length} jazyků`);

const edit = await call('/edits', { method: 'POST' });
console.log('rozpracovaná úprava:', edit.id);

await call(`/edits/${edit.id}/tracks/production`, {
  method: 'PUT',
  body: {
    track: 'production',
    releases: [
      {
        name: '1.5.4',
        versionCodes: [versionCode],
        status: 'completed',
        releaseNotes,
      },
    ],
  },
});
console.log(`✓ verze ${versionCode} zapsána do produkčního kanálu`);

if (flags.includes('--commit')) {
  await call(`/edits/${edit.id}:commit`, { method: 'POST' });
  console.log('\nZVEŘEJNĚNO — verze jde k uživatelům');
} else {
  await call(`/edits/${edit.id}`, { method: 'DELETE' }).catch(() => undefined);
  console.log('\n[zkouška] úprava zahozena, v obchodě se nic nezměnilo');
  console.log('Zveřejníš přidáním --commit');
}
