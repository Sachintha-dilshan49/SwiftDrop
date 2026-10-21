# Git Workflow

How the nine of us share one repository without breaking each other's work.

## 1. Branches

```
main      ●───────────────●──────────────●        releases only (demo-ready)
           \             / \            /
dev         ●──●──●──●──●───●──●──●──●─●          integration branch
               \  /  \  /       \    /
feature/…       ●●    ●●         ●●●               your work
```

| Branch | Purpose | Who writes to it |
|---|---|---|
| `main` | Stable, demo-ready code. Tagged releases. | Team lead, through a PR from `dev` |
| `dev` | Integration branch. Always builds and passes tests. | Everyone, only through PRs |
| `feature/*` | New work | One person |
| `fix/*` | Bug fixes on `dev` | One person |
| `docs/*`, `test/*`, `chore/*`, `ci/*` | Non-feature work | One person |
| `hotfix/*` | Urgent fix to `main` (e.g. right before the demo) | Team lead |

### Branch names

```
<type>/<scope>-<short-description>
```

- Lower case, words separated by hyphens, under ~50 characters.
- Include the issue number if there is one: `feature/12-parcel-status-history`.

Examples:

```
feature/parcel-status-history
feature/23-auth-google-login
fix/dispatch-courier-ownership-check
docs/architecture-sequence-diagrams
test/notification-webhook-retry
ci/jacoco-coverage-gate
chore/compose-healthchecks
```

## 2. Daily flow

```bash
# 1. start from the latest dev
git switch dev
git pull origin dev

# 2. create your branch
git switch -c feature/parcel-status-history

# 3. work, commit small steps
git add -p
git commit            # opens the .gitmessage template

# 4. keep up to date with dev (do this at least daily)
git fetch origin
git rebase origin/dev # fix conflicts, then: git rebase --continue

# 5. push
git push -u origin feature/parcel-status-history
# after a rebase on an already-pushed branch:
git push --force-with-lease

# 6. open a PR into dev on GitHub
```

Rules:

- Only rebase **your own** branch. Never rebase or force-push `dev` or `main`.
- Use `--force-with-lease`, never plain `--force`.
- If two people must work on one feature, agree who owns the branch, or split into two branches.

### One-time setup for every member

```bash
git config --global user.name  "Your Name"
git config --global user.email "the-email-you-use-on-github@example.com"
git config --global pull.rebase true
git config --global fetch.prune true
git config commit.template .gitmessage     # run inside the repo
```

## 3. Commit messages – Conventional Commits

```
<type>(<scope>): <subject>

[optional body: what and why]

[optional footer: Closes #12 / BREAKING CHANGE: ...]
```

### Types

| Type | Use for |
|---|---|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `test` | Adding or fixing tests |
| `refactor` | Code change with no behaviour change |
| `perf` | Performance improvement |
| `style` | Formatting only |
| `build` | Maven, npm, Dockerfiles, dependencies |
| `ci` | GitHub Actions |
| `chore` | Anything else (configs, cleanup) |
| `revert` | Reverting a previous commit |

### Scopes

`gateway` · `auth` · `pricing` · `parcel` · `dispatch` · `merchant` · `notification` · `ai` · `frontend` · `common` · `deploy` · `ci` · `docs`

### Subject rules

- Imperative mood: "add", not "added" or "adds".
- Lower case, no full stop, max 72 characters.
- Says what changes, not which file.

### Good and bad

| Good | Bad |
|---|---|
| `feat(parcel): add status history endpoint` | `update` |
| `fix(dispatch): block couriers from viewing others' deliveries` | `fixed bug` |
| `test(notification): cover webhook retry after 500 response` | `tests` |
| `build(deploy): add healthchecks to compose services` | `docker stuff` |
| `docs(docs): add courier status sequence diagram` | `Final final docs` |

Breaking change example:

```
feat(common)!: rename toStatus to newStatus in ParcelStatusChanged

BREAKING CHANGE: event version bumped to 2; all consumers must update.
```

## 4. Pull requests

### Before you open one

- [ ] Rebased on the latest `origin/dev`
- [ ] Tests pass locally for the modules you touched
- [ ] New code has tests (including an authorization test for new endpoints)
- [ ] No secrets, `.env` files or personal data in the diff
- [ ] New environment variables added to `.env.example`
- [ ] Public API changes reflected in `docs/architecture.md`

### PR title

Same format as a commit: `feat(parcel): add status history endpoint`. With squash merging, the PR title becomes the commit on `dev`.

### Size

Aim for under ~400 changed lines. Large features are split into several PRs (e.g. entity + migration first, then service, then controller).

### Review

- Every PR needs **1 approval**. Request your backup buddy (see `team-and-ownership.md`).
- PRs that touch another owner's module also need that owner's approval (CODEOWNERS does this automatically).
- Reviewers respond within 24 hours. If you can't, say so in the group.
- Review for: correctness, tests, security (auth checks, no PII in logs), consistency with the docs.
- Use GitHub suggestions for small fixes; prefix optional comments with `nit:`.
- The author resolves conversations after addressing them and re-requests review.

### Merging

- **Squash and merge** into `dev`, using the PR title as the commit message.
- Only merge when CI is green and approval is in.
- Delete the branch after merging.

## 5. Releases to `main`

- The team lead opens a PR `dev → main` at each milestone (end of each sprint, and before the demo).
- Merge with **Create a merge commit** (keeps history of what went into each release).
- Tag the release: `v0.1.0`, `v0.2.0`, … `v1.0.0` for the final submission.

```bash
git switch main && git pull
git tag -a v0.1.0 -m "Sprint 1: auth, parcel create, compose"
git push origin v0.1.0
```

### Hotfix

```bash
git switch main && git pull
git switch -c hotfix/gateway-cors-origin
# fix, commit, PR into main; after merge, PR main → dev (or cherry-pick) so dev has the fix too
```

## 6. GitHub settings (team lead sets these up)

**Branch protection for `main` and `dev`** (Settings → Branches → Add rule, or Rulesets):

- Require a pull request before merging
- Require 1 approval; dismiss stale approvals when new commits are pushed
- Require review from Code Owners
- Require status checks to pass (the CI workflow) and branches to be up to date
- Require conversation resolution before merging
- Block force pushes and deletions
- `dev` and `main`: restrict who can push (nobody directly)

**Repository settings:**

- Allow squash merging (default message: PR title), allow merge commits (for releases), disable rebase merging
- Automatically delete head branches
- Default branch: `dev` (so PRs target it automatically)

**Labels:** `backend`, `frontend`, `devops`, `docs`, `test`, `bug`, `blocked`, `good-first-issue`, plus one per service.

**Project board:** GitHub Projects with columns Backlog → To do → In progress → In review → Done. One issue per task; link PRs with `Closes #n`.

## 7. If something goes wrong

| Problem | Fix |
|---|---|
| Committed to `dev` locally by mistake | `git switch -c feature/my-work` then `git switch dev && git reset --hard origin/dev` |
| Rebase conflict | Fix files, `git add <file>`, `git rebase --continue`; or `git rebase --abort` to start over |
| Pushed a secret | Tell the team lead immediately. Rotate the secret first; removing it from history is second. |
| Need to undo a merged PR | `git revert <commit>` on a new branch and open a PR. Never rewrite `dev`. |
| Wrong commit message on your own unpushed commit | `git commit --amend` |
