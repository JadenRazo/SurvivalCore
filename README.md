# SurvivalCore

> **Status: archived experiment.** This repository is not a supported Minecraft server distribution. Its current patch set has no verified build, release artifact, benchmark suite, plugin-compatibility matrix, or production operating record.

SurvivalCore is an experimental Paper 1.21.8 patch set exploring server performance, administrative tooling, and configurable survival mechanics. It is preserved as a code-study artifact.

## What is in this repository

- A Gradle and paperweight patcher configuration pinned to one Paper commit
- A queue of server build and Minecraft source patches under `survivalcore-server/`
- An API build patch under `survivalcore-api/`
- Parallel Java source snapshots under `sources/`; these are review aids, not a separate wired source set
- Experimental components for entity tick budgets, pathfinding and task executors, redstone throttling, monitoring, configuration, administrative commands, and quality-of-life behavior
- Scripts intended to apply the patch queue, build a Paperclip JAR, and update the pinned upstream reference

The presence of a patch or class shows an implementation attempt. It does not establish correctness, safety, performance, or compatibility.

## What is not established

This repository does **not** provide evidence for claims that the earlier README made:

- no project-owned unit or stress tests are committed
- no repeatable benchmarks, raw results, hardware profile, or baseline comparison are committed
- no plugin compatibility suite or tested-plugin matrix is committed
- no tagged release or checksum is published from this tree
- no current CI system validates the patch queue or build
- no load, soak, corruption-recovery, or thread-safety results are available
- no provenance record demonstrates that implementations were directly ported from the projects previously named in the README

Accordingly, this project makes no performance-improvement or “full compatibility” claim.

## Intended patch flow

The build files target Java 21, Gradle 8.14.5, paperweight `2.0.0-SNAPSHOT`, Minecraft 1.21.8, and the Paper commit in `gradle.properties`.

The original intended workflow was:

```bash
./gradlew applyAllPatches
./gradlew createMojmapPaperclipJar
```

These commands require network access and upstream artifacts. They are documented to explain the repository structure, not as a current build guarantee. The pinned Paper commit, snapshot plugin, patch context, and dependency repositories may have drifted.

If studying the patch queue, review these locations first:

```text
build.gradle.kts
gradle.properties
survivalcore-server/build.gradle.kts.patch
survivalcore-server/minecraft-patches/
survivalcore-api/build.gradle.kts.patch
```

## Risk notes

Minecraft and Paper APIs frequently assume ownership by the main server thread. Moving pathfinding, entity tracking, chunk work, spawning, or mutable game state to executors can introduce races, stale reads, deadlocks, ordering changes, world corruption, and plugin-visible behavioral differences.

Before anyone adapts this work, a new maintained fork should:

1. rebase each patch against a supported Paper version and document its provenance
2. establish a clean, reproducible build from a fresh clone
3. add focused concurrency and correctness tests before enabling any async behavior
4. run controlled benchmarks against unmodified Paper with raw data and an explicit methodology
5. run representative plugin, restart, crash-recovery, and long-duration soak tests
6. ship experimental features disabled by default with measured rollback paths

Do not run this patch set against a valued world or public server without independent review, backups, restore testing, and acceptance of data-loss risk.

## Why it was archived

The project’s public claims outpaced its evidence and maintenance. Archiving preserves the implementation work without presenting it as a reliable product. A future revival should start from a supported upstream version and treat reproducibility, correctness, and evidence as release gates.

## Security

See [SECURITY.md](SECURITY.md). Do not publish server credentials, world data, player data, or exploit details in an issue.

## License

The repository is distributed under GPL-3.0. Paper, Minecraft, and other upstream material retain their respective rights and licenses. Review provenance and upstream licensing before redistributing a derived build.
