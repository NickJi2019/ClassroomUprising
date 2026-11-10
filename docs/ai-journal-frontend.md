# AI journal: frontend history, one commit per request

Branch `ai-journal/frontend-requests`, in the worktree `.claude/worktrees/ai-journal`, starting from `a90c3c3`. Each commit is one request the user made to the frontend Claude conversations, with the frontend files as they stood when that request was finished. Commit messages give the conversation ID, the time, the user's words (with an English translation), the outcome and how the files were obtained.

Conversations:

- `8eea6548-962d-4214-a72f-936514d0c8e8`: the frontend conversation (2026-11-09 11:58 EST to 2026-11-10 08:12 EST), requests 01-17.
- `9c72e958-a190-4956-ae31-da34a3390f9f`: forked from it at 08:18 EST, requests 18-21.
- The earlier Sprint1 skeleton conversation (`1f1fba33`, 2026-11-06) is on its own branch, `claude/sprint1-skeleton-readme-c3656f`, one commit per request. Its work was never merged.

| # | Request time | Conversation | Request | Commit | Frontend files |
| --- | --- | --- | --- | --- | --- |
| 01 | 2026-11-09 11:58 EST | `8eea6548` | Investigate before coding; P3R-style UI proposal (no file changes) | `0b93112` | none |
| 02 | 2026-11-09 12:04 EST | `8eea6548` | Agree the frontend-only role (no file changes) | `b308b49` | none |
| 03 | 2026-11-09 12:27 EST | `8eea6548` | First JavaFX frontend over the backend's Demo day flow | `ce79f04` | 49 added/changed |
| 04 | 2026-11-09 12:56 EST | `8eea6548` | Document the frontend | `2e0b8d2` | 3 added/changed |
| 05 | 2026-11-09 13:27 EST | `8eea6548` | Clock transition for time changes and date-flip calendar for new days | `2817b31` | 15 added/changed |
| 06 | 2026-11-09 15:30 EST | `8eea6548` | Research P3R UI motion; four-phase plan (no file changes) | `db36895` | none |
| 07 | 2026-11-09 15:40 EST | `8eea6548` | Phase 1: image-based layered scenes and the asset manifest | `da000f9` | 41 added/changed, 1 removed |
| 08 | 2026-11-09 15:59 EST | `8eea6548` | Phase 2: one travelling menu cursor | `94c7aa7` | 10 added/changed |
| 09 | 2026-11-09 16:19 EST | `8eea6548` | Copy phase 2 to the main folder (no file changes) | `1288056` | none |
| 10 | 2026-11-09 16:20 EST | `8eea6548` | Phase 3: mask reveal transitions; faster stepped effects | `94b23dd` | 18 added/changed |
| 11 | 2026-11-09 16:41 EST | `8eea6548` | Phase 4: clock and calendar refinement | `05e50bb` | 17 added/changed |
| 12 | 2026-11-09 19:13 EST | `8eea6548` | Combat frontend, first part (stopped at the usage limit) | `e5418df` | 23 added/changed |
| 13 | 2026-11-09 19:30 EST | `8eea6548` | Finish the combat screen | `5527208` | 7 added/changed |
| 14 | 2026-11-09 20:22 EST | `8eea6548` | Tutorial tip cards instead of dialogue | `39fc96e` | 14 added/changed |
| 15 | 2026-11-09 20:39 EST | `8eea6548` | Fit the Grade 11 review lectures; dialogue box grows for long lines | `929701c` | 2 added/changed |
| 16 | 2026-11-10 00:20 EST | `8eea6548` | Lecture slide projector and music/sound system | `f0aa305` | 44 added/changed |
| 17 | 2026-11-10 08:07 EST | `8eea6548` | Evidence notes for the Midterm AI Feasibility Audit Report | `87414ce` | 1 added/changed |
| 18 | 2026-11-10 08:18 EST | `9c72e958` | Review Overview slides with spoken explanations (slide cues) | `93c8f46` | 28 added/changed |
| 19 | 2026-11-10 08:41 EST | `9c72e958` | Check whether git history was saved (no file changes) | `91b5212` | none |
| 20 | 2026-11-10 08:45 EST | `9c72e958` | Inventory conversations and worktrees; commit per conversation (no file changes) | `e9424bc` | none |
| 21 | 2026-11-10 08:54 EST | `9c72e958` | Per-request AI journal history and handoff to the backend | (this commit) | this file |

## How the history was rebuilt

- Claude Code backs up every file it has edited once, at each new user message. Those snapshots give each request's end state exactly. The last snapshot was checked against the final files: all 88 tracked frontend files matched byte for byte.
- 21 files were only written by shell commands and never snapshotted. Each was created and finished within one request: the slide and sound files in request 16, the textures in 07, `TutorialTipsTest` in 14, the audit notes in 17. The transcripts show no later writes, so their final content is also exact for those requests.
- An interrupted message and its resend count as one request (01, 03, 06). A follow-up sent while a request was running is in that request's commit (10).
- Requests that changed no repository file are empty commits (01, 02, 06, 09, 19, 20), so that every request appears once.
- Rebuilding the commits in order ends at exactly the frontend files in the main folder when request 18 started; request 18 adds the files that request copied into the main folder.

## What these commits contain

- Frontend: `src/main/java/.../ui/**`, `src/test/java/.../ui/**` except `Grade11ReviewContentTest`, `src/main/resources/com/aphs/classroomuprising/**`, `Main.java`, `MainEntryPointTest.java`, `docs/art-asset-spec.md`, `docs/midterm-audit-evidence.md`, `docs/backend-request-review-lectures.md` and this file.
- Shared with the backend: `README.md`, `docs/implementation-status.md` and `docs/class diagram.mmd`. Each frontend commit records them as they stood at that time, including any backend text already in them.
- Not included: backend core classes and tests, `demo.properties`, `Grade11ReviewContentTest`, `docs/Sprint1.md`, `docs/sequence diagram.mmd`, `docs/frontend-combat-handoff.md`, `build.gradle` and `.gitignore`. In this worktree they are staged (`git diff --cached --stat`) but not committed, as they stood in the main folder when this branch was made.

## For the backend: organising your commits

1. Make one commit per backend request in the same format: what was asked, the outcome, the conversation or tool used, and how the files were obtained. Set the author date to when that state existed (`GIT_AUTHOR_DATE`). Say so where a state can only be approximated.
2. Put backend and frontend commits in time order on one branch from `a90c3c3`, for example by cherry-picking both sets in order onto a new branch. Do not change the frontend commits' frontend files. For the shared docs, resolve each conflict to the file as it stood at that commit's time.
3. Check that the final tree equals the main folder's current files: `diff -r` against the main folder, excluding `build`, `.gradle`, `.idea`, `.claude` and `.git`.
4. Leave the main folder, its staged `Main.java` and the remote alone until the user decides; nothing here has been pushed.

Requests to the frontend still go through the user.
