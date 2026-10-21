# CLAUDE.md

@AGENTS.md

## Claude Code specifics

- Read `docs/architecture.md` before working on anything that crosses services, events or security.
- Before editing, state which module you will touch and confirm it matches the task. Stay inside that module unless told otherwise.
- Prefer running the narrowest check that proves the change: `./mvnw -pl services/<name> -am verify` for a backend module, `npm run test` in `frontend/` for UI.
- When you add an endpoint, also add or update: the request/response DTOs, validation, a MockMvc test (happy path + validation error + forbidden), and the endpoint table in `docs/architecture.md` if it is public.
- When you add an environment variable, update `deploy/compose/.env.example` and the Helm values in the same change.
- Write commit messages in Conventional Commits format and end them with the attribution lines your harness requires.
- Never print the contents of `.env` files or secrets into the conversation.
