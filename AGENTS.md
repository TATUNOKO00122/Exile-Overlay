# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 mod using **Architectury** framework (Fabric + Forge).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platforms**: Fabric, Forge

## Build Commands

```bash
./gradlew build                              # Build all platforms
./gradlew clean build                        # Clean and rebuild
./gradlew :fabric:build                      # Build Fabric only
./gradlew :forge:build                       # Build Forge only
./gradlew test                               # Run all tests
./gradlew :fabric:runClient                  # Run Fabric client
./gradlew :forge:runClient                   # Run Forge client
```

## Project Structure

```
common/src/main/java/com/example/exile_overlay/
├── api/                    # Public interfaces & data provider API (30 files)
├── client/                 # Client-side rendering & UI (47 files)
│   ├── config/            # Configuration & config screens
│   ├── damage/            # Damage popup system
│   └── render/            # HUD rendering pipeline
├── mixin/                 # Mixin implementations (1 file)
└── util/                  # Utility classes

fabric/src/main/java/       # Fabric platform (7 files)
forge/src/main/java/        # Forge platform (9 files)
```

## Where to Look

| Task | Location | Notes |
|------|----------|-------|
| Add new MOD data provider | `common/src/main/java/com/example/exile_overlay/api/` | See `AbstractModDataProvider`, `IModDataProvider` |
| Add HUD renderer | `common/src/main/java/com/example/exile_overlay/client/render/` | Register via `HudRenderManager` |
| Add damage popup style | `common/src/main/java/com/example/exile_overlay/client/damage/` | `DamageFontRenderer`, `FontPreset` |
| Add config screen | `common/src/main/java/com/example/exile_overlay/client/config/screen/` | Extend `DraggableHudConfigScreen` |
| Platform-specific hook | `fabric/.../client/` or `forge/.../client/` | Keep minimal, delegate to common |
| Mixin injection | `common/mixin/` or platform `mixin/` | Use `@Inject` at HEAD/TAIL |

## Code Map

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `ModDataProviderRegistry` | Class | `api/` | Entry point for MOD data access |
| `HudRenderManager` | Class | `client/render/` | Central HUD render coordinator |
| `DamagePopupManager` | Class | `client/damage/` | Damage number popup controller |
| `IHudRenderer` | Interface | `api/` | HUD renderer contract |
| `IModDataProvider` | Interface | `api/` | Data provider contract |
| `ExampleModFabric` | Class | `fabric/` | Fabric entry point |
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
- **Resource locations**: `snake_case`

### Import Order
```java
import net.minecraft.*;
import net.fabricmc.*;
import net.minecraftforge.*;
import dev.architectury.*;
import org.spongepowered.asm.*;
import com.example.exile_overlay.*;
import java.util.*;
```

## Architecture Rules

1. **Common-first**: Implement in `common/` if platform-agnostic
2. **@ExpectPlatform**: Use for platform-specific implementations
3. **IDataSource**: Use interface for data abstraction
4. **HudRenderManager**: Register renderers via `registerCommand()`
5. **rendererIndex**: Use map for `IHudRenderer` lookup by config key

## Mixin Rules

- Place in `com.example.exile_overlay.mixin` package
- Use `@Inject` at `HEAD` or `TAIL`
- Avoid `@Overwrite`
- Wrap in try-catch with SLF4J logger
- Use `@Unique` for private fields/methods
- Mark client-only: `@Environment(EnvType.CLIENT)`

### Pattern
```java
@Environment(EnvType.CLIENT)
@Mixin(TargetClass.class)
public abstract class MyMixin {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/MyMixin");

    @Inject(method = "targetMethod", at = @At("TAIL"))
    private void exileOverlay$onMethod(CallbackInfo ci) {
        try {
            // Logic here
        } catch (Exception e) {
            LOGGER.error("Failed", e);
        }
    }
}
```

## Rendering Guidelines

- Use `GuiGraphics` for 2D rendering
- Enable blend: `RenderSystem.enableBlend()`
- Push/pop pose stack
- Check `mc.screen != null` to skip when GUI open
- Render thread only for graphics

## Safety Rules

### Error Handling
- Wrap mixin code in try-catch
- Return early for null: `if (player == null) return;`
- Side checks: `if (!entity.level().isClientSide()) return;`

### Memory
- Reuse `StringBuilder` via `setLength(0)`
- Avoid allocation in render/tick methods
- Use primitive keys (long) vs `UUID.toString()`

### Thread Safety
- Rendering: Render Thread only
- Shared data: Use `ConcurrentHashMap`
- GUI updates: `Minecraft.getInstance().execute()`

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Architectury API: 9.2.14
- Fabric Loader: 0.18.4
- Fabric API: 0.92.7+1.20.1
- Forge: 1.20.1-47.4.10

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Mixin not applying | Check mixin config JSON and target class names |
| Texture not loading | Verify path `assets/exile_overlay/textures/`, check ResourceLocation mod ID |
| Build failures | Run `./gradlew clean`, verify Java 17 |
| NPE in render | Ensure `mc.screen != null` check before drawing |

## References

- See `MOD_COMPATIBILITY_API.md` for data provider API details
- See `api/AGENTS.md` for API module specifics
