#!/usr/bin/env node
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const store = process.argv[2];

const stores = {
  appgallery: './lib/publish/appgallery.mjs',
  rustore: './lib/publish/rustore.mjs',
  galaxystore: './lib/publish/galaxystore.mjs',
};

if (!store || !stores[store]) {
  console.error(`Usage: node publish-to-stores.mjs <${Object.keys(stores).join('|')}>`);
  process.exit(1);
}

try {
  const mod = await import(stores[store]);
  await mod.publish();
} catch (e) {
  console.error(e.message);
  process.exit(1);
}
