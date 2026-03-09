# pi-tele

A zero-config Telegram bot runner. Install it, give it your bot token, and it just works.

---

## Installation

### Global (recommended for Termux / personal use)

```bash
npm install -g pi-tele
```

### Or run without installing

```bash
npx pi-tele <command>
```

---

## Quick Start

### 1. Get a bot token

Open Telegram and message [@BotFather](https://t.me/BotFather):

```
/newbot
```

Copy the token it gives you.

### 2. Set up your token

```bash
pi-tele setup --token 123456789:ABCdefGhIJKlmNoPQRsTUVwxyz
```

This validates the token with Telegram and saves it to a `.env` file in the current directory.

### 3. Start the bot

```bash
pi-tele start
```

---

## Commands

| Command                         | Description                                |
| ------------------------------- | ------------------------------------------ |
| `pi-tele setup --token <token>` | Validate and save your bot token           |
| `pi-tele start`                 | Start the bot                              |
| `pi-tele start --token <token>` | Start with an inline token (skips .env)    |
| `pi-tele status`                | Check if the token is configured and valid |
| `pi-tele --help`                | Show help                                  |

---

## What the bot does

| User sends | Bot replies     |
| ---------- | --------------- |
| `/start`   | Welcome message |
| `/help`    | Lists commands  |
| `/ping`    | Pong!           |
| Any text   | Echoes it back  |

---

## Termux Setup (Android)

```bash
pkg update && pkg install nodejs
npm install -g pi-tele
pi-tele setup --token YOUR_TOKEN
pi-tele start
```
