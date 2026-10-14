import { writeFileSync, unlinkSync } from 'fs';
import { exec, requireFile, buildApk } from '../utils.mjs';

export async function publish() {
  const notesFile = process.env.WHATS_NEW_FILE || '/tmp/github-whatsnew.txt';
  const notes = requireFile('release notes', notesFile);
  const versionCode = exec("grep versionCode build.gradle | head -1 | sed 's/[^0-9]*//g'");
  const version = `3.2.${versionCode}`;
  const tag = `v${version}`;

  const googleApk = buildApk('google', 'release');
  const freeApk = buildApk('free', 'release');

  const tmpFile = `/tmp/gh-notes-${versionCode}.txt`;
  writeFileSync(tmpFile, notes);

  console.log('=== Creating GitHub Release ===');
  exec(`gh release create ${tag} ${googleApk} ${freeApk} --title "${version}" --notes-file ${tmpFile} --repo juick/Juick-Android`);
  unlinkSync(tmpFile);
  console.log(`Released ${tag}`);
}
