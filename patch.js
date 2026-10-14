#!/usr/bin/env node
// patch.js v2 — идемпотентный патчер + создание новых файлов.
// Маркеры:
//   @@@FILE@@@ <path>
//   @@@CREATE@@@ \n <content> \n @@@END@@@          — создать файл (если не существует)
//   @@@IF-NOT@@@ \n <guard> \n @@@FIND@@@ \n <find> \n @@@REPLACE@@@ \n <replace> \n @@@END@@@
//   @@@FIND@@@ \n <find> \n @@@REPLACE@@@ \n <replace> \n @@@END@@@
// Запуск: node patch.js patch.txt .

const fs = require('fs');
const path = require('path');

const patchFile = process.argv[2] || 'patch.txt';
const baseDir   = process.argv[3] || process.cwd();

if (!fs.existsSync(patchFile)) {
  console.error('❌ Патч-файл не найден:', patchFile);
  process.exit(1);
}

const raw = fs.readFileSync(patchFile, 'utf8');
const byFile = {};

const sectionRe = /@@@FILE@@@[ \t]*([^\n\r]+)\r?\n([\s\S]*?)(?=@@@FILE@@@|$)/g;
let m;
while ((m = sectionRe.exec(raw)) !== null) {
  const fileName = m[1].trim();
  const body = m[2];
  const patches = [];

  const createRe = /@@@CREATE@@@\r?\n([\s\S]*?)@@@END@@@/g;
  let c;
  while ((c = createRe.exec(body)) !== null) {
    patches.push({ type: 'create', content: c[1] });
  }

  const findRe = /(?:@@@IF-NOT@@@\r?\n([\s\S]*?))?@@@FIND@@@\r?\n([\s\S]*?)@@@REPLACE@@@\r?\n([\s\S]*?)@@@END@@@/g;
  let p;
  while ((p = findRe.exec(body)) !== null) {
    patches.push({ type: 'replace', ifNot: p[1] != null ? p[1] : null, find: p[2], replace: p[3] });
  }

  if (!byFile[fileName]) byFile[fileName] = [];
  byFile[fileName].push(...patches);
}

let ok = 0, fail = 0, skip = 0, created = 0;

for (const fileName of Object.keys(byFile)) {
  const fullPath = path.resolve(baseDir, fileName);
  const patches = byFile[fileName];
  const creates  = patches.filter(x => x.type === 'create');
  const replaces = patches.filter(x => x.type === 'replace');
  const exists   = fs.existsSync(fullPath);

  if (!exists) {
    if (creates.length === 0) {
      console.log(`\n❌ Файл не найден: ${fullPath}`);
      fail += patches.length;
      continue;
    }
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, creates[0].content);
    console.log(`\n📝 ${fileName} — создан (${creates[0].content.length} байт)`);
    created++;
    continue;
  }

  const original = fs.readFileSync(fullPath, 'utf8');
  let src = original;
  console.log(`\n📝 ${fileName} — ${replaces.length} патчей`);
  replaces.forEach((p, i) => {
    if (p.ifNot && src.indexOf(p.ifNot) !== -1) {
      console.log(`  ⊘ #${i+1}: уже применён (guard найден), пропущен`);
      skip++;
      return;
    }
    const idx = src.indexOf(p.find);
    if (idx === -1) {
      console.log(`  ❌ #${i+1}: не найдено: "${shorten(p.find)}"`);
      fail++;
      return;
    }
    const cnt = src.split(p.find).length - 1;
    if (cnt > 1) console.log(`  ⚠ #${i+1}: ${cnt} совпадений, применяю первое`);
    src = src.slice(0, idx) + p.replace + src.slice(idx + p.find.length);
    console.log(`  ✓ #${i+1}: применён`);
    ok++;
  });

  if (src !== original) {
    fs.writeFileSync(fullPath + '.bak', original);
    fs.writeFileSync(fullPath, src);
    console.log(`  💾 Записано. Бэкап: ${fileName}.bak`);
  }
}

console.log(`\n=== ok ${ok}   skip ${skip}   fail ${fail}   created ${created} ===`);
function shorten(s){ return s.length > 60 ? s.slice(0, 57) + '...' : s; }