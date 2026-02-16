# SurvivalCore

Custom Minecraft server fork based on Paper 1.21.8 for the WeenieSMP network. Built for multi-core performance on 6-core servers with full Bukkit/Spigot/Paper plugin compatibility.

## What Is This?

SurvivalCore cherry-picks proven optimizations from Leaf, Gale, Lithium, and Pufferfish into a single maintainable Paper fork. Unlike Purpur (which adds hundreds of mostly-unused feature patches), SurvivalCore focuses exclusively on performance.

### Key Features

- **Async Entity Tracking** - Position/velocity/metadata broadcasts moved off main thread
- **Async Pathfinding** - A* calculations submitted to worker thread pool
- **Async Mob Spawning** - Spawn position calculation done async, entity creation stays on main thread
- **FastMath** - Trig lookup tables, bit-manipulation floor/ceil, fast inverse sqrt
- **SIMD Acceleration** - Java Vector API for batch distance/color calculations
- **Hopper Optimization** - Lithium-style inventory caching and skip-empty checks
- **Entity AI Improvements** - Distance-based tick frequency, goal selector throttling
- **Object Pooling** - Thread-local pools for BlockPos, Vec3, AABB
- **Performance Monitoring** - Per-tick timing breakdown and async pool utilization
- **Villager Lobotomize** - Disable AI for stuck villagers (from Purpur)

Everything is configurable via `config/survivalcore.yml` with sensible defaults and kill switches.

## Building

Requires Java 21 and Git.

```bash
# Clone
git clone https://github.com/JadenRazo/SurvivalCore.git
cd SurvivalCore

# Apply upstream Paper patches
./gradlew applyAllPatches

# Build the server JAR
./gradlew createMojmapPaperclipJar

# Output: build/libs/survivalcore-paperclip-*-mojmap.jar
```

### Development Workflow

```bash
# After modifying files in paper-server/src/:
./gradlew rebuildPatches

# To update Paper upstream:
./scripts/upstream-update.sh
./gradlew applyAllPatches
# Resolve any conflicts
./gradlew rebuildPatches
```

## JVM Flags

SurvivalCore is designed for ZGC Generational (Java 21+):

```bash
java \
  -Xms20G -Xmx20G \
  -XX:+UseZGC -XX:+ZGenerational \
  -XX:+AlwaysPreTouch \
  -XX:+DisableExplicitGC \
  -XX:+PerfDisableSharedMem \
  -XX:+UseStringDeduplication \
  --add-modules=jdk.incubator.vector \
  -jar survivalcore.jar nogui
```

The `--add-modules=jdk.incubator.vector` flag enables SIMD acceleration. Without it, SIMD operations fall back to scalar code automatically.

## Configuration

Generated on first run at `config/survivalcore.yml`. All optimizations can be independently toggled. Thread count of `0` means auto-detect based on available cores.

```yaml
async:
  entity-tracker:
    enabled: true
    max-threads: 0       # auto: cores/4
    compat-mode: true    # sync tracking for NPC plugins
  pathfinding:
    enabled: true
    max-threads: 0       # auto: cores/3
  mob-spawning:
    enabled: true

performance:
  simd:
    enabled: true
  fast-math:
    enabled: true
  hopper:
    optimized-inventory-caching: true
    skip-empty-check: true
    throttle-when-full: true
  entity-ai:
    inactive-goal-selector-throttle: true
    distance-based-tick-frequency: true
    brain-tick-batching: true
  memory:
    object-pooling: true

redstone:
  implementation: alternate-current

monitoring:
  enabled: true
  report-interval: 6000  # ticks (5 min)
```

## Thread Allocation (6 Cores)

| Core | Assignment |
|------|-----------|
| 0 | Main tick thread |
| 1 | Chunk system workers |
| 2 | Entity tracker pool |
| 3 | Pathfinding pool |
| 4 | Mob spawning + misc async |
| 5 | Netty I/O + GC threads |

## Plugin Compatibility

Full Bukkit/Spigot/Paper API compatibility. All existing plugins work without modification. The compat-mode flag for entity tracking ensures NPC plugins (Citizens, FancyNpcs) function correctly.

## License

Patches are licensed under the same terms as Paper (GPL-3.0). Upstream Paper/Minecraft code retains its original licensing.
