# Handoff prompt — NemoPill CI still failing on PR #1 (`1 · Setup`)

Copy everything below the line into a fresh session.

---

You are picking up a CI debugging effort on the **NemoPill** repo (the folder is connected/mounted). Follow `CLAUDE.md`. The work below is a **Human-gatekeeper-approved hot-fix** (approved by Isidro Rodriguez, repo admin) executed deliberately out-of-sync with the active T-007 task packet scope; it is documented in `_context/09_decision_log.md` as **ADR-076**. Read ADR-076 first.

## Repo / PR facts
- Repo: `irodriguez-io/NemoPill` (public, solo dev, admin = the user). Remote is SSH.
- Branch: `task-007-design` → PR #1 against `main`. PR: https://github.com/irodriguez-io/NemoPill/pull/1
- CI workflow: `.github/workflows/ci.yml`. Six required status checks: `1 · Setup`, `2 · Lint`, `3 · Secret Scan`, `4 · Framework Validate`, `5 · Architecture Conformance`, `6 · Build`. Checks 2–6 declare `needs: setup`, so they SKIP while Setup is red.
- Gradle is pinned to **8.10.2** in `_source/gradle/wrapper/gradle-wrapper.properties`. JDK 17 (Temurin) on `ubuntu-latest` runners. The Gradle project lives under `_source/` (`GRADLE_DIR: _source`).

## The original problem
PR #1 was BLOCKED for two reasons: (1) CI red at `1 · Setup`, and (2) a solo-dev branch-protection deadlock (`required_approving_review_count: 1` + `enforce_admins: true` = unmergeable; author can't approve own PR).

## What was changed during the prior session (all pushed)
1. **Added `_source/gradle/wrapper/gradle-wrapper.jar`** — it had never been committed (only `.properties` + `libs.versions.toml` were tracked), so wrapper-validation reported "0 wrappers". The committed JAR is the official Gradle 8.10.2 wrapper; SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`, **cross-checked against Gradle's published `gradle-8.10.2-wrapper.jar.sha256` and matches**. Commit `d6d405a`.
2. **Fixed `_source/gradlew` line 100** — it was corrupted to `CLASSPATH="\$APP_HOME/gradle/wrapper/gradle-wrapper.jar"` (escaped `$`, so `$APP_HOME` never expanded → `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`). Restored to canonical `CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar`. Commit `a072bd1`.
3. **Replaced `gradle/actions/wrapper-validation@v3`** in `ci.yml` with an offline pinned-SHA-256 check (`sha256sum -c`), because the action failed on ~3 of 4 runs with intermittent `connect ETIMEDOUT` to its Cloudflare-fronted checksum endpoint (Gradle's status page showed 100% uptime, so it appeared to be a runner→Cloudflare issue). A comment in `ci.yml` warns to update the hash on any Gradle upgrade.
4. **Branch protection → Option A**: `gh api -X PATCH .../branches/main/protection/required_pull_request_reviews -F required_approving_review_count=0`. Confirmed applied. `enforce_admins` left `true`; all six status checks retained. (Use `-F` not `-f` — the field needs an integer.)

## Current state — the thing to solve
After all of the above, `1 · Setup` STILL fails, but the **run duration jumped from ~14s to ~1m20s**. The fast 14s failures were the wrapper-validation network timeout; 1m20s means it now gets past validation and dies later — most likely at the **"Prime Gradle cache"** step (`./gradlew help --configuration-cache`), which downloads the full Gradle 8.10.2 distribution and runs a task. The exact failure has NOT yet been read.

**Your first action:** pull the latest failed log and find the real error before theorizing:
```bash
RID="$(gh run list -b task-007-design -L1 --json databaseId -q '.[0].databaseId')"
gh run view "$RID" --log-failed | tail -60
```

## Challenge these assumptions from the prior session
The prior session fixed things one layer at a time by pushing to CI and reading the next error. Question whether that loop and its premises were right:

1. **"The network failures are purely transient/external."** Re-examine. Could the runner have a systemic egress restriction (self-hosted runner? org network policy? IPv6-only failures + IPv4 timeouts)? If general egress is constrained, "Prime Gradle cache" (distribution download) and Lint/Build (dependency downloads) may fail the same way — meaning the real fix is network/runner config, not per-step patches. Check what `runs-on` actually resolves to and whether other steps make external calls.
2. **"This project has ever built successfully."** It may never have. The errors could be a sequence of real, latent build-config problems (configuration-cache incompatibility with the Android Gradle Plugin, missing Android SDK in the Setup job, module/settings issues). Don't assume each error is the "last" one.
3. **"Fix-forward via CI is the right feedback loop."** It is slow and has burned many runs. Strongly consider **reproducing locally**: install Temurin 17 (`brew install --cask temurin@17`), then `cd _source && ./gradlew help --configuration-cache`. This surfaces the real errors in seconds without CI roundtrips, and tests the exact command Setup runs. Recommend this early.
4. **"`--configuration-cache` on `help` is benign."** Verify it's actually compatible with this project's plugin set; it can fail configuration-cache validation independent of network.
5. **"The pinned-checksum swap and Option A protection were the right calls."** They were approved, but if a reviewer challenges them, ADR-076 has the rationale — confirm it still holds.
6. **"The wrapper JAR / gradlew are now fully correct."** Likely yes (checksum matched official; gradlew line verified), but re-confirm if the new error is wrapper-related.

## Governance reminders (from CLAUDE.md)
- Proposal Mode is default. This hot-fix is gatekeeper-approved; keep recording durable decisions in ADR-076 / `_context/09_decision_log.md` and append a `_context/handoff_log.md` entry at turn end.
- Do not push, merge, or run destructive git ops without explicit instruction. The user runs git/`gh` in their own terminal (the agent sandbox has no `gh`, no push key, and a network allowlist that blocks `services.gradle.org` and `raw.githubusercontent.com` — so the agent cannot download the distribution or push; hand the user exact commands).
- A stale `.git/index.lock` recurred when the agent ran `git status` in the sandbox; avoid running git in the sandbox. If it appears, the user clears it with `rm -f .git/index.lock`.
- Goal: `1 · Setup` green → checks 2–6 run → `gh pr merge 1 --squash --delete-branch`. Then T-007/M-001 close steps in the packet (add the two new CI checks `unit-integration-ui-snapshot` and `coverage` to branch protection; M-001 milestone sign-off).

Start by reading ADR-076 and the latest failed log, then tell me what's actually failing and challenge whichever assumption above is most likely wrong.
