// Transforme les dessins SVG de art/ en images WebP pour l'appli.
// Usage : node art/rendre.js   (Playwright + Chromium, puis python3 avec Pillow pour le WebP)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const art = __dirname;
const sortie = path.join(art, '..', 'app', 'src', 'main', 'res', 'drawable-nodpi');
const tmp = fs.mkdtempSync(path.join(require('os').tmpdir(), 'rendu-'));

// [fichier source, nom de la ressource, largeur en pixels]
const travaux = [
  ['scenes/nuit.svg', 'scene_nuit', 1080],
  ['scenes/jour.svg', 'scene_jour', 1080],
  ['tombes/tombe_nuit.svg', 'tombe_nuit', 288],
  ['tombes/tombe_jour.svg', 'tombe_jour', 288],
  ['tombes/fantome.svg', 'fantome', 192],
];
for (const f of fs.readdirSync(path.join(art, 'emblemes'))) {
  if (f.endsWith('.svg')) travaux.push([`emblemes/${f}`, `embleme_${f.replace('.svg', '')}`, 480]);
}

(async () => {
  const navigateur = await chromium.launch();
  const page = await navigateur.newPage();
  for (const [src, nom, largeur] of travaux) {
    const svg = fs.readFileSync(path.join(art, src), 'utf8');
    const [, w, h] = svg.match(/viewBox="0 0 (\d+) (\d+)"/);
    const hauteur = Math.round((largeur * h) / w);
    await page.setViewportSize({ width: largeur, height: hauteur });
    const sized = svg.replace(/width="\d+" height="\d+"/, `width="${largeur}" height="${hauteur}"`);
    await page.setContent(`<html><body style="margin:0;background:transparent">${sized}</body></html>`);
    const png = path.join(tmp, `${nom}.png`);
    await page.screenshot({ path: png, omitBackground: true });
    execFileSync('python3', ['-c',
      'import sys; from PIL import Image; Image.open(sys.argv[1]).save(sys.argv[2], "WEBP", quality=88, method=6)',
      png, path.join(sortie, `${nom}.webp`)]);
    console.log(nom, `${largeur}x${hauteur}`);
  }
  await navigateur.close();
})();
