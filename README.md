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

## Testing

Unit tests and stress tests validate every subsystem. Run them with:

```bash
./gradlew :survivalcore-server:test
```

### Stress Test Suite

The `StressTest` class hammers each optimization subsystem under extreme load to catch regressions and verify performance at scale. All tests include timing assertions to flag unexpected slowdowns.

| Test | What It Does | Scale |
|------|-------------|-------|
| EntityTickBudget: Massive Load | Budget checks under heavy entity count | 10,000 entities/tick |
| EntityTickBudget: Index Rotation | Start index wrapping at high speed | 100,000 rotations |
| TNTBatcher: Clustered Explosions | Batch merging with nearby + scattered TNT | 10,000 TNT (5k clustered, 5k spread) |
| TNTBatcher: Large Area Spread | Batching across a 10km x 10km area | 10,000 TNT |
| TickCoalescer: Position Tracking | Duplicate tick detection and blocking | 100,000 unique positions |
| TickCoalescer: Reset Cycles | Repeated fill/reset/refill under load | 100 cycles, 1,000 positions each |
| EntityCleanup: Escalating Counts | Threshold transitions from 0 to 50k entities | 50,000 entity count sweep |
| EntityCleanup: Type Checks | Protected/despawnable/force-despawn lookups | 600,000 type checks |
| FarmDetector: Chunk Simulation | Varying entity density across many chunks | 500 chunks, 0-1000 entities each |
| FarmDetector: Tick Throttling | shouldTickEntity under critical-density chunks | 1,000,000 tick checks |
| ObserverDebounce: Redstone Clock | Simulates a lag machine with rapid observer fires | 50,000 observers over 20 ticks |
| ObserverDebounce: Cleanup | Stale entry cleanup under heavy tracking | 10,000 tracked observers |
| ObjectPool: Multi-Threaded | Concurrent acquire/release across threads | 8 threads, 100k ops each |
| ObjectPool: Scoped Resources | Try-with-resources lifecycle stress | 10,000 scoped acquisitions |
| FastMath: Trig Operations | Sin/cos accuracy at scale | 1,000,000 calculations |
| FastMath: Distance Calculations | 3D distanceSq with random coordinates | 1,000,000 calculations |
| FastMath: Floor/Ceil Sweep | Integer rounding correctness | 1,000,000 operations |
| FastMath: Sqrt/Atan2 Accuracy | Approximation error bounds | 1,000,000 calculations |
| PerformanceMonitor: Timing Records | Recording entries across all categories | 1,000,000 records |
| PerformanceMonitor: Counter Updates | Incrementing all counter types | 800,000 increments |
| PerformanceMonitor: Mixed Workload | Interleaved timing + counter operations | 100,000 iterations |
| Integrated: All Subsystems | Full tick simulation with every system active | 1,000 simulated game ticks |

## Plugin Compatibility

Full Bukkit/Spigot/Paper API compatibility. All existing plugins work without modification. The compat-mode flag for entity tracking ensures NPC plugins (Citizens, FancyNpcs) function correctly.

## License

Patches are licensed under the same terms as Paper (GPL-3.0). Upstream Paper/Minecraft code retains its original licensing.
