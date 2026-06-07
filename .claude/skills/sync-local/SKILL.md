---
name: sync-local
description: Sync the local git repo to match GitHub after PRs are merged and their branches deleted. Switches to the main branch, fetches with prune, fast-forwards main, and deletes local branches whose upstream was deleted on the remote. Use after merging and deleting branches on GitHub, or whenever the user says "sync local", "/sync-local", or asks to clean up stale local branches.
---

# sync-local

Bring the local git repository in line with GitHub after one or more PRs have been merged and their branches deleted on the remote. This is a maintenance/git-hygiene task — it does **not** touch `_source/`, `_context/`, or any framework workflow, so the normal Proposal/Apply gate in `CLAUDE.md` does not apply.

## What this skill does

1. Detect the repository's default branch (the branch `origin/HEAD` points to — usually `main`).
2. Switch to that branch.
3. `git fetch --prune` — refresh remote knowledge and drop stale `remotes/origin/...` refs.
4. `git pull --ff-only` — fast-forward the default branch to the merged commits.
5. Find local branches whose upstream is marked **`gone`** (their remote branch was deleted) and force-delete them.
6. Report exactly what changed.

## Safety contract

- **Never** delete the default branch or the branch the user is currently on if it has no `gone` upstream.
- Only delete local branches whose upstream tracking ref is reported as `gone` by `git branch -vv`. That marker means the remote branch this local branch tracked has been deleted — the reliable signal that a merged-and-deleted feature branch is safe to remove. Do not delete branches that merely lack an upstream (those were never pushed and may be local work in progress).
- This skill discards nothing in the working tree on its own. If `git pull --ff-only` or `git checkout` fails because of uncommitted local changes, **stop and report** — do not auto-stash or auto-discard unless the user explicitly says to.
- Use `-D` (force) for the stale-branch deletes: branches merged via squash/merge-commit PRs are not "fully merged" from `-d`'s point of view, so `-d` would refuse them. The `gone` upstream check is what makes `-D` safe here.

## Procedure

Run this as a single sequence. First gather state:

```bash
# Default branch name (strip the "origin/" prefix)
DEFAULT_BRANCH=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##')
DEFAULT_BRANCH=${DEFAULT_BRANCH:-main}
echo "Default branch: $DEFAULT_BRANCH"
git status -sb
```

If `git status` shows uncommitted changes that would block checkout/pull, stop and ask the user how to proceed (keep, stash, or discard). Otherwise continue:

```bash
git checkout "$DEFAULT_BRANCH"
git fetch --prune
git pull --ff-only
```

Then identify and delete stale local branches (upstream `gone`):

```bash
# List local branches whose upstream was deleted on the remote
git branch -vv | grep ': gone]' | awk '{print $1}' | sed 's/^[*+] //'
```

Show that list to the user, then delete them:

```bash
git branch -vv | grep ': gone]' | awk '{print $1}' | sed 's/^[*+] //' | xargs -r git branch -D
```

Finally, confirm the result:

```bash
git branch -a
git status -sb
```

## Reporting

Summarize for the user:

- Which branch was checked out and whether `main` (or the default) was fast-forwarded (and by how many commits, if known).
- Which stale remote-tracking refs were pruned.
- Which local branches were deleted (by name).
- The final branch list.

If there were no stale branches to delete, say so plainly rather than implying work was done. If anything was skipped (e.g. uncommitted changes blocked the pull), state that clearly.
