A link to a working Telegram bot - t.me/ai_teacher_1212_bot

# AI Learning Assistant — Telegram Bot on n8n

A Telegram bot that turns any web article into a structured study summary and a 5-question multiple-choice quiz, powered by two AI roles (`Teacher` and `Examiner`) running inside a single n8n workflow.

The whole bot is one importable workflow file: [`n8n-learning-bot.workflow.json`](./n8n-learning-bot.workflow.json).

---

## What the bot does

| Command | Behaviour |
|---|---|
| `/start` | Shows a welcome message and the list of supported commands. |
| `/learn <url>` | Fetches the URL, extracts text, asks the **Teacher** AI for a structured summary (5–7 key points, main concepts, difficulty), saves it, and replies with the summary plus a "Take a quiz on this material" button. |
| `/quiz` | Lists previously saved materials as inline buttons. After you pick one, the **Examiner** AI generates 5 multiple-choice questions and sends them one by one. At the end you get a score and per-question feedback. |

Materials and quiz progress are persisted in n8n's workflow static data, so they survive between executions and across sessions for the same user.

---

## Prerequisites

1. **n8n** — a free trial of [n8n Cloud](https://n8n.io) is enough, or any self-hosted n8n 1.x instance.
2. **A Telegram bot token** — created via [@BotFather](https://t.me/BotFather) (`/newbot`, follow the prompts).
3. **An OpenAI API key** _or_ the bundled n8n free GPT credits that come with the trial.

The workflow uses `gpt-4o-mini` by default (fast, cheap, supports JSON mode). You can change the model in the `Teacher AI` and `Examiner AI` nodes if you prefer another one.

---

## Setup, step by step

### 1. Create your Telegram bot

1. Open Telegram, talk to [@BotFather](https://t.me/BotFather), send `/newbot`, give it a name and a username.
2. Save the **bot token** that BotFather gives you. Looks like `123456:ABC-XXXX...`.
3. Recommended: send `/setcommands` to BotFather and paste:

   ```
   start - Show welcome and commands
   learn - Submit a URL to study (usage: /learn <url>)
   quiz - Pick a saved topic and take a quiz
   ```

### 2. Import the workflow into n8n

1. In n8n, go to **Workflows → Import from File** (or the menu inside the editor → **Import from File**).
2. Pick `task-3/n8n-learning-bot.workflow.json`.
3. The workflow opens in the editor with 16 nodes laid out left-to-right.

### 3. Hook up credentials

The import marks two credentials as missing. Open each of these nodes and pick or create the credentials.

#### a) Telegram credential (used by `Telegram Trigger`)

- In the `Telegram Trigger` node, click the credential dropdown → **Create New**.
- Type: **Telegram API**.
- Paste your bot token from BotFather. Save.

#### b) OpenAI credential (used by `Teacher AI` and `Examiner AI`)

You have two options:

- **Use n8n free GPT credits (trial users):** when the OpenAI node asks for credentials, pick the built-in **n8n free OpenAI credits** option. No key needed.
- **Use your own OpenAI key:** create a new **OpenAi API** credential and paste your `sk-...` key.

Set the same OpenAI credential on both `Teacher AI` and `Examiner AI`.

### 4. Provide the bot token to the outbound HTTP node

The bot uses one shared `Send Telegram` HTTP Request node to call the Telegram Bot API (this is what allows dynamic inline keyboards). It reads the bot token from the environment variable `TELEGRAM_BOT_TOKEN`.

Pick whichever option fits your environment:

| Environment | How to set `TELEGRAM_BOT_TOKEN` |
|---|---|
| n8n Cloud trial | Open the workflow → **Settings → Variables** (if available), add `TELEGRAM_BOT_TOKEN`. If your plan does not expose variables, fall back to the inline option below. |
| Self-hosted (docker / docker-compose) | Add `TELEGRAM_BOT_TOKEN=123456:ABC...` to the `n8n` service environment and restart. |
| Self-hosted (npm) | Set `TELEGRAM_BOT_TOKEN` in your shell or `.env` before starting n8n. |
| Inline fallback | Open the `Send Telegram` node, replace `{{ $env.TELEGRAM_BOT_TOKEN }}` in the URL with your literal token. Do the same in any future copies of the node. (Less secure: the token ends up in the workflow JSON.) |

> Why the env var? Most outgoing messages can be sent through the regular Telegram node, but the **topic picker** has a variable number of inline buttons (one per saved material), which n8n's Telegram node cannot express statically. We therefore call `https://api.telegram.org/bot<TOKEN>/sendMessage` directly, with the token injected from the environment.

### 5. Activate the workflow

Click the **Active** toggle in the top right of the n8n editor. The Telegram Trigger registers a webhook with Telegram and the bot is live.

You can quickly verify with `/start` in your bot's chat — you should see the welcome message within a few seconds.

---

## Using the bot

```
You: /start
Bot: Welcome to your AI-powered learning assistant! ...

You: /learn https://en.wikipedia.org/wiki/Photosynthesis
Bot: (5–7 key points, main concepts, difficulty)
     [ Take a quiz on this material ]

You tap the button (or send /quiz later)
Bot: Quiz on: Photosynthesis
     Question 1 of 5: ...
     [A] [B]
     [C] [D]

You tap [B]
Bot: Question 2 of 5: ...

...after the 5th question:
Bot: Quiz complete!
     Score: 4/5 (80%)
     Q1: Correct (you picked B).
     Q2: Incorrect (you picked A, correct is C).
        Explanation: ...
     ...

You: /quiz
Bot: Pick a topic for your quiz:
     [ LM-001: Photosynthesis ]
     [ LM-002: React Hooks Guide ]
```

A few good things to know:

- You can run `/learn` as many times as you like — each material gets a sequential ID like `LM-001`, `LM-002`, ...
- `/quiz` lists **all** of your saved materials (last 20), so you can revisit any topic at any time.
- The bot does not require a workflow restart between commands — everything runs through one Telegram trigger.
- Re-importing or deleting the workflow wipes the in-memory state. Other manual edits do not.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Bot replies nothing at all | Workflow is not active. Toggle **Active** in the top right of the editor. |
| `/start` works but `/learn` and `/quiz` time out | `TELEGRAM_BOT_TOKEN` not set / wrong. Re-check Step 4. |
| Summary says "I could not generate a quiz for this material" | The Examiner returned malformed JSON or the article was extremely short. Try a richer URL. |
| `Fetch URL` fails with 403/blocked | Some sites (paywalls, Cloudflare) refuse server-side fetching. Try a Wikipedia / blog / docs URL. |
| Long article gives generic answers | Content is truncated to 12 000 characters before AI calls (token-limit safety). Use a more focused source. |
| OpenAI rate / quota errors | You exhausted free n8n credits or your key's quota. Add billing or switch credential. |
| Buttons show "loading..." for 30 seconds | Normal during `Examiner AI` generation; the bot acks the callback as soon as `gpt-4o-mini` finishes. |

---

## File layout in this folder

```
task-3/
├── n8n-learning-bot.workflow.json   # the importable n8n workflow
├── README.md                        # this file
├── report.md                        # design notes / decisions
└── n8n_task.txt                     # original task description
```
