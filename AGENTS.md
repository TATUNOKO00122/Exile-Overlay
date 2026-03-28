# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 Forge mod using **Architectury** framework (Forge-only).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platform**: Forge only (Fabric disabled in `gradle.properties`)

## Build Commands

```bash
./gradlew build                              # Build mod
./gradlew clean build                        # Clean and rebuild
./gradlew :forge:build                       # Build Forge module
./gradlew :forge:runClient                   # Run Minecraft client
./gradlew :forge:runServer                   # Run server (if configured)
```

**Note**: No test suite exists. Add tests to `forge/src/test/java/` and run via `./gradlew :forge:test`.
For single test: `./gradlew :forge:test --tests "com.example.exile_overlay.SomeTest"`

## Project Structure

```
forge/src/main/java/com/example/exile_overlay/
├── api/                    # Public interfaces & data provider API
│   ├── IModDataProvider.java    # Core provider interface
│   ├── ModDataProviderRegistry.java  # Registry entry point
│   ├── Result.java              # Functional error handling
│   ├── MethodHandlesUtil.java   # Reflection utilities
│   └── [data/cache/validation classes]
├── client/
│   ├── damage/            # Damage popup system
│   ├── favorite/          # Favorite item system
│   └── render/            # HUD rendering pipeline
│       ├── HudRenderManager.java  # Central coordinator
│       ├── orb/           # Energy orb rendering
│       ├── entity/        # Entity health bars
│       ├── vanilla/       # Vanilla HUD overlays
│       └── effect/        # Buff overlays
├── forge/
│   ├── ExampleModForge.java     # Forge entry point
│   ├── client/            # Forge client handlers
│   ├── mixin/             # Forge-specific mixins
│   └── event/             # Event handlers
├── mixin/                 # Common mixins
└── util/                  # Utility classes
```

## Code Map

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `HudRenderManager` | Class | `client/render/` | Central HUD render coordinator (singleton) |
| `ModDataProviderRegistry` | Class | `api/` | Entry point for MOD data access |
| `DamagePopupManager` | Class | `client/damage/` | Damage number popup controller |
| `IHudRenderer` | Interface | `api/` | HUD renderer contract |
| `IModDataProvider` | Interface | `api/` | Data provider contract |
| `IRenderCommand` | Interface | `api/` | Render command interface |
| `Result<T,E>` | Class | `api/` | Functional error handling (Success/Failure) |
| `ExampleModForge` | Class | `forge/` | Forge entry point |

## Code Style

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line endings**: LF
- **Max line length**: 120 characters
- **Braces**: Same line (K&R style)
- **No wildcard imports**

### Naming Conventions
- **Classes**: `PascalCase`
- **Methods/Fields**: `camelCase`
- **Constants**: `SCREAMING_SNAKE_CASE`
- **Mixin unique fields**: `exileOverlay$fieldName` prefix
- **Mixin methods**: `exileOverlay$methodName` prefix
- **Resource locations**: `snake_case` (e.g., `textures/gui/orb.png`)
- **Logger names**: `"exile_overlay/ClassName"`

### Import Order
```java
import net.minecraft.*;
import net.minecraftforge.*;
import dev.architectury.*;
import org.spongepowered.asm.*;
import org.slf4j.*;
import com.example.exile_overlay.*;
import java.util.*;
```

### Types
- Use `switch` expressions (Java 17+): `return switch (x) { case "A" -> 1; default -> 0; };`
- Pattern matching in instanceof: `if (entity instanceof LivingEntity living) { ... }`
- Avoid raw types; use generic type parameters

## Architecture Rules

1. **Common-first**: Implement platform-agnostic code in reusable classes
2. **Singleton pattern**: `HudRenderManager`, `DamagePopupManager`, caches use getInstance()
3. **Registry pattern**: Register via `HudRenderManager.registerCommand()`, `ModDataProviderRegistry.register()`
4. **Result<T,E>**: Use functional error handling instead of throwing exceptions
5. **ConcurrentHashMap**: Thread-safe storage for shared data

## Mixin Rules

- Place in `com.example.exile_overlay.mixin` or `forge.mixin` package
- Use `@Inject` at `HEAD` or `TAIL`; avoid `@Overwrite`
- Wrap in try-catch with SLF4J logger
- Use `@Unique` for private fields/methods
- Forge client-only: `@OnlyIn(Dist.CLIENT)`
- Remap false for external MOD targets: `@Mixin(targets = "com.robertx22...", remap = false)`

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
            ci.cancel(); // If replacing behavior
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
- Skip when GUI open: `if (mc.screen != null) return;`
- Render thread only: Never render from non-client threads
- Object pooling: Use `PooledRenderContext` via `RenderContextPool.acquire()` / `release()`

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
- Wrap mixin code in try-catch
- Wrap reflection calls in try-catch with specific exception types
- Return early for null: `if (player == null) return;`
- Side checks: `if (!entity.level().isClientSide()) return;`
- Use `ErrorLogCache` for deduplicated error logging

### Reflection Safety
- Use `MethodHandlesUtil` for cached MethodHandle lookup
- Validate signature with javap before using `invokeExact`
- Per-method try-catch for `getMethod` / `getDeclaredMethod`
- Log error and return default on failure, never crash game

## Memory & Performance

- Reuse `StringBuilder` via `setLength(0)`
- Avoid allocation in render/tick methods
- Use primitive keys (long from UUID) vs `UUID.toString()`
- Use `UnifiedCache` with frame-based TTL for per-frame data
- Zero allocations in hot paths: Pre-allocate collections, reuse objects

## Thread Safety

- Rendering: Render Thread only
- Shared data: `ConcurrentHashMap` with volatile flags
- GUI updates: `Minecraft.getInstance().execute()` for thread-safe scheduling
- Singleton initialization: Double-check locking or static init

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Forge: 1.20.1-47.4.10
- Mine and Slash (optional): 6.3.14 (compileOnly)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Mixin not applying | Check mixin config JSON, verify target class names |
| Reflection fails | Verify signature with javap, use `unreflect` not `invokeExact` |
| Texture not loading | Path: `assets/exile_overlay/textures/`, check ResourceLocation |
| Build failures | Run `./gradlew clean`, verify Java 17 |
| NPE in render | Add `mc.screen != null` / `mc.player == null` checks |
| FPS drop | Profile allocations, check cache TTL, verify no per-frame object creation |

## References

- `MOD_COMPATIBILITY_API.md` - Data provider API documentation
- `api/AGENTS.md` - API module specifics
- `context.md` - Current project state and recent changes