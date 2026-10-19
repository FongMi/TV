import { createHash, createPrivateKey, createPublicKey, sign } from 'node:crypto'
import { readFile, rename, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const [bundleArg, keyArg] = process.argv.slice(2)
if (!bundleArg || !keyArg) {
    console.error('Usage: node sign-bundle.mjs <bundle-directory> <rsa-private-key.pem>')
    process.exit(2)
}

const bundle = path.resolve(bundleArg)
const privateKey = createPrivateKey(await readFile(path.resolve(keyArg)))
if (privateKey.asymmetricKeyType !== 'rsa' || Number(privateKey.asymmetricKeyDetails?.modulusLength || 0) < 2048) {
    throw new Error('The signing key must be RSA with at least 2048 bits')
}

const index = await readFile(path.join(bundle, 'index.js'))
const config = await readFile(path.join(bundle, 'index.config.js'))
const indexSha256 = digest(index, 'sha256')
const configSha256 = digest(config, 'sha256')
const payload = Buffer.from(`catvod-node-bundle-v1\n${indexSha256}\n${configSha256}\n`, 'ascii')
const publicKey = createPublicKey(privateKey).export({ format: 'der', type: 'spki' })
const manifest = {
    version: 1,
    algorithm: 'SHA256withRSA',
    indexSha256,
    configSha256,
    publicKey: publicKey.toString('base64'),
    signature: sign('RSA-SHA256', payload, privateKey).toString('base64'),
}
const target = path.join(bundle, 'index.manifest.json')
await atomicWrite(path.join(bundle, 'index.js.md5'), `${digest(index, 'md5')}\n`)
await atomicWrite(path.join(bundle, 'index.config.js.md5'), `${digest(config, 'md5')}\n`)
await atomicWrite(target, `${JSON.stringify(manifest, null, 2)}\n`)
console.log(`Signed ${target}`)
console.log(`Key SHA-256: ${digest(publicKey, 'sha256')}`)

async function atomicWrite(target, value) {
    const temporary = `${target}.tmp-${process.pid}`
    try {
        await writeFile(temporary, value, { flag: 'wx' })
        await rename(temporary, target)
    } finally {
        await rm(temporary, { force: true })
    }
}

function digest(value, algorithm) {
    return createHash(algorithm).update(value).digest('hex')
}
