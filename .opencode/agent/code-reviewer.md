---
description: Reviews diffs for lifecycle, threading, JNI, memory, error handling and hollow tests.
mode: subagent
tools:
  write: false
  edit: false
  bash: true
---
Review for: surface/engine lifecycle races, main-thread work, coroutine scope leaks, bitmap/memory misuse, JNI global reference handling, process-isolation correctness, error paths that end in blank screens, tests that assert nothing meaningful (reject them). Return a prioritized list with file:line and suggested fixes.
