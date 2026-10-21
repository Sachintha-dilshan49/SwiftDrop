# Contributing

The full guide is [`docs/git-workflow.md`](docs/git-workflow.md). The short version:

1. Pick or create an issue and assign it to yourself.
2. Update `dev` and branch from it:
   ```bash
   git switch dev && git pull
   git switch -c feature/parcel-status-history
   ```
3. Commit with Conventional Commits:
   ```
   feat(parcel): add status history endpoint
   ```
4. Before opening the PR, bring your branch up to date and run the tests:
   ```bash
   git fetch origin && git rebase origin/dev
   ./mvnw -pl services/parcel-service -am verify
   ```
5. Push and open a PR into `dev`. Fill in the template, link the issue (`Closes #12`), request your backup buddy.
6. Merge with **Squash and merge** once CI is green and one approval is in. Delete the branch.

Nobody pushes to `main` or `dev` directly. Never commit `.env` files or secrets.

Post your update in the WhatsApp group every Monday and Thursday: done / next / blocked.
