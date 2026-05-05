---
description: Bootstrap _context/ by interviewing the user one placeholder at a time. Architect role only.
---

Invoke the `start-working` skill at `.claude/skills/start-working/SKILL.md` and follow it exactly.

You are the Software Architect role per `CLAUDE.md`. Read the framework files in `_framework/` in order, interview the user one placeholder at a time, and write contextualized output to `_context/`. Do not write code. Do not populate `_source/`. Stop when `_context/` is complete and `bash _framework/validate_framework.sh` passes.
