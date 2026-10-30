import host from 'catvod:host';

const callbacks = new Map();
let nextId = 0;

export const dialogActions = {
    async action(id, event) {
        const callback = callbacks.get(id);
        if (!callback) throw new Error('Dialog is closed');
        const update = await callback(event);
        if (update == null) return '{}';
        if (typeof update !== 'object' || Array.isArray(update)) throw new TypeError('Dialog action result must be an object');
        return JSON.stringify(update);
    },
    release(id) {
        callbacks.delete(id);
    }
};

export async function dialog(options, callback = null) {
    if (!options || typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Dialog options must be an object');
    if (callback != null && typeof callback !== 'function') throw new TypeError('Dialog callback must be a function');
    const id = callback == null ? null : String(++nextId);
    if (id != null) callbacks.set(id, callback);
    try {
        return await host.dialog(options, id);
    } finally {
        callbacks.delete(id);
    }
}
