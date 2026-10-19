let choiceDialog, choiceTitle, choiceList, choiceField;
let choiceClosing = false;

function bindChoice(field, title, options) {
    field.choiceTitle = title;
    field.choiceOptions = options;
    field.classList.add('md-choice');
    field.setAttribute('aria-haspopup', 'dialog');
    field.setAttribute('aria-expanded', 'false');
    field.addEventListener('click', () => openChoice(field));
    updateChoice(field);
}

function updateChoice(field) {
    const label = field.choiceOptions().find(option => String(option.value) === field.value)?.label ?? field.value;
    if (field.textContent !== label) field.textContent = label;
    field.setAttribute('aria-label', field.choiceTitle() + ': ' + label);
    if (choiceField !== field || !choiceDialog?.open) return;
    if (field.disabled) {
        closeChoice();
        return;
    }
    for (const option of choiceList.children) {
        const selected = option.value === field.value;
        option.classList.toggle('active', selected);
        option.setAttribute('aria-selected', String(selected));
    }
}

function createChoiceDialog() {
    choiceDialog = document.createElement('dialog');
    choiceDialog.id = 'choice_dialog';
    choiceDialog.className = 'md-dialog choice-dialog';
    choiceDialog.setAttribute('aria-labelledby', 'choice_title');
    choiceTitle = document.createElement('h2');
    choiceTitle.id = 'choice_title';
    choiceTitle.className = 'md-dialog-title';
    choiceList = document.createElement('div');
    choiceList.className = 'md-dialog-list';
    choiceList.setAttribute('role', 'listbox');
    choiceList.setAttribute('aria-labelledby', 'choice_title');
    const actions = document.createElement('div'), cancel = document.createElement('button');
    actions.className = 'md-dialog-actions';
    cancel.className = 'md-dialog-btn';
    cancel.type = 'button';
    cancel.textContent = t('cancel');
    cancel.addEventListener('click', closeChoice);
    actions.append(cancel);
    choiceDialog.append(choiceTitle, choiceList, actions);
    choiceDialog.addEventListener('cancel', event => {
        event.preventDefault();
        event.stopPropagation();
        closeChoice();
    });
    choiceDialog.addEventListener('close', () => {
        const field = choiceField;
        choiceField = null;
        field?.setAttribute('aria-expanded', 'false');
        if (field?.isConnected && !field.disabled) field.focus({preventScroll: true});
    });
    choiceDialog.addEventListener('click', event => {
        const box = choiceDialog.getBoundingClientRect();
        if (event.target === choiceDialog && (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom)) closeChoice();
    });
    choiceList.addEventListener('keydown', event => {
        const options = [...choiceList.querySelectorAll('button:not([disabled])')];
        const index = options.indexOf(document.activeElement);
        let next;
        if (event.key === 'ArrowDown') next = (index + 1) % options.length;
        else if (event.key === 'ArrowUp') next = (index - 1 + options.length) % options.length;
        else if (event.key === 'Home') next = 0;
        else if (event.key === 'End') next = options.length - 1;
        else return;
        event.preventDefault();
        if (options[next]) {
            for (const option of options) option.tabIndex = -1;
            options[next].tabIndex = 0;
            options[next].focus();
        }
    });
    document.body.append(choiceDialog);
}

function openChoice(field) {
    if (field.disabled || choiceClosing || choiceDialog?.open) return;
    if (!choiceDialog) createChoiceDialog();
    choiceField = field;
    choiceTitle.textContent = field.choiceTitle();
    choiceList.replaceChildren();
    for (const option of field.choiceOptions()) choiceList.append(createChoiceOption(field, option));
    field.setAttribute('aria-expanded', 'true');
    history.pushState({choiceDialog: true}, '');
    choiceDialog.showModal();
    updateChoice(field);
    const selected = choiceList.querySelector('.active:not([disabled])') || choiceList.querySelector('button:not([disabled])');
    if (selected) {
        selected.tabIndex = 0;
        selected.focus({preventScroll: true});
        selected.scrollIntoView({block: 'nearest'});
    }
}

function createChoiceOption(field, option) {
    const button = document.createElement('button');
    button.type = 'button';
    button.value = String(option.value);
    button.textContent = option.label;
    button.className = 'md-dialog-list-item';
    button.disabled = !!option.disabled;
    button.tabIndex = -1;
    button.setAttribute('role', 'option');
    button.addEventListener('click', () => selectChoiceOption(field, button));
    return button;
}

function selectChoiceOption(field, button) {
    if (field.disabled) {
        closeChoice();
        return;
    }
    const changed = field.value !== button.value;
    field.value = button.value;
    closeChoice();
    updateChoice(field);
    if (changed) {
        field.dispatchEvent(new Event('input', {bubbles: true}));
        field.dispatchEvent(new Event('change', {bubbles: true}));
    }
}

function closeChoice() {
    if (!choiceDialog?.open) return choiceClosing;
    if (history.state?.choiceDialog) {
        choiceClosing = true;
        history.back();
    }
    choiceDialog.close();
    return true;
}

if (typeof window !== 'undefined') window.addEventListener('popstate', event => {
    if (!choiceDialog?.open && !choiceClosing) return;
    event.stopImmediatePropagation();
    choiceClosing = false;
    if (choiceDialog.open) choiceDialog.close();
});

if (typeof module !== 'undefined' && module.exports) module.exports = {bindChoice, updateChoice, closeChoice};
