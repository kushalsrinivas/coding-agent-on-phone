const TelegramBot = require("node-telegram-bot-api");
const { executePrompt } = require("./agent");
const { splitMessage, formatResponse } = require("./formatter");

/**
 * Creates and starts a pi-tele bot instance.
 * @param {string} token - Telegram Bot API token
 * @param {Object} [options={}] - Optional configuration
 * @param {Object} [options.polling] - Polling options passed to node-telegram-bot-api
 * @param {string} [options.agentCommand] - Path/name of the pi CLI
 * @param {string} [options.agentWorkingDir] - Working directory for agent execution
 * @param {number} [options.agentTimeout] - Max seconds per prompt
 * @param {string} [options.agentModel] - Model override for the agent
 * @returns {TelegramBot} The running bot instance
 */
function createBot(token, options = {}) {
  if (!token) {
    throw new Error(
      "A Telegram bot token is required. Get one from @BotFather.",
    );
  }

  const bot = new TelegramBot(token, {
    polling: options.polling ?? true,
  });

  // /start command
  bot.onText(/\/start/, (msg) => {
    const chatId = msg.chat.id;
    const firstName = msg.from?.first_name ?? "there";
    bot.sendMessage(
      chatId,
      `👋 Hey ${firstName}! I'm pi-tele bot.\n\nUse /prompt <message> to send a prompt to the coding agent. Use /help to see all commands.`,
    );
  });

  // /help command
  bot.onText(/\/help/, (msg) => {
    const chatId = msg.chat.id;
    bot.sendMessage(
      chatId,
      `📖 *Available Commands*\n\n` +
        `/start — Welcome message\n` +
        `/help — Show this help\n` +
        `/ping — Check if I'm alive\n` +
        `/prompt <text> — Send a prompt to the coding agent\n\n` +
        `The /prompt command forwards your message to the pi coding agent, ` +
        `which can read files, run commands, and edit code on your behalf.`,
      { parse_mode: "Markdown" },
    );
  });

  // /ping command
  bot.onText(/\/ping/, (msg) => {
    const chatId = msg.chat.id;
    bot.sendMessage(chatId, "🏓 Pong!");
  });

  // /prompt command — forward to pi coding agent
  bot.onText(/\/prompt\s+([\s\S]+)/, async (msg, match) => {
    const chatId = msg.chat.id;
    const prompt = match[1].trim();

    if (!prompt) {
      bot.sendMessage(chatId, "Usage: /prompt <your message to the agent>");
      return;
    }

    const status = await bot.sendMessage(
      chatId,
      `⏳ Processing your prompt...`,
    );

    try {
      const output = await executePrompt(prompt, {
        command: options.agentCommand,
        workingDir: options.agentWorkingDir,
        timeout: options.agentTimeout,
        model: options.agentModel,
      });

      const parts = splitMessage(formatResponse(output), 4096);
      for (const part of parts) {
        await bot.sendMessage(chatId, part);
      }
    } catch (err) {
      const errText = err.message || "Unknown error";
      const parts = splitMessage(`❌ Agent error:\n\n${errText}`, 4096);
      for (const part of parts) {
        await bot.sendMessage(chatId, part);
      }
    }
  });

  // Echo all other text messages
  bot.on("message", (msg) => {
    if (msg.text && !msg.text.startsWith("/")) {
      const chatId = msg.chat.id;
      bot.sendMessage(chatId, `You said: ${msg.text}`);
    }
  });

  bot.on("polling_error", (err) => {
    console.error("[pi-tele] Polling error:", err.message);
  });

  console.log("[pi-tele] Bot is running...");
  if (options.agentWorkingDir) {
    console.log(
      `[pi-tele] Agent working directory: ${options.agentWorkingDir}`,
    );
  }
  return bot;
}

module.exports = { createBot };
