'use strict'

const http = require('http')
const path = require('path')

const [, , indexPath, configPath, dataRoot, bridgePortText, token] = process.argv
const bridgePort = Number(bridgePortText)
const bridgeTokenHeader = 'X-CatVod-Token'
let runtime = null
let server = null

process.env.CATVOD_DISABLE_AUTOSTART = '1'
process.env.HOST = '127.0.0.1'
process.env.PORT = '0'
process.env.HOME = dataRoot
process.chdir(dataRoot)

const originalHttpRequest = http.request

function isBridgeRequest(input, options = {}) {
    let host
    let port
    let requestPath
    let method = options.method
    if (typeof input === 'string' || input instanceof URL) {
        const target = new URL(input)
        host = target.hostname
        port = target.port || (target.protocol === 'https:' ? '443' : '80')
        requestPath = target.pathname
    } else if (input && typeof input === 'object') {
        host = input.hostname || input.host
        port = input.port
        requestPath = input.path || input.pathname
        method = input.method
    }
    return String(method || 'GET').toUpperCase() === 'POST'
        && (host === '127.0.0.1' || host === 'localhost')
        && Number(port) === bridgePort
        && String(requestPath || '').split('?', 1)[0] === '/msg'
}

http.request = function authenticatedBridgeRequest(...args) {
    const input = args[0]
    const options = args[1] && typeof args[1] === 'object' ? args[1] : {}
    if (!isBridgeRequest(input, options)) return originalHttpRequest.apply(this, args)
    if (typeof input === 'string' || input instanceof URL) {
        const secured = { ...options, headers: { ...(options.headers || {}), [bridgeTokenHeader]: token } }
        if (args[1] && typeof args[1] === 'object') args[1] = secured
        else args.splice(1, 0, secured)
    } else {
        args[0] = { ...input, headers: { ...(input.headers || {}), [bridgeTokenHeader]: token } }
    }
    return originalHttpRequest.apply(this, args)
}

function send(action, opt = {}) {
    const body = JSON.stringify({ action, opt })
    const request = http.request({
        host: '127.0.0.1',
        port: bridgePort,
        path: '/msg',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(body),
            [bridgeTokenHeader]: token,
        },
        timeout: 3000,
    })
    request.on('error', () => {})
    request.on('timeout', () => request.destroy())
    request.end(body)
}

function reportStarted() {
    const address = server && server.address()
    if (!address || typeof address !== 'object') return
    send('serverStarted', {
        address: `http://127.0.0.1:${address.port}`,
        token,
        pid: process.pid,
        version: process.version,
        arch: process.arch,
    })
}

globalThis.catDartServerPort = () => bridgePort
globalThis.catServerFactory = handler => {
    server = http.createServer(handler)
    const listen = server.listen.bind(server)
    server.listen = (...args) => {
        const callback = typeof args[args.length - 1] === 'function' ? args[args.length - 1] : undefined
        return listen({ host: '127.0.0.1', port: 0, exclusive: true }, callback)
    }
    server.once('listening', reportStarted)
    return server
}

async function main() {
    const configModule = require(path.resolve(configPath))
    const indexModule = require(path.resolve(indexPath))
    const config = configModule && (configModule.default || configModule)
    runtime = indexModule && (indexModule.default || indexModule)
    if (!runtime || typeof runtime.start !== 'function') throw new Error('Node bundle does not export start()')
    await runtime.start(config)
}

main().catch(error => {
    send('nodeError', { message: String(error && (error.stack || error.message) || error), token })
    setTimeout(() => process.exit(1), 100)
})
