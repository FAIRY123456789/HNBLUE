# HNBLUE OpenClaw read-only lab plugin

This package is intentionally isolated from HNBLUE production data and credentials. Its only tool returns static public capability metadata and explicit denied capability names. It does not connect to MySQL, Java, files, the shell, or arbitrary URLs.

The source targets the current `defineToolPlugin` contract, but host validation is blocked on this machine: Node is 20.19.5 while current OpenClaw tool plugins require Node 24.16+ or 26.1+, and the `openclaw` CLI is absent. `npm run test:core` remains a dependency-free safety test; it is not evidence that the OpenClaw host loaded the plugin.
