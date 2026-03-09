// Matches all ANSI escape sequences (colors, cursor movement, etc.)
const ANSI_RE =
  // eslint-disable-next-line no-control-regex
  /[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><~]/g;

/**
 * Strip ANSI escape codes from a string.
 * Uses a regex so we don't need the ESM-only `strip-ansi` package.
 */
function stripAnsi(text) {
  return text.replace(ANSI_RE, "");
}

/**
 * Split a long string into chunks that each fit within Telegram's message
 * size limit, breaking at line boundaries when possible.
 * @param {string} text
 * @param {number} [maxLen=4096]
 * @returns {string[]}
 */
function splitMessage(text, maxLen = 4096) {
  if (text.length <= maxLen) return [text];

  const parts = [];
  let remaining = text;

  while (remaining.length > 0) {
    if (remaining.length <= maxLen) {
      parts.push(remaining);
      break;
    }

    let splitIdx = remaining.lastIndexOf("\n", maxLen);
    if (splitIdx <= 0) {
      splitIdx = remaining.lastIndexOf(" ", maxLen);
    }
    if (splitIdx <= 0) {
      splitIdx = maxLen;
    }

    parts.push(remaining.slice(0, splitIdx));
    remaining = remaining.slice(splitIdx).replace(/^\n/, "");
  }

  return parts;
}

/**
 * Lightly format agent output for Telegram display.
 * Trims whitespace and returns a fallback if empty.
 */
function formatResponse(text) {
  const trimmed = (text || "").trim();
  return trimmed || "(no output)";
}

module.exports = { stripAnsi, splitMessage, formatResponse };
