#!/usr/bin/env node

const { Command } = require("commander");
const path = require("path");
const fs = require("fs");
const { validateToken } = require("../src/validate");
const { createBot } = require("../src/index");

const ENV_FILE = path.resolve(process.cwd(), ".env");

const program = new Command();

program
  .name("pi-tele")
  .description("A simple Telegram bot runner")
  .version("0.1.0");

// ── setup ──────────────────────────────────────────────────────────────────
program
  .command("setup")
  .description("Configure your Telegram bot token")
  .requiredOption(
    "-t, --token <token>",
    "Your Telegram bot token from @BotFather",
  )
  .action(async (opts) => {
    const token = opts.token.trim();

    console.log("🔍 Validating token with Telegram...");
    const result = await validateToken(token);

    if (!result.ok) {
      console.error(`❌ Invalid token: ${result.error}`);
      console.error("   Get a valid token from @BotFather on Telegram.");
      process.exit(1);
    }

    const { first_name, username } = result.bot;
    console.log(`✅ Token valid! Connected to: ${first_name} (@${username})`);

    // Write / update the .env file
    let envContent = "";
    if (fs.existsSync(ENV_FILE)) {
      // Replace existing TELEGRAM_BOT_TOKEN line if present
      envContent = fs.readFileSync(ENV_FILE, "utf8");
      if (/^TELEGRAM_BOT_TOKEN=.*/m.test(envContent)) {
        envContent = envContent.replace(
          /^TELEGRAM_BOT_TOKEN=.*/m,
          `TELEGRAM_BOT_TOKEN=${token}`,
        );
      } else {
        envContent += `\nTELEGRAM_BOT_TOKEN=${token}\n`;
      }
    } else {
      envContent = `TELEGRAM_BOT_TOKEN=${token}\n`;
    }

    fs.writeFileSync(ENV_FILE, envContent);
    console.log(`💾 Token saved to ${ENV_FILE}`);
    console.log('🚀 Run "pi-tele start" to launch your bot.');
  });

// ── start ──────────────────────────────────────────────────────────────────
program
  .command("start")
  .description("Start the bot using the token from .env")
  .option("-t, --token <token>", "Override token (skips .env lookup)")
  .action(async (opts) => {
    let token = opts.token;

    if (!token) {
      // Load from .env in cwd
      if (!fs.existsSync(ENV_FILE)) {
        console.error(
          '❌ No .env file found. Run "pi-tele setup --token <token>" first.',
        );
        process.exit(1);
      }
      require("dotenv").config({ path: ENV_FILE });
      token = process.env.TELEGRAM_BOT_TOKEN;
    }

    if (!token) {
      console.error(
        '❌ TELEGRAM_BOT_TOKEN is not set. Run "pi-tele setup --token <token>" first.',
      );
      process.exit(1);
    }

    console.log("🔍 Verifying token before starting...");
    const result = await validateToken(token);
    if (!result.ok) {
      console.error(`❌ Token check failed: ${result.error}`);
      process.exit(1);
    }

    const { first_name, username } = result.bot;
    console.log(`🤖 Starting bot: ${first_name} (@${username})`);
    createBot(token, {
      agentCommand: process.env.AGENT_COMMAND || "pi",
      agentWorkingDir: process.env.AGENT_WORKING_DIR || process.cwd(),
      agentTimeout: parseInt(process.env.AGENT_TIMEOUT || "300", 10),
      agentModel: process.env.AGENT_MODEL || undefined,
    });
  });

// ── status ─────────────────────────────────────────────────────────────────
program
  .command("status")
  .description("Check bot configuration and connectivity")
  .action(async () => {
    if (!fs.existsSync(ENV_FILE)) {
      console.log(
        '⚠️  Not configured. Run "pi-tele setup --token <token>" to get started.',
      );
      process.exit(0);
    }

    require("dotenv").config({ path: ENV_FILE });
    const token = process.env.TELEGRAM_BOT_TOKEN;

    if (!token) {
      console.log("⚠️  .env exists but TELEGRAM_BOT_TOKEN is empty.");
      process.exit(0);
    }

    console.log("🔍 Checking token...");
    const result = await validateToken(token);

    if (!result.ok) {
      console.log(`❌ Token is invalid or expired: ${result.error}`);
      console.log('   Run "pi-tele setup --token <new_token>" to update it.');
    } else {
      const { first_name, username, id } = result.bot;
      console.log("✅ Bot is configured and token is valid.");
      console.log(`   Name    : ${first_name}`);
      console.log(`   Username: @${username}`);
      console.log(`   Bot ID  : ${id}`);
    }
  });

program.parse(process.argv);
