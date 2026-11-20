// Rich text editor for the admin panel. Mirrors the app's RichTextEditText + RichTextToolbar:
// the RichText model is the source of truth, the contenteditable shows raw ruby markup, and
// rendering with furigana only happens in the preview.
//
// Native editing (incl. IME composition) is left to the browser; after each input the DOM is read
// back into the model and re-rendered canonically. Toolbar actions operate on the model directly.
import * as RT from './richtext.js';

// ---------------------------------------------------------------- DOM <-> model

const TAG_FORMATS = {
    B: { type: 'bold' }, STRONG: { type: 'bold' },
    I: { type: 'italic' }, EM: { type: 'italic' },
    U: { type: 'underline' },
    S: { type: 'strikethrough' }, STRIKE: { type: 'strikethrough' }, DEL: { type: 'strikethrough' },
};

function elementFormats(el, inherited) {
    const formats = { ...inherited };
    const tagFormat = TAG_FORMATS[el.tagName];
    if (tagFormat) formats[tagFormat.type] = tagFormat;
    const d = el.dataset;
    for (const kind of ['bold', 'italic', 'underline', 'strikethrough']) if (kind in d) formats[kind] = { type: kind };
    if (d.color) formats.color = { type: 'color', argb: Number(d.color) };
    if (d.highlight) formats.highlight = { type: 'highlight', argb: Number(d.highlight) };
    if (d.size) formats.size = { type: 'size', proportion: Number(d.size) };
    return formats;
}

/** Flattens the editor DOM into pieces {node, start, text, formats} in document order. */
function readPieces(root) {
    const pieces = [];
    let length = 0;
    const push = (node, text, formats) => {
        pieces.push({ node, start: length, text, formats });
        length += text.length;
    };
    const walk = (node, formats) => {
        for (const child of node.childNodes) {
            if (child.nodeType === Node.TEXT_NODE) {
                if (child.nodeValue) push(child, child.nodeValue.replaceAll(' ', ' '), formats);
            } else if (child.nodeType === Node.ELEMENT_NODE) {
                if (child.tagName === 'BR') {
                    if (!('filler' in child.dataset)) push(child, '\n', formats);
                    continue;
                }
                // Blocks only appear if the browser created them despite our Enter handling.
                if ((child.tagName === 'DIV' || child.tagName === 'P') && length > 0 && !pieces.at(-1).text.endsWith('\n')) {
                    push(child, '\n', formats);
                }
                walk(child, elementFormats(child, formats));
            }
        }
    };
    walk(root, {});
    // Browsers (Firefox) pad an empty last line with a <br>; like our own filler it is not text.
    const last = pieces.at(-1);
    if (last?.node.nodeName === 'BR' && (pieces.length === 1 || pieces.at(-2).text.endsWith('\n'))) {
        pieces.pop();
        length -= 1;
    }
    return { pieces, length };
}

function piecesToRichText(pieces) {
    const text = pieces.map(p => p.text).join('');
    let rt = { text, spans: [] };
    for (const p of pieces) {
        for (const format of Object.values(p.formats)) rt = RT.applyFormat(rt, format, p.start, p.start + p.text.length);
    }
    return rt;
}

function renderEditorHtml(rt) {
    let html = RT.formatRuns(rt)
        .map(run => {
            const text = RT.escapeHtml(rt.text.slice(run.start, run.end));
            const f = run.formats;
            if (Object.keys(f).length === 0) return text;
            const attrs = ['bold', 'italic', 'underline', 'strikethrough'].filter(k => f[k]).map(k => ` data-${k}`);
            if (f.color) attrs.push(` data-color="${f.color.argb}"`);
            if (f.highlight) attrs.push(` data-highlight="${f.highlight.argb}"`);
            if (f.size) attrs.push(` data-size="${f.size.proportion}"`);
            return `<span${attrs.join('')} style="${RT.formatsToCss(f)}">${text}</span>`;
        })
        .join('');
    // A trailing newline is only visible with something after it.
    if (rt.text.endsWith('\n')) html += '<br data-filler>';
    return html;
}

function domPointToOffset(root, pieces, total, node, offset) {
    if (node.nodeType === Node.TEXT_NODE) {
        const piece = pieces.find(p => p.node === node);
        if (piece) return piece.start + Math.min(offset, piece.text.length);
    }
    // Element point (or an untracked empty text node): position before the given child.
    const target = node.nodeType === Node.ELEMENT_NODE ? node.childNodes[offset] : node;
    if (target) {
        for (const p of pieces) {
            if (p.node === target || target.contains(p.node)
                || (target.compareDocumentPosition(p.node) & Node.DOCUMENT_POSITION_FOLLOWING)) {
                return p.start;
            }
        }
        return total;
    }
    for (const p of pieces) {
        if (!node.contains(p.node) && (node.compareDocumentPosition(p.node) & Node.DOCUMENT_POSITION_FOLLOWING)) {
            return p.start;
        }
    }
    return total;
}

function offsetToDomPoint(root, pieces, offset) {
    // Prefer the end of the preceding text node, so typing inherits the preceding formats.
    for (const p of pieces) {
        const end = p.start + p.text.length;
        if (p.node.nodeType === Node.TEXT_NODE && p.start < offset && offset <= end) return [p.node, offset - p.start];
    }
    for (const p of pieces) {
        if (p.start < offset) continue;
        if (p.node.nodeType === Node.TEXT_NODE) return [p.node, 0];
        return [p.node.parentNode, [...p.node.parentNode.childNodes].indexOf(p.node)];
    }
    const last = pieces.at(-1);
    if (last && last.node.tagName === 'BR') return [last.node.parentNode, [...last.node.parentNode.childNodes].indexOf(last.node) + 1];
    return [root, root.childNodes.length];
}

// ---------------------------------------------------------------- editor

export class RichTextEditor {
    constructor(root, initial) {
        this.root = root;
        this.rt = initial;
        this.listeners = [];
        this.typing = null;       // {offset, formats}: toolbar choices for text typed at a collapsed cursor
        this.undoStack = [];
        this.redoStack = [];
        this.lastPush = { at: 0, kind: null };
        this.composing = false;

        root.contentEditable = 'true';
        root.spellcheck = true;
        this.render(null);

        root.addEventListener('compositionstart', () => { this.composing = true; });
        root.addEventListener('compositionend', () => { this.composing = false; this.sync(); });
        root.addEventListener('input', e => { if (!e.isComposing && !this.composing) this.sync(e.inputType); });
        root.addEventListener('beforeinput', e => {
            if (e.inputType === 'historyUndo') { e.preventDefault(); this.undo(); }
            if (e.inputType === 'historyRedo') { e.preventDefault(); this.redo(); }
            if (e.inputType.startsWith('format')) e.preventDefault();
        });
        root.addEventListener('keydown', e => this.onKeyDown(e));
        // Like the app: paste is always plain text.
        root.addEventListener('paste', e => {
            e.preventDefault();
            const text = (e.clipboardData?.getData('text/plain') ?? '').replace(/\r\n?/g, '\n');
            if (text) document.execCommand('insertText', false, text);
        });
        root.addEventListener('drop', e => e.preventDefault());
        document.addEventListener('selectionchange', () => {
            if (!this.hasFocus()) return;
            const sel = this.selection();
            if (this.typing && (sel.start !== sel.end || sel.start !== this.typing.offset)) this.typing = null;
            this.lastSelection = sel;
            this.emit();
        });
    }

    onChange(listener) {
        this.listeners.push(listener);
    }

    emit() {
        for (const l of this.listeners) l(this);
    }

    hasFocus() {
        return document.activeElement === this.root;
    }

    onKeyDown(e) {
        const mod = e.ctrlKey || e.metaKey;
        if (e.key === 'Enter' && !e.isComposing) {
            e.preventDefault();
            document.execCommand('insertText', false, '\n');
        } else if (mod && !e.altKey && e.key.toLowerCase() === 'z') {
            e.preventDefault();
            e.shiftKey ? this.redo() : this.undo();
        } else if (mod && !e.altKey && e.key.toLowerCase() === 'y') {
            e.preventDefault();
            this.redo();
        } else if (mod && !e.altKey && !e.shiftKey && ['b', 'i', 'u'].includes(e.key.toLowerCase())) {
            e.preventDefault();
            this.toggle({ b: 'bold', i: 'italic', u: 'underline' }[e.key.toLowerCase()]);
        }
    }

    // ---- selection

    selection() {
        const sel = document.getSelection();
        const fallback = this.savedSelection ?? { start: this.rt.text.length, end: this.rt.text.length };
        if (!sel || sel.rangeCount === 0 || !this.root.contains(sel.anchorNode)) return fallback;
        const { pieces, length } = readPieces(this.root);
        const a = domPointToOffset(this.root, pieces, length, sel.anchorNode, sel.anchorOffset);
        const b = domPointToOffset(this.root, pieces, length, sel.focusNode, sel.focusOffset);
        return { start: Math.min(a, b), end: Math.max(a, b) };
    }

    setSelection(start, end = start) {
        this.savedSelection = { start, end };
        if (!this.hasFocus()) return;
        const { pieces } = readPieces(this.root);
        const range = document.createRange();
        range.setStart(...offsetToDomPoint(this.root, pieces, start));
        range.setEnd(...offsetToDomPoint(this.root, pieces, end));
        const sel = document.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    }

    /** Remembers the selection before focus moves elsewhere (e.g. into the furigana dialog). */
    rememberSelection() {
        this.savedSelection = this.selection();
        return this.savedSelection;
    }

    render(selection) {
        const html = renderEditorHtml(this.rt);
        if (this.root.innerHTML !== html) this.root.innerHTML = html;
        if (selection) this.setSelection(selection.start, selection.end);
    }

    // ---- history

    pushHistory(kind) {
        const now = Date.now();
        // Coalesce consecutive typing into one undo step.
        if (kind === 'typing' && this.lastPush.kind === 'typing' && now - this.lastPush.at < 1000) {
            this.lastPush.at = now;
            return;
        }
        this.undoStack.push({ rt: this.rt, selection: this.lastSelection ?? this.selection() });
        if (this.undoStack.length > 200) this.undoStack.shift();
        this.redoStack = [];
        this.lastPush = { at: now, kind };
    }

    undo() {
        const entry = this.undoStack.pop();
        if (!entry) return;
        this.redoStack.push({ rt: this.rt, selection: this.selection() });
        this.restore(entry);
    }

    redo() {
        const entry = this.redoStack.pop();
        if (!entry) return;
        this.undoStack.push({ rt: this.rt, selection: this.selection() });
        this.restore(entry);
    }

    restore(entry) {
        this.rt = entry.rt;
        this.typing = null;
        this.lastPush = { at: 0, kind: null };
        this.render(entry.selection);
        this.emit();
    }

    // ---- model updates

    /** Reads native edits back from the DOM. */
    sync(inputType = 'insertText') {
        const { pieces, length } = readPieces(this.root);
        const sel = this.selection();
        let rt = piecesToRichText(pieces);
        const inserted = length - this.rt.text.length;
        if (this.typing && inserted > 0 && sel.start === sel.end && sel.start - inserted === this.typing.offset) {
            for (const [kind, format] of Object.entries(this.typing.formats)) {
                rt = RT.setKind(rt, kind, format, this.typing.offset, sel.start);
            }
        }
        this.typing = null;
        if (!RT.richTextEquals(rt, this.rt)) {
            this.pushHistory(inputType.startsWith('insert') ? 'typing' : inputType);
            this.rt = rt;
        }
        this.render(sel);
        this.lastSelection = sel;
        this.emit();
    }

    /** Applies a model transformation, keeping undo history and the DOM in sync. */
    update(fn, selection) {
        const before = this.selection();
        this.lastSelection = before;
        const rt = fn(this.rt, before);
        if (RT.richTextEquals(rt, this.rt)) return;
        this.pushHistory('format');
        this.rt = rt;
        this.render(selection ?? before);
        this.lastSelection = selection ?? before;
        this.emit();
    }

    /** Active formats for the toolbar: typing formats at a collapsed cursor, else the covering ones. */
    activeFormats() {
        const { start, end } = this.selection();
        if (start === end && this.typing?.offset === start) {
            const formats = { ...RT.formatsAt(this.rt, start) };
            for (const [kind, format] of Object.entries(this.typing.formats)) {
                if (format) formats[kind] = format; else delete formats[kind];
            }
            return formats;
        }
        return RT.formatsIn(this.rt, start, end);
    }

    setFormat(kind, format) {
        const { start, end } = this.selection();
        if (start === end) {
            const formats = this.typing?.offset === start ? this.typing.formats : {};
            this.typing = { offset: start, formats: { ...formats, [kind]: format } };
            this.emit();
            return;
        }
        this.update(rt => RT.setKind(rt, kind, format, start, end));
    }

    toggle(kind) {
        this.setFormat(kind, this.activeFormats()[kind] ? null : { type: kind });
    }

    clearFormatting() {
        const { start, end } = this.selection();
        if (start === end) {
            this.typing = { offset: start, formats: Object.fromEntries(RT.KINDS.map(k => [k, null])) };
            this.emit();
            return;
        }
        this.update(rt => RT.KINDS.reduce((acc, kind) => RT.removeKind(acc, kind, start, end), rt));
    }

    selectedText() {
        const { start, end } = this.selection();
        return this.rt.text.slice(start, end);
    }

    /** Same behavior as RichTextEditText.insertRuby. */
    insertRuby(base, reading, { start, end } = this.savedSelection ?? this.selection()) {
        const markup = RT.formatRuby(base, reading);
        this.root.focus();
        this.update(rt => {
            if (start !== end && rt.text.slice(start, end) === base) {
                const closed = RT.replaceText(rt, end, end, `|${reading}}`);
                return RT.replaceText(closed, start, start, '{');
            }
            return RT.replaceText(rt, start, end, markup);
        }, { start: start + markup.length, end: start + markup.length });
    }
}
