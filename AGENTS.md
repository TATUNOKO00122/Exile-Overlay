# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 mod using **Architectury** framework (Fabric + Forge).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platforms**: Fabric, Forge

## Build Commands

```bash
./gradlew build                    # Build all platforms
./gradlew clean build              # Clean and rebuild
./gradlew :fabric:build            # Build Fabric only
./gradlew :forge:build             # Build Forge only
./gradlew test                     # Run all tests
./gradlew test --tests "ClassName" # Run single test class
./gradlew :fabric:runClient        # Run Fabric client
./gradlew :forge:runClient         # Run Forge client
```

## Code Style

### Formatting
- Indentation: 4 spaces (no tabs)
- Line endings: LF
- Max line length: 120 characters
- Braces: Same line (K&R style)
- No wildcard imports
- **No automatic formatter** (spotless) configured

### Naming
- Classes: `PascalCase` (e.g., `HotbarRenderer`)
- Methods/Fields: `camelCase` (e.g., `renderHotbar`)
- Constants: `SCREAMING_SNAKE_CASE` (e.g., `BG_WIDTH`)
- Packages: lowercase reverse domain
- Mixin unique fields: `exileOverlay$fieldName` prefix
- Resource locations: `snake_case`

### Import Order
```java
import net.minecraft.*;            // Minecraft classes
import net.fabricmc.*;             // Fabric API
import net.minecraftforge.*;       // Forge API
import dev.architectury.*;         // Architectury API
import org.spongepowered.asm.*;    // Mixin
import com.example.exile_overlay.*; // Project classes
import java.util.*;                 // Java standard library
```

### Types & Safety
- Use explicit types, avoid `var`
- Mark parameters as `final` where appropriate
- Always null-check `Minecraft.getInstance().player` before use
- Use @Unique for private mixin fields/methods
- Use @Environment for client-only mixins

### Error Handling
- Wrap all mixin code in try-catch blocks
- Use SLF4J logger: `LoggerFactory.getLogger("exile_overlay/ClassName")`
- Return early for null states: `if (player == null) return;`
- Handle side checks at method start: `if (!entity.level().isClientSide()) return;`

## Mixin Rules

- Place mixins in `com.example.exile_overlay.mixin` package
- Use `@Inject` at `HEAD` or `TAIL` when possible
- Avoid `@Overwrite` unless absolutely necessary
- Use `CallbackInfo` for void methods, `CallbackInfoReturnable<T>` for return types
- Add `@Unique` annotation to private mixin fields/methods
- Prefix unique fields with `exileOverlay$`

### Mixin Pattern
```java
@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class DamageMixin {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageMixin");
    @Unique private float exileOverlay$lastHealth = -1;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void exileOverlay$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!((LivingEntity)(Object)this).level().isClientSide()) return;
            if (source == null) { LOGGER.debug("Null damage source received"); return; }
        } catch (Exception e) { LOGGER.error("Failed to track damage", e); }
    }
}
```

## Architecture

1. **Always implement in `common`** first if platform-agnostic
2. Use `@ExpectPlatform` for platform-specific implementations
3. Test on both Fabric and Forge
4. Use `IDataSource` interface for data abstraction
5. Register renderers in `HudRenderManager`
6. Register providers in `ModDataProviderRegistry`

## Development Guidelines

### Memory Management
- Reuse StringBuilder: `sb.setLength(0)` instead of creating new instances
- Avoid object allocation in render/tick methods
- Use primitive types (long) as map keys instead of UUID.toString()

### Rendering
- Use `GuiGraphics` for 2D rendering (1.20.1+)
- Enable blend mode: `RenderSystem.enableBlend()`
- Push/pop pose stack: `graphics.pose().pushPose()` / `popPose()`
- Check `mc.screen != null` before drawing to skip when GUI open

### Thread Safety
- Rendering: **Render Thread** only
- Data access: Use `ConcurrentHashMap` for cross-thread sharing
- GUI updates: Use `Minecraft.getInstance().execute()`
- Mixin hooks: Check side (`isClientSide`) before client logic

### API Design
- Prefer composition over inheritance
- Use interfaces for contracts (`IDataSource`, `IRenderCommand`)
- Provide default implementations in interfaces
- Document usage with JavaDoc (Japanese allowed)

## Project Structure

```
common/src/main/java/com/example/exile_overlay/
├── api/                                   # Public interfaces & abstractions
├── client/damage/                         # Damage popup system
├── client/config/                         # Configuration & UI
├── client/render/                         # Rendering pipeline
├── mixin/                                 # Mixin implementations
└── util/                                  # Utility classes

fabric/src/main/java/com/example/          # Fabric-specific
forge/src/main/java/com/example/           # Forge-specific
```

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Architectury API: 9.2.14
- Fabric Loader: 0.18.4
- Fabric API: 0.92.7+1.20.1
- Forge: 1.20.1-47.4.10

## Troubleshooting

**Mixin not applying:** Check mixin config JSON and target class names
**Texture not loading:** Verify path `assets/exile_overlay/textures/` and ResourceLocation mod ID
**Build failures:** Run `./gradlew clean`, verify Java 17 installed
**NullPointerException in render:** Ensure `mc.screen != null` check before drawing
