# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 Forge mod using **Architectury** framework (Forge-only).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platform**: Forge only (Fabric disabled in `gradle.properties`)
- **Mappings**: Mojang Official (`officialMojangMappings()`)

## Workflow Rules

- 3+ steps / architecture / unknown problem => Plan mode first. Stuck => Stop, replan.
- Complex investigation => Parallel subagents.
- User correction => Analyze cause, document rule to `lessons.md`.
- No proof of operation != Done. Ask: "Would a senior approve?"
- Never hack-fix. Elegant + simple solution only.
- Bug report provided => Self-diagnose, fix autonomously.
- 3+ failed fixes => Stop. Revisit root hypothesis.
- Language: Japanese.

## Build Commands

```bash
./gradlew build                              # Build mod JAR
./gradlew clean build                        # Clean and rebuild
./gradlew :forge:build                       # Build Forge module only
./gradlew :forge:runClient                   # Run Minecraft client
./gradlew :forge:runServer                   # Run server (if configured)
./gradlew :forge:test                        # Run all tests
./gradlew :forge:test --tests "com.example.exile_overlay.SomeTest"  # Single test class
./gradlew :forge:test --tests "com.example.exile_overlay.SomeTest.testMethod"  # Single method
```

**No lint/checkstyle/spotbugs/PMD is configured.**

## Project Structure

All source code lives in `forge/src/main/java/com/example/exile_overlay/` (the `common` module is empty).

```
forge/src/main/java/com/example/exile_overlay/
├── api/                    # Public interfaces & MOD compatibility layer
├── client/
│   ├── config/             # HUD position, anchor, config screens
│   ├── damage/             # Damage popup system (singleton: DamagePopupManager)
│   ├── favorite/           # Favorite item system
│   └── render/             # HUD rendering pipeline (HudRenderManager, orb, entity, vanilla, effect)
├── forge/
│   ├── ExampleModForge.java       # Forge @Mod entry point
│   ├── client/             # Forge client handlers (FMLClientSetupEvent, keybinds)
│   ├── mixin/              # Forge-specific mixins (M&S integration, remap=false)
│   └── event/              # Server tick event handlers
├── mixin/                  # Common mixins (vanilla targets, remap=true)
└── util/                   # Inventory sorting, Lootr/M&S helpers
```

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
- **Mixin unique fields/methods**: `exileOverlay$fieldName` / `exileOverlay$methodName` prefix
- **Resource locations**: `snake_case` (e.g., `textures/gui/orb.png`)
- **Logger names**: Mixins use `"exile_overlay/ClassName"`, others use `ClassName.class`

### Import Order
```java
// 1. Project imports: com.example.exile_overlay.*
// 2. Minecraft: net.minecraft.* + com.mojang.blaze3d.*
// 3. Forge: net.minecraftforge.*
// 4. Logging: org.slf4j.*
// 5. Third-party: dev.architectury.*, org.spongepowered.asm.*
// 6. Java standard library (separated by blank line)
```

### Java 17+ Features
- Switch expressions, pattern matching (`instanceof LivingEntity living`)
- Records: `HudError`, `CacheStats`, `StateSnapshot`
- No raw types; use generic type parameters
- `var` used sparingly

### Comments
- Javadoc on public interfaces, written in **Japanese**
- Design rationale tags: `【設計思想】`, `【責任】`, `【パフォーマンス最適化】`
- Inline comments for non-obvious logic only

## Architecture Rules

- **Singleton**: `HudRenderManager`, `DamagePopupManager`, `UnifiedCache` use `getInstance()`
- **Registry**: Register via `HudRenderManager.registerCommand()`, `ModDataProviderRegistry.register()`
- **Result<T,E>**: Functional error handling instead of throwing exceptions
- **Command pattern**: `IRenderCommand.execute()` with layer/priority sorting
- **Object pooling**: `RenderContextPool.acquire()`/`release()` for zero-allocation render paths
- **Dependency inversion**: Renderables implement `IHudRenderer`/`IRenderCommand`

## Mixin Rules

- Common mixins: `com.example.exile_overlay.mixin` (vanilla targets, `remap=true`)
- Forge mixins: `com.example.exile_overlay.forge.mixin` (external MOD targets, `remap=false`)
- Use `@Inject` at `HEAD` or `TAIL`; **never `@Overwrite`**
- Wrap in try-catch with SLF4J logger; never crash the game
- Use `@Unique` for all private fields/methods
- Forge client-only mixins: `@OnlyIn(Dist.CLIENT)`
- Never guess mixin target. Verify with source or existing impl.

## Rendering Guidelines

- Use `GuiGraphics` for 2D HUD rendering
- Enable blend: `RenderSystem.enableBlend()` before transparency
- Skip when GUI open: `if (mc.screen != null) return;`
- Render thread only: Never render from non-client threads
- Layer order: BACKGROUND -> FILL -> FRAME -> OVERLAY -> TEXT -> DEBUG
- Zero allocation: `RenderContextPool.acquire()`/`release()` for hot paths
- **OpenGL textures**: `ByteBuffer.allocateDirect()` (never `wrap()`), cache textures, unbind after use

## Error Handling

- **Result Pattern**: `result.match(success -> ..., error -> ...)`
- Wrap mixin `@Inject` bodies in try-catch
- Wrap reflection calls with specific exception types
- Return early for null: `if (player == null) return;`
- Side checks: `if (!entity.level().isClientSide()) return;`
- Use `ErrorLogCache` for deduplicated logging
- Use `CircuitBreaker` for cascade failure prevention

## Performance & Thread Safety

- Zero allocation in render/tick methods
- Reuse `StringBuilder` via `setLength(0)`
- `ConcurrentHashMap` with `volatile` flags for shared data
- GUI updates: `Minecraft.getInstance().execute()` for thread-safe scheduling
- Singleton init: idempotency guard (`if (initialized) return`)
- Per-tick: O(N) max. O(N²)+ => Spatial partition / throttle

## Verification Checklist

- [ ] Edge cases (0, null, empty, MAX), long-run (accumulated error)
- [ ] Regression: Verify build passes
- [ ] Performance: FPS, CPU, memory, per-frame allocations
- [ ] Visual: Actual appearance matches spec
- [ ] Functional: Feature works (not just code runs)

## Dependencies

- Minecraft: 1.20.1 | Java: 17 | Forge: 1.20.1-47.4.10 | Architectury: 9.2.14
- LWJGL: 3.3.2 (pinned via resolutionStrategy)
- Mine and Slash (optional): 6.3.14 (compileOnly, local JAR)

## References

- `MOD_COMPATIBILITY_API.md` - Data provider API documentation
- `api/AGENTS.md` - API module specifics
- `context.md` - Current project state