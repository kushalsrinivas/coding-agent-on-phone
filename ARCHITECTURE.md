# How This App Works (Plain English)

## The Big Picture

Think of this app as a **control room** on your Android phone. It manages two workers that run inside your phone:

1. **The Coding Agent** -- a Node.js program that can read, write, and edit code for you.
2. **The Telegram Bot** -- a Python program that sits in a Telegram chat, listens for your messages, and tells the coding agent what to do.

Your app is the boss that starts both workers, keeps them alive, lets you watch what they're doing, and lets you shut them down.

---

## The Three Layers

### Layer 1: The Linux Environment (the foundation)

Android phones don't come with Node.js or Python installed. So the very first time you open the app, it downloads a small Linux toolkit (from the Termux project, about 30 MB). This gets extracted into the app's private storage and gives us:

- A basic Linux file system (`bin/`, `lib/`, `etc/`)
- A package manager (`pkg`) that can install stuff
- The ability to run real Linux programs

Once we have that toolkit, the app runs `pkg install nodejs python` to get both runtimes. This only happens once. After that, everything is cached and ready to go.

```
First Launch:
  Download Linux toolkit -> Extract it -> Install Node.js + Python -> Done (never again)

Every Launch After:
  Linux toolkit already there -> Skip straight to starting processes
```

### Layer 2: The Two Processes (the workers)

After the Linux environment is ready, the app starts two separate processes, each in its own terminal:

**Process 1 -- The Coding Agent (Node.js)**

- Runs your custom agent code
- Accepts commands on its input (stdin)
- Outputs results on its output (stdout)
- Has access to the file system to read/write code

**Process 2 -- The Telegram Bot (Python)**

- Connects to Telegram's servers using your bot token
- Waits for messages from you in a Telegram chat
- When you send a command (like "fix the login bug"), it forwards that command to the coding agent
- When the agent finishes, the bot sends the result back to you on Telegram

Each process runs inside a PTY (pseudo-terminal). A PTY is basically a fake screen -- it makes the process think it's running in a real terminal window, so things like colored output and interactive prompts all work normally.

### Layer 3: The App UI (the control room)

The Android app itself is just a nice interface built with Jetpack Compose. It has four screens:

- **Dashboard** -- shows you at a glance if both processes are running, how long they've been up, and recent log output
- **Agent Terminal** -- a full interactive terminal where you can see everything the coding agent is doing (and type commands directly if you want)
- **Bot Terminal** -- same thing but for the Telegram bot
- **Settings** -- where you enter your Telegram bot token, configure the agent, and manage the Linux environment

---

## How the Bot Controls the Agent

This is the most important part. The bot and the agent are two separate programs, so they need a way to talk to each other. We use a **Unix domain socket**.

A Unix domain socket is like a phone call between two programs on the same device. Either side can talk at any time (it's bidirectional), and they send structured messages back and forth as JSON.

There's one socket file on the file system:

```
/data/data/app/run/agent.sock
```

The coding agent creates this socket and listens on it (like picking up the phone). The Telegram bot connects to it (like dialing in). Then they talk using JSON messages:

```
Bot sends:    { "type": "command", "text": "fix the auth bug", "path": "src/" }
Agent replies: { "status": "done", "changes": ["auth.js line 42 fixed"] }
```

Why sockets instead of simpler pipes? Three reasons:

- **Two-way**: Both sides can send and receive on the same connection (pipes are one-way, you'd need two)
- **Non-blocking**: Sockets don't freeze up if one side is slow (pipes can)
- **Structured**: You send proper JSON objects instead of raw text, so errors are easier to handle

Here's the full flow when you send a Telegram message:

```
Step 1: You type "/code fix the auth bug" in Telegram
          |
Step 2: Telegram's servers deliver that message to the Python bot
          |
Step 3: Security check -- is your Telegram user ID in the allowlist?
        Are you under the rate limit? If no, the message is dropped.
          |
Step 4: The bot builds a JSON message and sends it through the socket
          |
Step 5: The coding agent receives the JSON, checks that the requested
        file path is inside the allowed directory (sandbox check)
          |
Step 6: The agent does its thing (reads files, edits code, etc.)
        and sends a JSON result back through the socket
          |
Step 7: The bot reads the result from the socket
          |
Step 8: The bot sends the result back to you as a Telegram message
          |
Step 9: You see the result in your Telegram chat
```

Meanwhile, the Android app's terminal views show you ALL of this happening in real time -- you can watch the agent think and the bot send messages.

---

## Security (Important)

Since the Telegram bot lets you execute code and edit files remotely, we need three safety nets:

1. **User allowlist** -- Only YOUR Telegram user ID(s) can send commands. Everyone else is ignored. You configure this in the app's Settings screen.

2. **Directory sandbox** -- The coding agent can only touch files inside folders you explicitly allow (like `~/projects/`). It can't wander into system files or other apps' data. If a command tries to access `/etc/passwd` or `../../secrets`, it gets blocked.

3. **Rate limiter** -- Maximum number of commands per minute. If your bot token ever leaks, an attacker can't flood the agent with thousands of requests. They'll get throttled immediately.

```
Without security:
  Anyone finds your bot -> sends "delete everything" -> Bad day

With security:
  Random person finds your bot -> not in allowlist -> Message ignored -> You're fine
  You send a command -> in allowlist, under rate limit -> Runs in sandbox -> Safe
```

---

## Keeping Things Alive

Android is aggressive about killing apps that run in the background. If you switch to another app, Android might kill your processes after a few minutes.

To prevent this, we run a **Foreground Service**. This is an Android concept that says "hey, I'm doing important work, don't kill me." It shows a small persistent notification that says something like "2 processes running." As long as that notification is there, Android won't kill your processes.

```
Without Foreground Service:
  You switch to Chrome -> Android kills bot + agent after ~2 min -> Everything stops

With Foreground Service:
  You switch to Chrome -> Notification stays -> Processes keep running -> All good
```

---

## Summary: The Whole Thing in One Diagram

```
┌─────────────────────────────────────────────────────┐
│                  YOUR ANDROID PHONE                  │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │            YOUR APP (Kotlin)                  │   │
│  │                                               │   │
│  │   ┌─────────┐  ┌────────┐  ┌──────────┐     │   │
│  │   │Dashboard │  │Agent   │  │Bot       │     │   │
│  │   │(status)  │  │Terminal│  │Terminal  │     │   │
│  │   └─────────┘  └────────┘  └──────────┘     │   │
│  │                                               │   │
│  │   ┌──────────────────────────────────────┐   │   │
│  │   │  Foreground Service (keeps alive)     │   │   │
│  │   └──────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │       LINUX ENVIRONMENT (Termux bootstrap)    │   │
│  │                                               │   │
│  │   ┌─────────────┐   socket   ┌────────────┐  │   │
│  │   │ Coding Agent │◄══════════│ Telegram   │  │   │
│  │   │ (Node.js)    │══════════►│ Bot        │  │   │
│  │   └─────────────┘  (JSON)    │ (Python)   │  │   │
│  │                               └─────┬──────┘  │   │
│  └──────────────────────────────────────┼────────┘   │
│                                         │            │
└─────────────────────────────────────────┼────────────┘
                                          │ internet
                                          ▼
                                    ┌──────────┐
                                    │ Telegram  │
                                    │ Servers   │
                                    └──────────┘
                                          ▲
                                          │
                                     ┌────┴────┐
                                     │   YOU    │
                                     │(Telegram │
                                     │  chat)   │
                                     └─────────┘
```

That's it. The app is a management wrapper around two processes that talk to each other through a Unix socket, running inside a tiny Linux environment on your phone, with a security layer to keep things safe.
