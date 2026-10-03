#!/usr/bin/env node
/*
 * Reorganiza classes Java movendo-as para novos pacotes (package-by-feature).
 *
 * Uso: node scripts/move-module.js <mapping.json>
 *   mapping.json: { "com.antigo.Classe": "com.novo.pacote", ... }
 *
 * Para cada classe: faz `git mv` (preserva histórico), reescreve a declaração
 * `package`, atualiza imports e referências totalmente qualificadas em todo o
 * src/, e adiciona imports quando duas classes que dividiam o mesmo pacote
 * passam a ficar em pacotes diferentes.
 */
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const mapping = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));

const pkgOf = (fqcn) => fqcn.slice(0, fqcn.lastIndexOf('.'));
const simple = (fqcn) => fqcn.slice(fqcn.lastIndexOf('.') + 1);
const toPath = (pkg, base, name) => path.join(ROOT, base, ...pkg.split('.'), name + '.java');

function listJava(dir) {
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...listJava(p));
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

// 1. Move os arquivos.
const moved = {}; // novo caminho -> { oldFqcn, newFqcn }
for (const [oldFqcn, newPkg] of Object.entries(mapping)) {
  const name = simple(oldFqcn);
  const base = ['src/main/java', 'src/test/java'].find((b) => fs.existsSync(toPath(pkgOf(oldFqcn), b, name)));
  if (!base) throw new Error('Classe não encontrada: ' + oldFqcn);
  const from = toPath(pkgOf(oldFqcn), base, name);
  const to = toPath(newPkg, base, name);
  fs.mkdirSync(path.dirname(to), { recursive: true });
  execFileSync('git', ['mv', from, to], { cwd: ROOT });
  moved[to] = { oldFqcn, newFqcn: newPkg + '.' + name };
}

const esc = (s) => s.replace(/[.$]/g, '\\$&');
const byOld = Object.entries(mapping).map(([o, p]) => ({ o, n: p + '.' + simple(o) }));

// 2. Reescreve todos os arquivos .java.
for (const file of [...listJava(path.join(ROOT, 'src/main/java')), ...listJava(path.join(ROOT, 'src/test/java'))]) {
  let src = fs.readFileSync(file, 'utf8');
  const orig = src;
  const nl = src.includes('\r\n') ? '\r\n' : '\n';

  // Referências qualificadas (imports, static imports, FQCN no corpo).
  for (const { o, n } of byOld) {
    src = src.replace(new RegExp('(?<![\\w.])' + esc(o) + '(?![\\w])', 'g'), n);
  }

  const pkgMatch = src.match(/^package ([\w.]+);/m);
  const oldPkg = pkgMatch && pkgMatch[1];
  const info = moved[file];
  const myPkg = info ? pkgOf(info.newFqcn) : oldPkg;
  if (info) src = src.replace(/^package [\w.]+;/m, 'package ' + myPkg + ';');

  // Imports para classes antes visíveis por estarem no mesmo pacote.
  const body = src.replace(/^import .*$/gm, '');
  const needed = [];
  const candidates = byOld.filter(({ o, n }) => {
    const wasSamePkg = pkgOf(o) === (info ? pkgOf(info.oldFqcn) : oldPkg);
    return wasSamePkg && pkgOf(n) !== myPkg && n !== (info && info.newFqcn);
  });
  // Arquivo movido também perde acesso às classes que ficaram no pacote antigo.
  if (info) {
    const oldDir = path.dirname(toPath(pkgOf(info.oldFqcn), file.includes('src' + path.sep + 'test') ? 'src/test/java' : 'src/main/java', 'x'));
    if (fs.existsSync(oldDir)) {
      for (const f of fs.readdirSync(oldDir).filter((f) => f.endsWith('.java'))) {
        candidates.push({ n: pkgOf(info.oldFqcn) + '.' + f.slice(0, -5) });
      }
    }
  }
  for (const { n } of candidates) {
    const re = new RegExp('(?<![\\w.])' + simple(n) + '(?![\\w])');
    if (re.test(body) && !src.includes('import ' + n + ';')) needed.push('import ' + n + ';');
  }
  if (needed.length) {
    src = src.replace(/^(package [\w.]+;\r?\n)/m, '$1' + nl + needed.join(nl) + nl);
  }

  if (src !== orig) fs.writeFileSync(file, src);
}
console.log('Movidas ' + Object.keys(mapping).length + ' classes.');
