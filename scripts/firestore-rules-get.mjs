/**
 * Stáhne pravidla Firestore, která právě běží na produkci.
 *
 * Firebase CLI na tohle příkaz nemá — `firestore:rules:get` neexistuje,
 * umí jen nasazovat. Čte se proto rovnou Rules API. Hodí se před každým
 * nasazením: bez zálohy není co vrátit.
 *
 *   node scripts/firestore-rules-get.mjs [výstupní-soubor]
 *
 * Přihlášení bere z `gcloud auth`; hlavička x-goog-user-project musí být
 * uvedená, jinak API uživatelské přihlášení odmítne.
 */
import { execSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const PROJECT = 'bettermingle';
const out = process.argv[2] ?? 'pravidla-pred-nasazenim.rules';

const token = execSync('gcloud auth print-access-token', { encoding: 'utf8' }).trim();
const headers = { Authorization: `Bearer ${token}`, 'x-goog-user-project': PROJECT };

const release = await (
  await fetch(
    `https://firebaserules.googleapis.com/v1/projects/${PROJECT}/releases/cloud.firestore`,
    { headers }
  )
).json();

if (release.error) {
  console.error('Rules API odmítlo dotaz:', release.error.message);
  process.exit(1);
}

const ruleset = await (
  await fetch(`https://firebaserules.googleapis.com/v1/${release.rulesetName}`, { headers })
).json();

writeFileSync(out, ruleset.source.files.map((f) => f.content).join('\n'), 'utf8');

console.log(`nasazeno : ${release.updateTime}`);
console.log(`ruleset  : ${release.rulesetName.split('/').pop()}`);
console.log(`uloženo  : ${out}`);
