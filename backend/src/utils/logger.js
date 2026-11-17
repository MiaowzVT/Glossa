import path from 'node:path';
import { fileURLToPath } from 'node:url';

const LEVELS = { debug: 10, info: 20, warn: 30, error: 40 };
const COLORS = { debug: '\x1b[90m', info: '\x1b[36m', warn: '\x1b[33m', error: '\x1b[31m' };
const RESET = '\x1b[0m';
const minLevel = LEVELS[process.env.LOG_LEVEL] ?? LEVELS.debug;
const useColor = process.stdout.isTTY;

// Returns "file.js:line" of whoever called `fn`
function callerLocation(fn) {
    const original = Error.prepareStackTrace;
    Error.prepareStackTrace = (_, stack) => stack;   // get structured CallSite objects
    const holder = {};
    Error.captureStackTrace(holder, fn);             // drop frames from fn upward (the logger itself)
    const site = holder.stack[0];
    Error.prepareStackTrace = original;

    if (!site) return 'unknown';
    let file = site.getFileName() ?? 'unknown';
    if (file.startsWith('file://')) file = fileURLToPath(file);  // ESM gives file:// URLs
    return `${path.basename(file)}:${site.getLineNumber()}`;
}

const logger = {};
for (const level of Object.keys(LEVELS)) {
    logger[level] = function log(...args) {
        if (LEVELS[level] < minLevel) return;
        const tag = `[${callerLocation(log)}] ${level.toUpperCase()}`;
        console[level](useColor ? `${COLORS[level]}${tag}${RESET}` : tag, ...args);
    };
}

export default logger;