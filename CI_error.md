NemoPill — CI Failure and Branch-Protection Mismatch Report
Repo: irodriguez-io/NemoPill (public, solo developer, user is admin)
Branch: task-007-design → PR #1 against main
PR URL: https://github.com/irodriguez-io/NemoPill/pull/1
Date: 2026-05-27

1. CI Failure — root cause
The required status check 1 · Setup fails at the "Validate Gradle Wrapper checksum" step (gradle/actions/wrapper-validation@v3):


##[error]Gradle Wrapper Validation Failed!
✗ Other validation errors:
  Expected to find at least 1 Gradle Wrapper JARs but got only 0
Checks 2–6 (Lint, Secret Scan, Framework Validate, Architecture Conformance, Build) are SKIPPED because they declare needs: setup in .github/workflows/ — they don't run independently.

Why it fails
The Gradle wrapper in _source/gradle/wrapper/ is incomplete. A correct Gradle wrapper has four artifacts; this repo has only three:

File	Present?	Purpose
_source/gradlew	✅	POSIX launcher script
_source/gradlew.bat	✅	Windows launcher
_source/gradle/wrapper/gradle-wrapper.properties	✅	Declares Gradle 8.10.2 distribution URL
_source/gradle/wrapper/gradle-wrapper.jar	❌ MISSING	The bootstrap JAR launchers exec
Verified via git ls-files _source/gradle/ — only libs.versions.toml and gradle-wrapper.properties are tracked. The JAR was never committed (not local either; not a .gitignore issue — .gitignore only excludes .DS_Store and .claude/settings.local.json).

Without gradle-wrapper.jar:

The validation action correctly reports 0 wrappers found.
Even if validation passed, the next step (./gradlew help --configuration-cache) would fail because gradlew cannot bootstrap without the JAR.
Declared Gradle version
_source/gradle/wrapper/gradle-wrapper.properties:3:


distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
So the regenerated JAR must match Gradle 8.10.2.

Fix options
Local Gradle install (preferred): cd _source && gradle wrapper --gradle-version 8.10.2. This regenerates all four wrapper files. Commit only the JAR (or all four to be safe).
No local Gradle: download the official JAR — curl -L -o _source/gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar — then commit. wrapper-validation@v3 checksums the JAR against a known-good registry, so the upstream copy is the right source.
Android Studio: any "Sync Project with Gradle Files" action regenerates the wrapper.
Verification before pushing again: cd _source && ./gradlew --version should print Gradle 8.10.2 cleanly.

2. Branch-protection rule mismatch — the "PR review" gate
Current protection on main (from GET /repos/.../branches/main/protection):

Setting	Value	Notes
required_pull_request_reviews.required_approving_review_count	1	The blocker for solo dev
required_pull_request_reviews.dismiss_stale_reviews	true	Approvals dismissed on new pushes
required_pull_request_reviews.require_code_owner_reviews	false	
required_status_checks.contexts	6 jobs (Setup → Build)	All listed as required
required_status_checks.strict	true	PR must be up-to-date with main
enforce_admins	true	Admins cannot bypass — even with --admin flag
allow_force_pushes	false	
allow_deletions	false	
required_signatures.enabled	false	
Why this is illogical for a solo developer
GitHub doesn't let a PR author approve their own PR. With required_approving_review_count: 1 and only one human on the repo, the PR is structurally unmergeable without one of:

A second GitHub account approving (bot or alt).
Lowering the required review count to 0.
Disabling required reviews entirely.
Disabling enforce_admins so the admin can override (still leaves a "bypassed" marker on the PR).
Because enforce_admins: true, gh pr merge --admin will not work — the admin override is disabled.

Reasonable solo-dev configurations (pick one, document the trade-off)
Option	Change	Effect	Trade-off
A. Keep CI gate, drop human-review gate (recommended for solo work)	required_approving_review_count: 0, keep all 6 status checks	PRs merge as soon as CI passes	Loses the "force me to look at my own diff in PR view" prompt; mitigated by the rich CI gate
B. Self-review via second account	Add a personal alt or a bot (e.g. nemopill-bot) as collaborator, approve from it	Keeps formal review gate	Sock-puppet smell; extra account management; bot tokens to manage
C. Loosen admin enforcement	enforce_admins: false, keep required_approving_review_count: 1	Admin can gh pr merge --admin despite no approval	Bypass shows in PR history; defeats the rule on every PR — better to remove the rule than bypass it
D. Branch ruleset with bypass actor	Migrate from classic protection to a Ruleset and add your user as a "bypass actor" with pull_request exception	Reviews still required for any future collaborator; admin merges cleanly	More config surface; only available on Rulesets, not classic protection
The repo currently uses classic branch protection (path .../branches/main/protection), not a Ruleset, so option D would also require migrating.

For a solo dev with a 6-stage CI gate and dismiss_stale_reviews: true, option A is the cleanest: the CI gate is the real safety net; the review gate is decorative.

To apply option A via gh CLI:


gh api -X PATCH repos/irodriguez-io/NemoPill/branches/main/protection/required_pull_request_reviews \
  -f required_approving_review_count=0
Or via UI: Settings → Branches → main → Edit → uncheck "Require a pull request before merging" OR set required approvals to 0.

3. Current local/remote state
Local main is at d1637a1 (in sync with origin/main).
Local branch task-007-design is at 48567b1 and pushed; tracking origin/task-007-design.
PR #1 is open, mergeable: MERGEABLE, mergeStateStatus: BLOCKED (CI red + missing review).
Working tree clean.
gh CLI v2.92.0 installed; auth scopes on token: admin:public_key, gist, read:org, repo. No admin:repo_hook or workflow scopes, but repo is sufficient for the protection-PATCH call above.
4. Recommended plan to hand off
Fix CI first — regenerate _source/gradle/wrapper/gradle-wrapper.jar (Gradle 8.10.2), commit on task-007-design, push. CI should go green.
Decide on branch protection — choose option A/B/C/D above. If A, run the gh api -X PATCH command before re-checking PR #1.
Merge PR #1 — once CI is green and review requirement is resolved.
Clean up — delete task-007-design branch, pull main locally.
Document the decision — per CLAUDE.md, append an ADR to _context/09_decision_log.md capturing the branch-protection choice. Update _context/12_environments_and_devops.md if it documents the policy (it's referenced in the workflow header).
