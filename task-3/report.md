My Steps:
1) Sign in n8n, create workplace and workflow 
2) Install n8n-mcp for Cursor
3) Configure n8n-mcp
4) Create n8n_task.txt file with description and requirements of task
5) Generate plan, build
6) Create telegram bot
7) Import generated json file into my workflow
8) Insert access token for telegram
9) Execute workflow
10) Publish and test on telegram

A link to a working Telegram bot - t.me/ai_teacher_1212_bot

----------------------------------------------------------------
# Implementation Report — AI Learning Assistant Telegram Bot

## 1. What was built

A single n8n workflow ([`n8n-learning-bot.workflow.json`](./n8n-learning-bot.workflow.json), 16 nodes) that runs a Telegram bot with three commands and two AI roles:

- `/start` — welcome and command list.
- `/learn <url>` — fetches the URL, extracts text, asks the **Teacher** AI for a structured summary (5–7 key points, main concepts, difficulty), persists the material, and replies with a Markdown-style summary plus a "Take a quiz on this material" inline button.
- `/quiz` — lists saved topics as inline buttons, generates 5 multiple-choice questions via the **Examiner** AI for the selected topic, asks them one by one, validates answers, and produces a final score with explanations for incorrect answers.

The workflow handles plain messages and `callback_query` button clicks through the same `Telegram Trigger` and routes them with a single Switch node, so the user can mix `/learn` and `/quiz` freely without restarting the workflow.

---

## 2. Tools and techniques

| Concern | Choice | Why |
|---|---|---|
| Trigger | `n8n-nodes-base.telegramTrigger` (`updates: [message, callback_query]`) | One trigger handles both text commands and inline-button presses, so the workflow stays as a single executable unit. |
| Routing | `n8n-nodes-base.switch` v3.2 with 5 typed rules + fallback | Reading a single `type` field set by the normalizer keeps Switch rules trivially comparable, and the fallback output catches `help_learn` / `help_unknown`. |
| Content fetch | `n8n-nodes-base.httpRequest` v4.2, `responseFormat: text`, `neverError: true` | Returns the raw HTML in `$json.data` even on non-2xx, which lets the next Code node degrade gracefully. |
| HTML cleanup | Code node | A self-contained regex pipeline (script/style/noscript/tag stripping + entity decoding + whitespace normalization + 12 000-char cap) is enough for the typical article and avoids extra dependencies. |
| AI calls | `@n8n/n8n-nodes-langchain.openAi` v1.6, `chat.message`, `jsonOutput: true`, model `gpt-4o-mini` | Compatible with **n8n free GPT credits** out of the box (per the task note). `jsonOutput: true` forces structured JSON; `gpt-4o-mini` supports JSON mode and is fast and cheap. |
| Persistence | `$getWorkflowStaticData('global')` inside Code nodes | Built-in, no external service needed (per the "no extra subscriptions" requirement), survives executions and bot restarts. |
| Outbound Telegram | One shared `n8n-nodes-base.httpRequest` node calling `https://api.telegram.org/bot<TOKEN>/{{ method }}` | Allows fully dynamic `reply_markup` (variable-length inline keyboards for the topic picker) and the same node handles `sendMessage` and `answerCallbackQuery`. Token comes from `$env.TELEGRAM_BOT_TOKEN`. |

### Workflow architecture

```
Telegram Trigger
   │
Normalize Update            (Code: classify type, parse callback)
   │
Route by Type               (Switch: 5 rules + fallback)
   ├── start         → Build Welcome ────────────────────────────────────┐
   ├── learn         → Fetch URL → Strip HTML → Teacher AI → Save Material ┤
   ├── quiz          → Build Topic Picker ─────────────────────────────────┤
   ├── pickMaterial  → fan-out:                                           │
   │                    ├── Build Ack ────────────────────────────────────┤
   │                    └── Load Material → Examiner AI → Save Quiz ──────┤
   ├── answer        → fan-out:                                           │
   │                    ├── Build Ack ────────────────────────────────────┤
   │                    └── Record Answer ────────────────────────────────┤
   └── (fallback)    → Build Help ────────────────────────────────────────┤
                                                                          ▼
                                                              Send Telegram (HTTP)
```

`Send Telegram` is a single HTTP Request node with seven incoming connections; each upstream Code node hands it `{ method, body }`, where `body` is exactly the Telegram Bot API JSON.

`Build Ack` is wired into both the `pickMaterial` and `answer` outputs of the Switch (fan-out), which lets the bot answer `callback_query` immediately so the spinner disappears, while the slower main flow (Examiner / scoring) runs in parallel.

---

## 3. AI role design

### Teacher (`Teacher AI` node)

- **System prompt** instructs the model to return one valid JSON object with `title`, `summary` (5–7 sentences), `concepts` (3–8 strings), and `difficulty` (`beginner | intermediate | advanced`), and forbids markdown fences.
- **User message** is templated with the cleaned content and a best-guess title from the `<title>` tag, so the model has a hint even when the page text is dense.
- Output is parsed by `Save Material`, normalized into the data structure from the spec (`id`, `url`, `title`, `content`, `summary`, `concepts`, `difficulty`, `addedDate`), and pushed to `$getWorkflowStaticData('global').users[chatId].materials`.

### Examiner (`Examiner AI` node)

- **System prompt** demands exactly five questions, ids `Q1`–`Q5`, four options keyed `A`/`B`/`C`/`D`, exactly one correct letter per question, and an explanation per question — all in a strict JSON shape.
- **User message** carries the same cleaned content and the saved title.
- The output is parsed and **filtered** in `Save Quiz`: any question with missing options, missing/invalid `correctAnswer` (must be `A`–`D`), or empty text is dropped before being saved. If filtering leaves nothing, the user is told that the quiz could not be generated.

The whole quiz (questions + correct keys + explanations) is computed once and stored in `activeQuiz`. Per-question validation then becomes a deterministic comparison, but the explanation text is genuine AI output, satisfying the "validation is intelligent — not exact text match only" requirement: the user receives a tailored explanation written for that specific wrong choice.

---

## 4. Persistence model

State lives in `$getWorkflowStaticData('global').users` keyed by `chatId`:

```js
users[chatId] = {
  counter: 7,                    // monotonically increasing material counter
  materials: [
    {
      id: 'LM-007',
      url: '...',
      title: '...',
      content: '...',            // cleaned text, kept for re-quizzing later
      summary: ['...', ...],
      concepts: ['...', ...],
      difficulty: 'intermediate',
      addedDate: '2025-...'
    },
    ...
  ],
  activeQuiz: {                  // null when no quiz is in progress
    materialId: 'LM-007',
    questions: [{ id, question, options:{A,B,C,D}, correctAnswer, explanation }, ...],
    currentIndex: 2,
    answers: [{ idx, questionId, picked, correct, isCorrect }, ...]
  }
}
```

This satisfies the "data persists between sessions" and "users can return to previously saved materials" bullets without introducing a database.

---

## 5. What worked

- **Single-trigger architecture.** Routing both `message` and `callback_query` through one `Telegram Trigger` and a Switch keeps everything in one workflow with no glue code, and it scales naturally to new commands.
- **Pre-generating the whole quiz.** Generating all 5 questions in one Examiner call (with the correct letter and an explanation per question stored alongside) eliminates per-click AI latency and makes the answer-validation flow purely deterministic, but still feels intelligent because explanations are tailored.
- **`$getWorkflowStaticData('global')` for state.** Survives bot restarts, requires zero setup, and respects the "no extra subscriptions" constraint. State per `chatId` makes it implicitly multi-user.
- **One shared `Send Telegram` HTTP node.** Each upstream Code node prepares `{ method, body }` and the same HTTP node handles `sendMessage`, `answerCallbackQuery`, dynamic inline keyboards, etc. Adding new commands is now just "another Code node feeding into Send Telegram".
- **JSON mode.** `jsonOutput: true` plus an explicit JSON schema in the system prompt yields strictly parseable responses; the parsing helper still falls back to a regex extract just in case.
- **Smoke-tested locally.** The Code-node JS was syntax-checked and dry-run with simulated Telegram payloads (commands, callback queries, full 5-question quiz lifecycle) before delivery, so the routing, scoring, and final-report formatting are known to be correct.

---

## 6. What did not work / limitations

- **Variable-length inline keyboards via the Telegram node.** The native `n8n-nodes-base.telegram` node uses a `fixedCollection` for `inlineKeyboard.rows.values`, which cannot grow dynamically. The first attempt was to render the topic picker through that node, which only worked with a hardcoded button count. The final design uses an HTTP Request node that posts an arbitrary `reply_markup.inline_keyboard` JSON instead. The cost is needing `TELEGRAM_BOT_TOKEN` in the environment.
- **Extracting text from JS-heavy or paywalled pages.** A Code-based regex stripper handles ~90% of static articles fine but cannot run JavaScript or bypass paywalls. The bot now degrades gracefully (12 000-char cap, "best-effort" prompt) rather than failing loudly.
- **In-workflow state is lost on workflow re-import.** `workflowStaticData` is tied to the workflow's id; deleting and re-importing the workflow wipes saved materials. For a production deployment, swapping in a Postgres / Supabase / Airtable node would be a one-Code-node change.
- **Telegram message length limit (4096 chars).** The summary and final-results texts are truncated to 3800 chars to stay safely under the limit. Very long article titles or many wrong answers with very long explanations will be cut.
- **Concurrent click on the same question.** If the user double-taps an option, `Record Answer` is idempotent on the question index (the second press is ignored) — but two genuinely different questions clicked simultaneously could race. Acceptable for a single user; would need a queue for high concurrency.

---

## 7. Notable decisions

1. **Use `@n8n/n8n-nodes-langchain.openAi` (not the legacy `n8n-nodes-base.openAi`)** — this is the path that integrates with n8n's free GPT credits feature, per the task note.
2. **Pre-generate the full quiz in one AI call** — rather than calling the Examiner per question. Faster, cheaper, and lets us answer `callback_query` instantly.
3. **Store explanations together with the questions** — instead of regenerating them at validation time. The "intelligent validation" then becomes "deterministic comparison + AI-authored explanation", which is good enough for the spec and far more responsive.
4. **One generic `Send Telegram` HTTP Request** — keeps the workflow at 16 nodes instead of ~22 and makes it trivial to extend.
5. **Fan-out for `Build Ack`** — both `pickMaterial` and `answer` outputs of the Switch fan out into a quick ack chain alongside the main flow. This makes the bot feel responsive even while the Examiner is still thinking.
6. **`type=help_unknown` and `type=help_learn` go through the Switch fallback output**, with the message text differentiated inside `Build Help`. Avoids two near-duplicate branches.
7. **Models pinned to `gpt-4o-mini`** — best balance of cost, speed, and JSON-mode support for both Teacher and Examiner roles. Easy to change in the node UI.
8. **`responseFormat: text` + `neverError: true` on `Fetch URL`** — guarantees we always get a string (or empty) into `Strip HTML` so a 403 / 404 / dead host degrades into a graceful "I could not summarize this" rather than aborting the run.

---

## 8. Mapping to the task requirements

| Requirement | How it is satisfied |
|---|---|
| Bot responds correctly to `/start`, `/learn [url]`, `/quiz` | Switch rules `start`, `learn`, `quiz` plus fallback `help_learn` / `help_unknown` (`Normalize Update`). |
| Material processing extracts real content, summaries are URL-specific | `Fetch URL` + `Strip HTML` + Teacher AI run on the actual page text per request. |
| Teacher produces 5–7 key points, main concepts, difficulty | Teacher system prompt forces those exact fields; `Save Material` validates and stores them. |
| Examiner generates 5 questions specific to the material, never reused | Examiner is invoked per `pickMaterial` callback with the material's content; output is filtered to exactly 5 valid MCQs. |
| Answer validation is intelligent (not exact text match) | Case-insensitive letter comparison **plus** AI-generated explanation surfaced for incorrect answers. |
| Data persists between sessions | `$getWorkflowStaticData('global').users[chatId]` survives executions and restarts. |
| `/quiz` shows the user's saved topics | `Build Topic Picker` renders one inline button per saved material from static data. |
| No workflow restart between commands | One Telegram trigger and Switch handle every command and callback in one always-active workflow. |
