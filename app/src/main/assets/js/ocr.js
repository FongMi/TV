const OCR_MAX_FILE_BYTES = 32 * 1024 * 1024;
const OCR_FILE_TYPES = new Map([['png', 'image/png'], ['jpeg', 'image/jpeg'], ['jpg', 'image/jpeg'], ['bmp', 'image/bmp']]);
const OCR_IMAGE_TYPES = new Set(OCR_FILE_TYPES.values());
const OCR_ERRORS = new Map([
    ['invalid_image', 'ocrInvalidImage'], ['unsupported_image_type', 'ocrInvalidImage'],
    ['image_too_large', 'ocrTooLarge'], ['ocr_busy', 'ocrBusy'],
    ['ocr_restart_required', 'ocrRestart'], ['loopback_required', 'ocrLocalOnly']
]);

async function getOcrAvailability(signal) {
    try {
        const response = await fetch('/ocr/status', {cache: 'no-store', signal});
        if (response.status === 403) return 'ocrLocalOnly';
        if (!response.ok) return 'ocrUnavailable';
        const {installed} = await response.json();
        if (typeof installed === 'boolean') return installed ? '' : 'ocrMissing';
        return 'ocrUnavailable';
    } catch {
        return 'ocrUnavailable';
    }
}

function watchOcrAvailability(report, panel) {
    let pending = null;

    function cancel() {
        pending?.abort();
        pending = null;
    }

    async function refresh() {
        if (pending || document.hidden || (panel && !panel.classList.contains('active'))) return;
        const controller = new AbortController();
        pending = controller;
        const timeout = setTimeout(() => controller.abort(), 5000);
        const error = await getOcrAvailability(controller.signal);
        clearTimeout(timeout);
        if (pending !== controller) return;
        pending = null;
        report(error);
    }

    if (panel) document.addEventListener('panelchange', refresh);
    document.addEventListener('visibilitychange', refresh);
    window.addEventListener('pagehide', cancel);
    window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
    window.addEventListener('online', refresh);
    refresh();
    return cancel;
}

function initOcr() {
    const availability = document.getElementById('ocr_availability');
    const workspace = document.getElementById('ocr_workspace'), file = document.getElementById('ocr_file');
    const choose = document.getElementById('ocr_choose'), filename = document.getElementById('ocr_filename');
    const preview = document.getElementById('ocr_preview');
    const result = document.getElementById('ocr_result'), text = document.getElementById('ocr_text');
    const copy = document.getElementById('ocr_copy'), status = document.getElementById('ocr_status');
    const formats = filename.textContent;
    let request = null, previewUrl = '';

    function showStatus(key, error = false) {
        status.textContent = key ? t(key) : '';
        status.dataset.error = String(error);
        status.hidden = !key;
    }

    function clearResult() {
        text.value = '';
        result.hidden = true;
        showStatus();
    }

    function imageType(image) {
        if (OCR_IMAGE_TYPES.has(image.type)) return image.type;
        return OCR_FILE_TYPES.get(image.name.split('.').pop().toLowerCase());
    }

    function imageError(image, type) {
        if (!image.size || !type) return 'ocrInvalidImage';
        if (image.size > OCR_MAX_FILE_BYTES) return 'ocrTooLarge';
    }

    function clearPreview() {
        if (!previewUrl) return;
        URL.revokeObjectURL(previewUrl);
        previewUrl = '';
        preview.hidden = true;
        preview.removeAttribute('src');
    }

    function previewImage(image, type) {
        const blob = image.type === type ? image : image.slice(0, image.size, type);
        previewUrl = URL.createObjectURL(blob);
        preview.src = previewUrl;
    }

    function selectImage() {
        if (workspace.hidden || request) return;
        const image = file.files[0];
        if (!image) return;
        filename.textContent = image.name;
        clearPreview();
        clearResult();
        const type = imageType(image), error = imageError(image, type);
        if (error) {
            showStatus(error, true);
            return;
        }
        previewImage(image, type);
        return recognize(image, type);
    }

    function setRecognitionRequest(controller) {
        request = controller;
        choose.setAttribute('aria-disabled', String(!!controller));
    }

    function resetSelection() {
        request?.abort();
        setRecognitionRequest(null);
        file.value = '';
        filename.textContent = formats;
        clearPreview();
        clearResult();
    }

    function setAvailability(error) {
        if (error === 'ocrUnavailable' && !workspace.hidden) return;
        availability.textContent = error ? t(error) : '';
        availability.hidden = !error;
        workspace.hidden = !!error;
        if (error) resetSelection();
    }

    function showRecognition(ok, body) {
        if (ok && typeof body.text === 'string') {
            text.value = body.text;
            result.hidden = !body.text;
            showStatus(body.text ? 'ocrDone' : 'ocrEmpty');
        } else if (body.error === 'ocr_module_missing') {
            cancelAvailability();
            setAvailability('ocrMissing');
        } else {
            showStatus(OCR_ERRORS.get(body.error) || 'ocrFailed', true);
        }
    }

    async function recognize(image, type) {
        const controller = new AbortController();
        setRecognitionRequest(controller);
        showStatus('ocrRunning');
        const timeout = setTimeout(() => controller.abort(), 40000);
        try {
            const response = await fetch('/ocr?model=auto', {
                method: 'POST', headers: {'Content-Type': type}, body: image, signal: controller.signal
            });
            const body = await response.json();
            if (request === controller) showRecognition(response.ok, body);
        } catch {
            if (request === controller) showStatus('ocrFailed', true);
        } finally {
            clearTimeout(timeout);
            if (request === controller) setRecognitionRequest(null);
        }
    }

    async function copyResult() {
        const value = text.value;
        try {
            await navigator.clipboard.writeText(value);
            if (!result.hidden && value === text.value) showStatus('ocrCopied');
        } catch {
            if (result.hidden || value !== text.value) return;
            text.focus();
            text.select();
            showStatus('ocrCopyFailed', true);
        }
    }

    function openPicker() {
        if (request) return;
        file.value = '';
        file.click();
    }

    function leavePage(event) {
        if (request) resetSelection();
        else if (!event.persisted) clearPreview();
    }

    choose.addEventListener('click', openPicker);
    file.addEventListener('change', selectImage);
    preview.alt = t('ocrPreview');
    preview.addEventListener('load', () => { preview.hidden = !preview.naturalWidth; });
    preview.addEventListener('error', clearPreview);
    copy.addEventListener('click', copyResult);
    bindWebReturn(document.getElementById('ocr_return'), 'settings');
    const cancelAvailability = watchOcrAvailability(setAvailability);
    window.addEventListener('pagehide', leavePage);
}

function updateOcrEntry(error) {
    if (error !== 'ocrUnavailable') ocrEntry.hidden = !!error;
}

const ocrEntry = document.getElementById('ocr_entry');
if (ocrEntry) watchOcrAvailability(updateOcrEntry, ocrEntry.closest('.tab-panel'));
else initOcr();
