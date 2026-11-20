import * as RT from './richtext.js';
import { RichTextEditor } from './editor.js';

const $ = (selector, root = document) => root.querySelector(selector);

// ---------------------------------------------------------------- furigana dialog (FuriganaDialog.kt)

const dialog = $('#furigana-dialog');

function askFurigana(initialBase) {
    const base = $('[name=base]', dialog);
    const reading = $('[name=reading]', dialog);
    const add = $('[data-action=add]', dialog);
    const preview = $('.furigana-preview', dialog);
    base.value = initialBase;
    reading.value = '';

    const refresh = () => {
        const valid = RT.isValidRubyPart(base.value) && RT.isValidRubyPart(reading.value);
        add.disabled = !valid;
        base.classList.toggle('invalid', base.value !== '' && !RT.isValidRubyPart(base.value));
        reading.classList.toggle('invalid', reading.value !== '' && !RT.isValidRubyPart(reading.value));
        preview.innerHTML = valid ? RT.rubyHtml(RT.formatRuby(base.value, reading.value.trim())) : '';
        preview.hidden = !valid;
    };
    base.oninput = reading.oninput = refresh;
    refresh();

    return new Promise(resolve => {
        dialog.onclose = () => {
            const ok = dialog.returnValue === 'add' && RT.isValidRubyPart(base.value) && RT.isValidRubyPart(reading.value);
            resolve(ok ? { base: base.value, reading: reading.value.trim() } : null);
        };
        dialog.returnValue = '';
        dialog.showModal();
        (base.value === '' ? base : reading).focus();
    });
}

if (dialog) {
    $('form', dialog).addEventListener('submit', e => {
        e.preventDefault();
        if (!$('[data-action=add]', dialog).disabled) dialog.close('add');
    });
    $('[data-action=cancel]', dialog).addEventListener('click', () => dialog.close('cancel'));
}

// Plain String fields with ruby markup (lecture name): replace the selection with the markup.
for (const button of document.querySelectorAll('[data-furigana-for]')) {
    const input = document.getElementById(button.dataset.furiganaFor);
    button.addEventListener('click', async () => {
        const start = input.selectionStart;
        const end = input.selectionEnd;
        const result = await askFurigana(input.value.slice(start, end));
        input.focus();
        if (!result) return;
        const markup = RT.formatRuby(result.base, result.reading);
        input.setRangeText(markup, start, end, 'end');
        input.dispatchEvent(new Event('input', { bubbles: true }));
    });
}

// ---------------------------------------------------------------- rich text field

const editorRoot = $('#rich-editor');
if (editorRoot) {
    const hidden = document.getElementById(editorRoot.dataset.input);
    const editor = new RichTextEditor(editorRoot, RT.decode(hidden.value));
    const toolbar = $('#rich-toolbar');
    const panel = $('.rte-panel', toolbar);
    const preview = $('#rich-preview');
    const counter = $('#rich-counter');
    const maxBytes = Number(editorRoot.dataset.maxBytes);
    let openPanel = null;

    const button = (label, title, onClick, extraClass = '') => {
        const b = document.createElement('button');
        b.type = 'button';
        b.className = `rte-btn ${extraClass}`;
        b.innerHTML = label;
        b.title = title;
        b.setAttribute('aria-label', title);
        // Keep focus (and the selection) in the editor.
        b.addEventListener('mousedown', e => e.preventDefault());
        b.addEventListener('click', onClick);
        return b;
    };

    const main = $('.rte-main', toolbar);
    const toggles = {
        bold: button('<b>B</b>', 'Bold (Ctrl+B)', () => editor.toggle('bold')),
        italic: button('<i>I</i>', 'Italic (Ctrl+I)', () => editor.toggle('italic')),
        underline: button('<u>U</u>', 'Underline (Ctrl+U)', () => editor.toggle('underline')),
        strikethrough: button('<s>S</s>', 'Strikethrough', () => editor.toggle('strikethrough')),
    };
    const panels = {
        color: button('A<span class="rte-indicator"></span>', 'Text color', () => togglePanel('color')),
        highlight: button('<span class="rte-hl">A</span><span class="rte-indicator"></span>', 'Highlight', () => togglePanel('highlight')),
        size: button('<span style="font-size:.8em">A</span>A', 'Text size', () => togglePanel('size')),
    };
    const sep = () => Object.assign(document.createElement('span'), { className: 'rte-sep' });
    main.append(
        ...Object.values(toggles), sep(), ...Object.values(panels), sep(),
        button('ふ', 'Add furigana', async () => {
            const selection = editor.rememberSelection();
            const result = await askFurigana(editor.rt.text.slice(selection.start, selection.end));
            if (result) editor.insertRuby(result.base, result.reading, selection);
            else editorRoot.focus();
        }, 'rte-furigana'),
        button('T<sub>×</sub>', 'Clear formatting', () => editor.clearFormatting()),
    );

    function togglePanel(name) {
        openPanel = openPanel === name ? null : name;
        refresh();
    }

    function renderPanel(active) {
        panel.replaceChildren();
        panel.hidden = !openPanel;
        if (!openPanel) return;
        if (openPanel === 'size') {
            for (const [label, proportion] of RT.SIZE_OPTIONS) {
                const selected = (active.size?.proportion ?? null) === proportion;
                panel.append(button(label, `${label} text`, () => {
                    editor.setFormat('size', proportion === null ? null : { type: 'size', proportion });
                }, `rte-chip${selected ? ' selected' : ''}`));
            }
            return;
        }
        const kind = openPanel;
        const colors = kind === 'color' ? RT.TEXT_COLORS : RT.HIGHLIGHT_COLORS;
        panel.append(button('⌀', 'No color', () => editor.setFormat(kind, null), 'rte-swatch rte-none'));
        for (const argb of colors) {
            const hex = (argb >>> 0).toString(16).toUpperCase().padStart(8, '0');
            const swatch = button('', `Color #${hex}`, () => editor.setFormat(kind, { type: kind, argb }),
                `rte-swatch${active[kind]?.argb === argb ? ' selected' : ''}`);
            swatch.style.setProperty('--swatch', RT.argbToCss(argb));
            panel.append(swatch);
        }
    }

    function refresh() {
        const active = editor.activeFormats();
        for (const [kind, b] of Object.entries(toggles)) b.classList.toggle('active', !!active[kind]);
        for (const [kind, b] of Object.entries(panels)) {
            b.classList.toggle('active', openPanel === kind);
            const indicator = $('.rte-indicator', b);
            if (indicator) indicator.style.background = active[kind] ? RT.argbToCss(active[kind].argb) : '';
        }
        renderPanel(active);

        const encoded = RT.encode(editor.rt);
        hidden.value = encoded;
        const bytes = new TextEncoder().encode(encoded).length;
        counter.textContent = `${bytes.toLocaleString()} / ${maxBytes.toLocaleString()} bytes stored`;
        counter.classList.toggle('over', bytes > maxBytes);
        if (!preview.hidden) preview.innerHTML = RT.toHtml(editor.rt);
    }

    editor.onChange(refresh);
    refresh();

    const previewToggle = $('#rich-preview-toggle');
    previewToggle.addEventListener('click', () => {
        const showPreview = preview.hidden;
        preview.hidden = !showPreview;
        editorRoot.hidden = showPreview;
        toolbar.hidden = showPreview;
        previewToggle.textContent = showPreview ? 'Edit' : 'Preview';
        previewToggle.setAttribute('aria-pressed', String(showPreview));
        refresh();
    });

    $('#lecture-form').addEventListener('submit', () => {
        hidden.value = RT.encode(editor.rt);
    });
}

// ---------------------------------------------------------------- discard confirmation

const dirtyForm = $('form[data-confirm-discard]');
if (dirtyForm) {
    const snapshot = () => new URLSearchParams(new FormData(dirtyForm)).toString();
    const initial = snapshot();
    let submitting = false;
    dirtyForm.addEventListener('submit', () => { submitting = true; });
    window.addEventListener('beforeunload', e => {
        if (!submitting && snapshot() !== initial) e.preventDefault();
    });
}
