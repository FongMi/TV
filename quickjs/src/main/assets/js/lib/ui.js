(() => {
    const callbacks = new Map();
    let nextId = 0;
    globalThis.__DIALOG_ACTIONS__ = {
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
    globalThis.dialog = async function(options) {
        if (!options || typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Dialog options must be an object');
        const {onAction, ...fields} = options;
        if (onAction != null && typeof onAction !== 'function') throw new TypeError('Dialog onAction must be a function');
        const id = onAction == null ? null : String(++nextId);
        if (id != null) callbacks.set(id, onAction);
        try {
            return await _dialog(fields, id);
        } finally {
            callbacks.delete(id);
        }
    };
})();
