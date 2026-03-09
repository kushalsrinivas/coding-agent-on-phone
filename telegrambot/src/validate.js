const https = require("https");

/**
 * Calls Telegram's getMe endpoint to validate a bot token.
 * @param {string} token
 * @returns {Promise<{ok: boolean, bot?: object, error?: string}>}
 */
function validateToken(token) {
  return new Promise((resolve) => {
    const url = `https://api.telegram.org/bot${token}/getMe`;

    https
      .get(url, (res) => {
        let data = "";
        res.on("data", (chunk) => (data += chunk));
        res.on("end", () => {
          try {
            const json = JSON.parse(data);
            if (json.ok) {
              resolve({ ok: true, bot: json.result });
            } else {
              resolve({
                ok: false,
                error: json.description ?? "Invalid token",
              });
            }
          } catch {
            resolve({ ok: false, error: "Failed to parse Telegram response" });
          }
        });
      })
      .on("error", (err) => {
        resolve({ ok: false, error: `Network error: ${err.message}` });
      });
  });
}

module.exports = { validateToken };
