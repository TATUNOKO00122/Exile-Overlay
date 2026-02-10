# AGENTS.md - Minecraft Mod Development Guide

Minecraft 1.20.1 mod using **Architectury** framework (Fabric + Forge).

- **Mod ID**: `exile_overlay`
- **Java**: 17
- **Platforms**: Fabric, Forge

## Build Commands

```bash
# Build all platforms
./gradlew build                    # Unix/Mac
gradlew.bat build                  # Windows

./gradlew clean build              # Clean and rebuild
./gradlew :fabric:build            # Build Fabric only
./gradlew :forge:build             # Build Forge only

# Run tests
./gradlew test                                    # Run all tests
./gradlew test --tests "ClassName"                # Run single test class
./gradlew test --tests "com.example.ClassName.methodName"  # Run single method
./gradlew test --tests "*Benchmark"               # Run benchmark tests

# Development
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

### Error Handling
- Wrap all mixin code in try-catch blocks
- Use SLF4J logger: `LoggerFactory.getLogger("exile_overlay/ClassName")`
- Return early for null states: `if (player == null) return;`

## Mixin Rules

- Place mixins in `com.example.exile_overlay.mixin` package
- Use `@Inject` at `HEAD` or `TAIL` when possible
- Avoid `@Overwrite` unless absolutely necessary
- Use `CallbackInfo` for void methods, `CallbackInfoReturnable<T>` for return types
- Add `@Unique` annotation to private mixin fields/methods
- Prefix unique fields with `exileOverlay$`

```java
@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class DamageMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageMixin");

    @Unique
    private float exileOverlay$lastHealth = -1;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void exileOverlay$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            // Mixin logic here
        } catch (Exception e) {
            LOGGER.error("Failed to track damage", e);
        }
    }
}
```

## Development Guidelines

### Architecture
1. **Always implement in `common`** first if platform-agnostic
2. Use `@ExpectPlatform` for platform-specific implementations
3. Test on both Fabric and Forge

### Memory Management
- Reuse StringBuilder instead of creating new instances
- Avoid object allocation in render/tick methods
- Use primitive types as map keys instead of UUID.toString()

### Rendering
- Use `GuiGraphics` for 2D rendering (1.20.1+)
- Enable blend mode: `RenderSystem.enableBlend()`
- Push/pop pose stack properly: `graphics.pose().pushPose()` / `popPose()`

### Resource Locations
```java
private static final ResourceLocation TEXTURE =
    new ResourceLocation("exile_overlay", "textures/gui/texture.png");
```

### Thread Safety
- Rendering: **Render Thread**
- Network packets: **Netty Thread**
- Use `Minecraft.getInstance().execute()` for thread-safe GUI updates
- Use `ConcurrentHashMap` for cross-thread data sharing

## Project Structure

```
common/src/main/java/com/example/exile_overlay/    # Shared code
├── client/render/                                 # Rendering logic
├── client/damage/                                 # Damage popup system
├── api/                                           # API classes
├── util/                                          # Utility classes
└── mixin/                                         # Mixin classes

fabric/src/main/java/com/example/                  # Fabric-specific
forge/src/main/java/com/example/                   # Forge-specific

common/src/main/resources/
├── assets/exile_overlay/textures/                 # GUI textures
└── exile_overlay.accesswidener                    # Access wideners
```

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Architectury API: 9.2.14
- Fabric Loader: 0.18.4
- Fabric API: 0.92.7+1.20.1
- Forge: 1.20.1-47.4.10

## Troubleshooting

**Mixin not applying:** Check mixin configuration and verify target class names.
**Texture not loading:** Verify path `assets/exile_overlay/textures/...` and ResourceLocation mod ID.
**Build failures:** Run `./gradlew clean`, verify Java 17.
