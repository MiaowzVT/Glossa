// RichText + ruby markup, mirroring the app's `richtext/RichText.kt`, `richtext/SpanOps.kt` and
// `ruby/RubyMarkup.kt`. Used by the admin server (rendering) and the browser editor, so it must
// stay free of Node and DOM APIs.
//
// Stored format (must stay byte-compatible with kotlinx.serialization in the app):
//   {"text":"{雨|あめ}が降る","spans":[{"start":0,"end":6,"format":{"type":"bold"}}]}
// Offsets are UTF-16 code units into `text`, including ruby markup characters.

export const KINDS = ['bold', 'italic', 'underline', 'strikethrough', 'color', 'highlight', 'size'];

const VALUE_KEY = { color: 'argb', highlight: 'argb', size: 'proportion' };

// Same values as TextColorPalette / HighlightPalette in RichTextToolbar.kt (packed signed ARGB ints).
export const TEXT_COLORS = [
    0xFFE53935, 0xFFFB8C00, 0xFF43A047, 0xFF00897B,
    0xFF1E88E5, 0xFF8E24AA, 0xFFD81B60, 0xFF757575,
].map(c => c | 0);

export const HIGHLIGHT_COLORS = [
    0x80FFEB3B, 0x80FFA726, 0x8066BB6A, 0x8042A5F5, 0x80AB47BC, 0x80F06292,
].map(c => c | 0);

export const SIZE_OPTIONS = [['Small', 0.8], ['Normal', null], ['Large', 1.3], ['Huge', 1.6]];

export const EMPTY = Object.freeze({ text: '', spans: [] });

// ---------------------------------------------------------------- codec

export function encode(rt) {
    // kotlinx.serialization omits default values (encodeDefaults = false).
    const out = {};
    if (rt.text !== '') out.text = rt.text;
    if (rt.spans.length > 0) {
        out.spans = rt.spans.map(s => ({ start: s.start, end: s.end, format: encodeFormat(s.format) }));
    }
    return JSON.stringify(out);
}

function encodeFormat(format) {
    const key = VALUE_KEY[format.type];
    return key ? { type: format.type, [key]: format[key] } : { type: format.type };
}

/** Forgiving like RichText.decode: invalid data becomes plain text, out-of-range spans are dropped. */
export function decode(encoded) {
    if (encoded == null || encoded === '') return EMPTY;
    let rt;
    try {
        rt = parseStrict(JSON.parse(encoded));
    } catch {
        rt = null;
    }
    rt ??= { text: String(encoded), spans: [] };
    return { text: rt.text, spans: rt.spans.filter(s => s.start >= 0 && s.start < s.end && s.end <= rt.text.length) };
}

function parseStrict(json) {
    if (json === null || typeof json !== 'object' || Array.isArray(json)) return null;
    const text = json.text ?? '';
    const spans = json.spans ?? [];
    if (typeof text !== 'string' || !Array.isArray(spans)) return null;
    const parsed = [];
    for (const s of spans) {
        if (!s || !Number.isInteger(s.start) || !Number.isInteger(s.end)) return null;
        const format = parseFormat(s.format);
        if (!format) return null;
        parsed.push({ start: s.start, end: s.end, format });
    }
    return { text, spans: parsed };
}

function parseFormat(f) {
    if (!f || !KINDS.includes(f.type)) return null;
    const key = VALUE_KEY[f.type];
    if (!key) return { type: f.type };
    const value = f[key];
    if (key === 'argb' ? !Number.isInteger(value) || value !== (value | 0) : !Number.isFinite(value)) return null;
    return { type: f.type, [key]: value };
}

// ---------------------------------------------------------------- span operations (SpanOps.kt)

const sameFormat = (a, b) => a.type === b.type && a[VALUE_KEY[a.type]] === b[VALUE_KEY[b.type]];

function normalized(text, spans) {
    return {
        text,
        spans: spans
            .filter(s => s.start < s.end)
            .sort((a, b) => a.start - b.start || a.end - b.end || KINDS.indexOf(a.format.type) - KINDS.indexOf(b.format.type)),
    };
}

/** Removes formats of `kind` from [start, end), splitting spans that stick out. */
export function removeKind(rt, kind, start, end) {
    if (start >= end) return rt;
    const spans = [];
    for (const s of rt.spans) {
        if (s.format.type !== kind || s.end <= start || s.start >= end) {
            spans.push(s);
            continue;
        }
        if (s.start < start) spans.push({ ...s, end: start });
        if (s.end > end) spans.push({ ...s, start: end });
    }
    return normalized(rt.text, spans);
}

/** Applies `format` to [start, end), replacing other formats of its kind and merging equal neighbours. */
export function applyFormat(rt, format, start, end) {
    if (start >= end) return rt;
    const removed = removeKind(rt, format.type, start, end);
    let mergedStart = start;
    let mergedEnd = end;
    const spans = removed.spans.filter(s => {
        const touches = s.format.type === format.type && s.end >= start && s.start <= end && sameFormat(s.format, format);
        if (touches) {
            mergedStart = Math.min(mergedStart, s.start);
            mergedEnd = Math.max(mergedEnd, s.end);
        }
        return !touches;
    });
    spans.push({ start: mergedStart, end: mergedEnd, format });
    return normalized(rt.text, spans);
}

/** Sets (or with `format == null` clears) the format of `kind` on [start, end). */
export function setKind(rt, kind, format, start, end) {
    return format ? applyFormat(rt, format, start, end) : removeKind(rt, kind, start, end);
}

/** Formats typed text at `position` inherits: those of the preceding character. */
export function formatsAt(rt, position) {
    const result = {};
    if (rt.text.length === 0) return result;
    const index = position > 0 ? position - 1 : 0;
    for (const s of rt.spans) if (s.start <= index && s.end > index) result[s.format.type] = s.format;
    return result;
}

/** Formats covering the whole of [start, end). */
export function formatsIn(rt, start, end) {
    if (start === end) return formatsAt(rt, start);
    const result = {};
    for (const kind of KINDS) {
        const spans = rt.spans.filter(s => s.format.type === kind && s.start < end && s.end > start);
        if (spans.length === 0) continue;
        const format = spans[0].format;
        const covered = spans.filter(s => sameFormat(s.format, format)).sort((a, b) => a.start - b.start);
        let reached = start;
        for (const s of covered) {
            if (s.start > reached) break;
            reached = Math.max(reached, s.end);
        }
        if (reached >= end) result[kind] = format;
    }
    return result;
}

/**
 * Replaces [start, end) with `insert`. Spans behave like Android's SPAN_EXCLUSIVE_EXCLUSIVE:
 * text inserted at a span boundary does not extend the span.
 */
export function replaceText(rt, start, end, insert) {
    const delta = insert.length - (end - start);
    const mapStart = p => (p < start ? p : p >= end ? p + delta : start + insert.length);
    const mapEnd = p => (p <= start ? p : p >= end ? p + delta : start);
    const text = rt.text.slice(0, start) + insert + rt.text.slice(end);
    return normalized(text, rt.spans.map(s => ({ ...s, start: mapStart(s.start), end: mapEnd(s.end) })));
}

export function richTextEquals(a, b) {
    return encode(a) === encode(b);
}

// ---------------------------------------------------------------- ruby markup (RubyMarkup.kt)

const RUBY_PATTERN = /\{([^{}|]+)\|([^{}|]+)\}/g;
const isBlank = s => s.trim() === '';

export function findRuby(text) {
    const matches = [];
    for (const m of text.matchAll(RUBY_PATTERN)) {
        if (isBlank(m[1]) || isBlank(m[2])) continue;
        matches.push({ start: m.index, end: m.index + m[0].length, base: m[1], reading: m[2] });
    }
    return matches;
}

export const formatRuby = (base, reading) => `{${base}|${reading}}`;

export const isValidRubyPart = value => !isBlank(value) && !/[{}|]/.test(value);

export function stripToBase(text) {
    return text.replace(RUBY_PATTERN, (all, base, reading) => (isBlank(base) || isBlank(reading) ? all : base));
}

// ---------------------------------------------------------------- HTML

export function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

export function argbToCss(argb) {
    const a = ((argb >>> 24) & 0xFF) / 255;
    return `rgba(${(argb >>> 16) & 0xFF}, ${(argb >>> 8) & 0xFF}, ${argb & 0xFF}, ${Number(a.toFixed(3))})`;
}

export function formatsToCss(formats) {
    const css = [];
    if (formats.bold) css.push('font-weight:700');
    if (formats.italic) css.push('font-style:italic');
    const decorations = [formats.underline && 'underline', formats.strikethrough && 'line-through'].filter(Boolean);
    if (decorations.length) css.push(`text-decoration:${decorations.join(' ')}`);
    if (formats.color) css.push(`color:${argbToCss(formats.color.argb)}`);
    if (formats.highlight) css.push(`background-color:${argbToCss(formats.highlight.argb)}`);
    if (formats.size) css.push(`font-size:${Number(formats.size.proportion)}em`);
    return css.join(';');
}

/** Splits [from, to) into runs of equal formatting: [{start, end, formats}]. */
export function formatRuns(rt, from = 0, to = rt.text.length) {
    const cuts = new Set([from, to]);
    for (const s of rt.spans) {
        if (s.start > from && s.start < to) cuts.add(s.start);
        if (s.end > from && s.end < to) cuts.add(s.end);
    }
    const points = [...cuts].sort((a, b) => a - b);
    const runs = [];
    for (let i = 0; i < points.length - 1; i++) {
        const formats = {};
        for (const s of rt.spans) if (s.start <= points[i] && s.end >= points[i + 1]) formats[s.format.type] = s.format;
        runs.push({ start: points[i], end: points[i + 1], formats });
    }
    return runs;
}

function formattedHtml(rt, from, to) {
    return formatRuns(rt, from, to)
        .map(run => {
            const text = escapeHtml(rt.text.slice(run.start, run.end));
            const css = formatsToCss(run.formats);
            return css ? `<span style="${css}">${text}</span>` : text;
        })
        .join('');
}

/** Read-only rendering with furigana as <ruby>, like the app's RubyText. Expects `white-space: pre-wrap`. */
export function toHtml(rt) {
    let html = '';
    let position = 0;
    for (const m of findRuby(rt.text)) {
        const baseStart = m.start + 1;
        html += formattedHtml(rt, position, m.start);
        html += `<ruby>${formattedHtml(rt, baseStart, baseStart + m.base.length)}<rt>${escapeHtml(m.reading)}</rt></ruby>`;
        position = m.end;
    }
    return html + formattedHtml(rt, position, rt.text.length);
}

/** Renders a plain String field with ruby markup (lecture names). */
export const rubyHtml = text => toHtml({ text, spans: [] });
