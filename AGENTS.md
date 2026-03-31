# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 Forge mod using **Architectury** framework (Forge-only).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platform**: Forge only (Fabric disabled in `gradle.properties`)
- **Mappings**: Mojang Official (`officialMojangMappings()`)

## Build Commands

```bash
./gradlew build                              # Build mod JAR
./gradlew clean build                        # Clean and rebuild
./gradlew :forge:build                       # Build Forge module only
./gradlew :forge:runClient                   # Run Minecraft client
./gradlew :forge:runServer                   # Run server (if configured)
```

**No lint/checkstyle/spotbugs/PMD is configured.** No static analysis plugins exist.

**No test suite exists.** To add tests:
1. Create `forge/src/test/java/com/example/exile_overlay/`
2. Run all: `./gradlew :forge:test`
3. Run single test: `./gradlew :forge:test --tests "com.example.exile_overlay.SomeTest"`
4. Run single method: `./gradlew :forge:test --tests "com.example.exile_overlay.SomeTest.testMethod"`

## Project Structure

All source code lives in `forge/src/main/java/com/example/exile_overlay/` (the `common` module is empty).

```
forge/src/main/java/com/example/exile_overlay/
├── api/                    # Public interfaces & MOD compatibility layer
│   ├── IModDataProvider.java       # Core provider interface for RPG MOD data
│   ├── ModDataProviderRegistry.java # Static registry entry point
│   ├── Result.java                 # Functional error handling (Success/Failure)
│   ├── CircuitBreaker.java         # Cascade failure prevention
│   ├── UnifiedCache.java           # Frame-based cache with DataType TTL
│   ├── RenderContext.java          # Immutable render context (Builder pattern)
│   ├── PooledRenderContext.java    # Mutable context for object pooling
│   ├── RenderContextPool.java      # acquire()/release() object pool
│   └── MethodHandlesUtil.java      # Cached MethodHandle reflection
├── client/
│   ├── config/             # HUD position, anchor, config screens, ModMenu integration
│   ├── damage/             # Damage popup system (singleton: DamagePopupManager)
│   ├── favorite/           # Favorite item system
│   └── render/             # HUD rendering pipeline
│       ├── HudRenderManager.java   # Central coordinator (singleton)
│       ├── RenderPipelineImpl.java # Pipeline: layer + priority sorting
│       ├── orb/            # Energy orb rendering (OrbRegistry, OrbRenderer)
│       ├── entity/         # Entity health bars
│       ├── vanilla/        # Vanilla HUD overlays (air, food)
│       └── effect/         # Buff overlays
├── forge/
│   ├── ExampleModForge.java       # Forge @Mod entry point
│   ├── client/             # Forge client handlers (FMLClientSetupEvent, keybinds)
│   ├── mixin/              # Forge-specific mixins (M&S integration, remap=false)
│   └── event/              # Server tick event handlers
├── mixin/                  # Common mixins (vanilla targets, remap=true)
└── util/                   # Inventory sorting, Lootr/M&S helpers
```

## Code Map

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `HudRenderManager` | Class (singleton) | `client/render/` | Central HUD render coordinator |
| `ModDataProviderRegistry` | Class (static) | `api/` | Entry point for MOD data access |
| `DamagePopupManager` | Class (singleton) | `client/damage/` | Damage number popup controller |
| `UnifiedCache` | Class (singleton) | `api/` | Frame-based cache with per-DataType TTL |
| `IHudRenderer` | Interface | `api/` | HUD element contract (render, position, drag, scale) |
| `IRenderCommand` | Interface | `api/` | Render command (execute, layer, priority) |
| `IRenderPipeline` | Interface | `api/` | Command registration and layer-based execution |
| `IModDataProvider` | Interface | `api/` | MOD data provider contract |
| `IDataSource` | Interface | `api/` | Lightweight data source (getValue, isAvailable) |
| `Result<T,E>` | Class | `api/` | Functional error handling (map, flatMap, match, tryCatch) |
| `RenderLayer` | Enum | `api/` | BACKGROUND, FILL, FRAME, OVERLAY, TEXT, DEBUG |
| `UpdateFrequency` | Enum | `api/` | CRITICAL(0), NORMAL(15f), SLOW(60f), STATIC(300f) ticks |
| `ExampleModForge` | Class | `forge/` | Forge entry point |

## Code Style

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line endings**: LF
- **Max line length**: 120 characters
- **Braces**: Same line (K&R style)
- **No wildcard imports**
- **Blank lines**: 1 between methods/sections, no double-blank-lines
- **Section dividers**: `// ========== Section Name ==========` in larger classes

### Naming Conventions
- **Classes**: `PascalCase`
- **Methods/Fields**: `camelCase`
- **Constants**: `SCREAMING_SNAKE_CASE`
- **Enum values**: `SCREAMING_SNAKE_CASE`
- **Mixin unique fields**: `exileOverlay$fieldName` prefix
- **Mixin unique methods**: `exileOverlay$methodName` prefix
- **Resource locations**: `snake_case` (e.g., `textures/gui/orb.png`)
- **Logger names**: Mixins use `"exile_overlay/ClassName"`, others use `ClassName.class` or `LogUtils.getLogger()`

### Import Order (actual convention)
```java
// 1. Project imports
import com.example.exile_overlay.api.*;
// 2. Minecraft
import net.minecraft.client.*;
// 3. Forge
import net.minecraftforge.*;
// 4. Third-party (Architectury, SpongePowered Mixin)
import dev.architectury.*;
import org.spongepowered.asm.*;
// 5. Logging
import org.slf4j.*;
import com.mojang.logging.LogUtils;
// 6. Java standard library
import java.util.*;
```

### Java 17+ Features
- Switch expressions: `return switch (x) { case "A" -> 1; default -> 0; };`
- Pattern matching: `if (entity instanceof LivingEntity living) { ... }`
- Records: `HudError`, `CacheStats`, `StateSnapshot`
- No raw types; use generic type parameters
- `var` used sparingly

### Comments & Javadoc
- Javadoc on all public interfaces and key methods, written in **Japanese**
- Design rationale tags: `【設計思想】`, `【責任】`, `【パフォーマンス最適化】`, `【スレッド安全性】`
- Inline comments for non-obvious logic only
- No unnecessary comments on self-explanatory code

## Architecture Rules

1. **Common-first**: Platform-agnostic code in reusable classes (though currently all code is in forge module)
2. **Singleton pattern**: `HudRenderManager`, `DamagePopupManager`, `UnifiedCache` use `getInstance()`
3. **Registry pattern**: Register via `HudRenderManager.registerCommand()`, `ModDataProviderRegistry.register()`, `OrbRegistry`
4. **Result<T,E>**: Functional error handling instead of throwing exceptions
5. **Command pattern**: `IRenderCommand.execute()` with layer/priority sorting via `IRenderPipeline`
6. **Builder pattern**: `RenderContext.Builder`, `ModDataProviderBuilder`
7. **Dependency inversion**: All renderable elements implement `IHudRenderer`/`IRenderCommand`; data via `IModDataProvider`/`IDataSource`
8. **Object pooling**: `RenderContextPool.acquire()`/`release()` for zero-allocation render paths

## Mixin Rules

- Common mixins: `com.example.exile_overlay.mixin` package (vanilla targets, `remap=true`)
- Forge mixins: `com.example.exile_overlay.forge.mixin` package (external MOD targets, `remap=false`)
- Use `@Inject` at `HEAD` or `TAIL`; **never `@Overwrite`**
- Wrap in try-catch with SLF4J logger; never crash the game
- Use `@Unique` for all private fields/methods
- Forge client-only mixins: `@OnlyIn(Dist.CLIENT)`
- External MOD targets: `@Mixin(targets = "com.robertx22...", remap = false)`
- Common mixin config: `exile_overlay.mixins.json` (`defaultRequire: 1`)
- Forge mixin config: `exile_overlay-forge.mixins.json` (`defaultRequire: 0`, graceful degradation)

### Pattern
```java
@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.external.ModClass", remap = false)
public class ExternalModMixin {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExternalModMixin");

    @Inject(method = "targetMethod", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onMethod(CallbackInfo ci) {
        try {
            // Logic here
            ci.cancel();
        } catch (Exception e) {
            LOGGER.error("Failed: {}", e.getMessage(), e);
        }
    }
}
```

## Rendering Guidelines

- Use `GuiGraphics` for 2D HUD rendering
- Enable blend: `RenderSystem.enableBlend()` before transparency
- Push/pop pose stack: `graphics.pose().pushPose()` / `popPose()`
- Skip when GUI open: `if (mc.screen != null) return;` (exception: `DraggableHudConfigScreen` for preview)
- Render thread only: Never render from non-client threads
- Layer order: BACKGROUND -> FILL -> FRAME -> OVERLAY -> TEXT -> DEBUG
- Object pooling: `RenderContextPool.acquire()` / `release()` for zero-allocation hot paths

## Error Handling

### Result Pattern (Preferred)
```java
Result<Float, HudError> result = fetchData(player);
result.match(
    success -> render(success),
    error -> LOGGER.warn("Failed: {}", error)
);
float value = result.getOrDefault(0f);
```

### Try-Catch Pattern
- Wrap all mixin `@Inject` bodies in try-catch
- Wrap reflection calls in try-catch with specific exception types
- Return early for null: `if (player == null) return;`
- Side checks: `if (!entity.level().isClientSide()) return;`
- Use `ErrorLogCache` for deduplicated error logging
- Use `CircuitBreaker` for cascade failure prevention

### Reflection Safety
- Use `MethodHandlesUtil` for cached MethodHandle lookup
- Validate signature with `javap` before using `invokeExact`; prefer `unreflect`
- Per-method try-catch for `getMethod` / `getDeclaredMethod`
- Log error and return default on failure, never crash game

## Memory & Performance

- Reuse `StringBuilder` via `setLength(0)`
- Avoid allocation in render/tick methods
- Use primitive keys (`long` from UUID) vs `UUID.toString()`
- `UnifiedCache` with per-DataType TTL for per-frame data
- Zero allocations in hot paths: pre-allocate collections, reuse objects
- `UpdateFrequency` enum controls cache TTL: CRITICAL=0, NORMAL=15, SLOW=60, STATIC=300 ticks

## Thread Safety

- Rendering: Render thread only
- Shared data: `ConcurrentHashMap` with `volatile` flags
- Provider list: `CopyOnWriteArrayList`
- GUI updates: `Minecraft.getInstance().execute()` for thread-safe scheduling
- Singleton initialization: idempotency guard (`if (initialized) return`) or double-check locking
- `CircuitBreaker`: `AtomicReference` with CAS for state management

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Forge: 1.20.1-47.4.10
- Architectury: 9.2.14
- LWJGL: 3.3.2 (pinned via resolutionStrategy)
- Mine and Slash (optional): 6.3.14 (compileOnly, local JAR)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Mixin not applying | Check mixin config JSON, verify target class names, check `defaultRequire` |
| Reflection fails | Verify signature with `javap`, use `unreflect` not `invokeExact` |
| Texture not loading | Path: `assets/exile_overlay/textures/`, check `ResourceLocation` |
| Build failures | `./gradlew clean`, verify Java 17 |
| NPE in render | Add `mc.screen != null` / `mc.player == null` checks |
| FPS drop | Profile allocations, check cache TTL, verify no per-frame object creation |
| M&S integration fails | Check JAR path in `forge/build.gradle`, mixin `defaultRequire: 0` allows graceful degradation |

## References

- `MOD_COMPATIBILITY_API.md` - Data provider API documentation
- `api/AGENTS.md` - API module specifics (provider priorities, thread safety)
- `context.md` - Current project state and recent changes
