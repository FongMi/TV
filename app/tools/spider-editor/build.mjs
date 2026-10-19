import {build} from 'esbuild';
import {copyFile, readFile, readdir, unlink, writeFile} from 'node:fs/promises';
import path from 'node:path';

const directory = '../../src/main/assets/js/spider-format';
const editor = await build({
    entryPoints: ['editor.mjs'], outfile: '../../src/main/assets/js/spider-editor.js',
    bundle: true, minify: true, target: 'es2020', format: 'iife',
    globalName: 'SpiderEditor', legalComments: 'external', metafile: true
});
for (const name of await readdir(directory)) await unlink(path.join(directory, name));
const formatter = await build({
    entryPoints: ['worker.mjs'], outdir: directory,
    bundle: true, splitting: true, minify: true, target: 'es2020', format: 'esm',
    legalComments: 'external', metafile: true,
    plugins: [{
        name: 'prettier-browser',
        setup(build) {
            build.onResolve({filter: /^prettier$/}, () => ({path: path.resolve('node_modules/prettier/standalone.mjs')}));
        }
    }]
});
await copyFormatterRuntimes();
await writeLicenses([editor, formatter]);

async function copyFormatterRuntimes() {
    for (const [source, name] of [
        ['web-tree-sitter/web-tree-sitter.wasm', 'web-tree-sitter.wasm'],
        ['prettier-plugin-java/dist/tree-sitter-java_orchard.wasm', 'tree-sitter-java_orchard.wasm']
    ]) await copyFile(path.join('node_modules', source), path.join(directory, name));
}

async function writeLicenses(results) {
    const packages = new Map();
    for (const input of results.flatMap(result => Object.keys(result.metafile.inputs))) {
        const match = /^node_modules\/((?:@[^/]+\/)?[^/]+)\//.exec(input);
        if (!match || packages.has(match[1])) continue;
        packages.set(match[1], await readLicense(match[1]));
    }
    await writeFile('../../src/main/assets/js/spider-editor.LICENSE.txt', [...packages.values()].join('\n\n').replace(/[ \t]+$/gm, ''), 'utf8');
}

async function readLicense(name) {
    const directory = path.join('node_modules', name);
    const metadata = JSON.parse(await readFile(path.join(directory, 'package.json'), 'utf8'));
    const license = await readFile(path.join(directory, 'LICENSE'), 'utf8');
    const notices = await Promise.all((await readdir(directory)).filter(name => /notice/i.test(name)).sort().map(name => readFile(path.join(directory, name), 'utf8')));
    return metadata.name + ' ' + metadata.version + '\n' + [license, ...notices].join('\n\n');
}
