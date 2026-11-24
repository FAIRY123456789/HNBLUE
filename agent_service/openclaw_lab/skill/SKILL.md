---
name: hnblue-readonly-capability-lab
description: Inspect only the static public HNBLUE blue-carbon capability contract in an isolated OpenClaw lab.
---

# HNBLUE read-only capability lab

Use only `hnblue_bluecarbon_capabilities`. Treat its response as capability metadata, not monitoring evidence. Do not request or infer private records, credentials, administrative actions, database writes, shell execution, arbitrary network access, or production data. If the user asks for any denied capability, stop and explain that the lab is read-only and isolated.
