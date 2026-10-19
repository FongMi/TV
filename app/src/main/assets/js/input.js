class InputLibrary {
    constructor(storage) {
        this.storage = storage;
        this.key = 'tv.web.inputs';
        this.data = { searches: [], pushes: [] };
        const saved = storage?.getItem(this.key);
        if (!saved) return;
        const data = JSON.parse(saved);
        for (const key of ['searches', 'pushes']) {
            if (!Array.isArray(data[key]) || data[key].some(value => typeof value !== 'string')) throw new Error('Invalid input history');
            this.data[key] = [...new Set(data[key])].slice(0, 10);
        }
    }

    save() {
        if (!this.storage) throw new Error('Storage unavailable');
        this.storage.setItem(this.key, JSON.stringify(this.data));
    }

    remember(type, value) {
        const key = type === 'search' ? 'searches' : 'pushes';
        this.data[key] = [value, ...this.data[key].filter(item => item !== value)].slice(0, 10);
        this.save();
    }

    remove(key, index) {
        this.data[key].splice(index, 1);
        this.save();
    }
}

let inputLibrary;

function renderInputLibrary() {
    for (const [key, id, type] of [['searches', 'search_history', 'search'], ['pushes', 'push_history', 'push']]) renderInputHistory(key, id, type);
}

function renderInputHistory(key, id, type) {
    const list = document.getElementById(id);
    const focused = list.contains(document.activeElement) ? { ...document.activeElement.dataset } : null;
    const rows = document.createDocumentFragment();
    inputLibrary.data[key].forEach((value, index) => rows.append(createInputHistoryRow(key, type, value, index)));
    if (!inputLibrary.data[key].length) {
        const empty = document.createElement('p');
        empty.className = 'panel-hint';
        empty.textContent = t('historyEmpty');
        rows.append(empty);
    }
    list.replaceChildren(rows);
    if (focused) {
        const buttons = Array.from(list.querySelectorAll('button'));
        const next = buttons.find(button => button.dataset.value === focused.value && button.dataset.action === focused.action) || buttons.find(button => button.dataset.action === focused.action) || list.closest('details').querySelector('summary');
        next.focus({ preventScroll: true });
    }
}

function createInputHistoryRow(key, type, value, index) {
    const row = document.createElement('div');
    row.className = 'library-row';
    const use = createInputHistoryUse(type, value);
    const remove = createInputHistoryRemove(key, index, value);
    row.append(use, remove);
    return row;
}

function createInputHistoryUse(type, value) {
    const use = document.createElement('button');
    use.type = 'button';
    use.className = 'library-use';
    use.textContent = value;
    use.title = value;
    use.dataset.value = value;
    use.dataset.action = 'use';
    use.setAttribute('aria-label', t(type) + ': ' + value);
    use.addEventListener('click', () => {
        document.getElementById(type === 'search' ? 'keyword' : 'push_url').value = value;
        if (type === 'search') search();
        else push();
    });
    return use;
}

function createInputHistoryRemove(key, index, value) {
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'md-btn md-btn-outlined md-icon-button';
    remove.innerHTML = '<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true"><use href="icons.svg#close"/></svg>';
    remove.title = t('remove') + ': ' + value;
    remove.dataset.value = value;
    remove.dataset.action = 'remove';
    remove.setAttribute('aria-label', remove.title);
    remove.addEventListener('click', () => removeInputHistory(key, index));
    return remove;
}

function removeInputHistory(key, index) {
    try {
        inputLibrary.remove(key, index);
    } catch (error) {
        warnToast(t('historyUnavailable'));
    }
    renderInputLibrary();
}

function rememberInput(type, value) {
    try {
        inputLibrary.remember(type, value);
    } catch (error) {
        warnToast(t('historyUnavailable'));
    }
    renderInputLibrary();
}

function initInputLibrary() {
    try {
        inputLibrary = new InputLibrary(window.localStorage);
    } catch (error) {
        inputLibrary = new InputLibrary(null);
        document.querySelectorAll('.history-hint').forEach(node => node.textContent = t('historyUnavailable'));
    }
    renderInputLibrary();
}

if (typeof module !== 'undefined' && module.exports) module.exports = { InputLibrary };
else initInputLibrary();
