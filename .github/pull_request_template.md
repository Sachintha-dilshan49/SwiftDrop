## What and why

<!-- One or two sentences. Link the issue. -->

Closes #

## Module(s) touched

- [ ] gateway  - [ ] auth  - [ ] pricing  - [ ] parcel  - [ ] dispatch
- [ ] merchant  - [ ] notification  - [ ] ai  - [ ] frontend  - [ ] common
- [ ] deploy / ci  - [ ] docs

## How to test

<!-- Steps or curl commands a reviewer can run. -->

## Checklist

- [ ] Branch rebased on latest `dev`
- [ ] PR title follows Conventional Commits (`feat(parcel): ...`)
- [ ] Tests added or updated (including 401/403 cases for new endpoints)
- [ ] Tests pass locally
- [ ] No secrets, `.env` files or personal data in code, logs or errors
- [ ] New env vars added to `deploy/compose/.env.example`
- [ ] Public API or event changes reflected in `docs/architecture.md`
- [ ] Screenshots attached for UI changes (desktop and phone width)

## Notes for the reviewer

<!-- Anything you are unsure about, or changes in another owner's module. -->
