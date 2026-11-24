# OpenClaw Lab status — 2026-10-19 RC2

## Actually executed

- Isolated runtime: Node.js `26.12.0` and OpenClaw `2026.10.5` under the RC2 directory; the system Node.js `20.19.5` was not replaced.
- Plugin: TypeScript build passed; Node core test passed `1/1`; runtime `plugins inspect` reports version `0.1.0`, `enabled=true`, `activated=true`, `status=loaded`, dependency set complete, and accepted tool contract `hnblue_bluecarbon_capabilities`.
- Model/provider: local Ollama `0.40.5`, `qwen3:1.7b` (`2.0B`, `Q4_K_M`, CPU only, `size_vram=0`) on loopback.
- Session `hnblue-rc2-tool-003`, run `40cb7163-0335-4ed5-9ace-b32f3a2a7ac1`: OpenClaw asked the model to use the plugin; the model emitted the exact tool call with `{}`; the handler returned `success=true`, `HNBLUE_CAPABILITY_METADATA_V1`, `synthetic=false`, four read-only capabilities, and five explicit denials. The first post-tool model turn had no deliverable text and was classified `non_deliverable_terminal_turn`; the same run subsequently persisted a short final assistant answer.

## Stability limit found

- Session `hnblue-rc2-tool-004`, run `54fc1c1b-e1c5-4c41-84d3-1a9404489947`, tested `reasoning=false`. The small model repeatedly invented arguments for the zero-argument tool; JSON Schema correctly rejected every malformed call. The command was stopped after more than six minutes and the configuration was reverted to `reasoning=true`.
- Therefore plugin loading and one real model-driven tool execution are **IMPLEMENTED_AND_EXECUTED**, while unattended native terminal reliability with `qwen3:1.7b` on this host is **PARTIAL / NOT RELEASE READY**.

## Boundary

- The lab tool returns static public capability metadata only. It cannot access a database, accounts, shell, arbitrary URLs, production data, or private government material.
- No production database, real account, online deployment, internal-data export, Git clone, or remote push was used.

Official references checked on 2026-10-19:

- https://docs.openclaw.ai/install/node
- https://docs.openclaw.ai/plugins/tool-plugins
- https://docs.openclaw.ai/providers/ollama
- https://docs.openclaw.ai/tools/tool-search
