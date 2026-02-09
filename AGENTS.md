# AGENTS.md - Minecraft Mod Development Guide

## Project Overview

Minecraft 1.20.1 mod using **Architectury** framework for multi-platform support (Fabric + Forge).

- **Mod ID**: `exile_overlay`
- **Minecraft Version**: 1.20.1
- **Java Version**: 17
- **Platforms**: Fabric, Forge

## Build Commands

### General
```bash
./gradlew build                    # Build all platforms
./gradlew clean build              # Clean and rebuild
./gradlew jar                      # Build main JAR
./gradlew check                    # Run all checks
```

### Platform-Specific
```bash
./gradlew :fabric:build            # Build Fabric only
./gradlew :forge:build             # Build Forge only
./gradlew :common:build            # Build common module only
```

### Testing
```bash
./gradlew test                     # Run all tests
./gradlew :common:test             # Run common module tests only
./gradlew :fabric:test             # Run Fabric tests only
./gradlew :forge:test              # Run Forge tests only
./gradlew test --tests "ClassName" # Run single test class
./gradlew test --tests "com.example.ClassName.methodName"  # Run single test method
```

### Development
```bash
./gradlew :fabric:runClient        # Run Fabric client
./gradlew :forge:runClient         # Run Forge client
./gradlew :fabric:runServer        # Run Fabric server
./gradlew :forge:runServer         # Run Forge server
```

## Code Style Guidelines

### Java Conventions

**Formatting:**
- Indentation: 4 spaces (no tabs)
- Line endings: LF
- Max line length: 120 characters
- Braces: Same line (K&R style)
- Wildcard imports: Avoid, use explicit imports

**Naming:**
- Classes: `PascalCase` (e.g., `HotbarRenderer`)
- Methods/Fields: `camelCase` (e.g., `renderHotbar`, `bgWidth`)
- Constants: `SCREAMING_SNAKE_CASE` (e.g., `BG_WIDTH`)
- Packages: lowercase, reverse domain (e.g., `com.example.exile_overlay`)
- Resource locations: `snake_case` (e.g., `hotbar_background.png`)

**Types:**
- Use explicit types, avoid `var`
- Mark parameters as `final` where appropriate
- Use `@Nullable`/`@NotNull` annotations

### Minecraft Conventions

**Import Order:**
```java
import net.minecraft.*;            // Minecraft classes
import net.fabricmc.*;             // Fabric API
import net.minecraftforge.*;       // Forge API
import dev.architectury.*;         // Architectury API
import org.spongepowered.asm.*;    // Mixin
import com.example.exile_overlay.*; // Project classes
import java.util.*;                 // Java standard library
```

**Resource Locations:**
```java
private static final ResourceLocation TEXTURE = 
    new ResourceLocation("exile_overlay", "textures/gui/texture.png");
```

**Mixin Rules:**
- Place mixins in `com.example.mixin` package
- Use `@Inject` at `HEAD` or `TAIL` when possible
- Avoid `@Overwrite` unless absolutely necessary
- Keep mixins minimal - delegate to helper classes
- Use `CallbackInfo` for void methods, `CallbackInfoReturnable<T>` for return types

**Client-Side Code:**
```java
@Environment(EnvType.CLIENT)
import net.fabricmc.api.Environment;
```

## Project Structure

```
common/src/main/java/com/example/exile_overlay/    # Shared code
├── client/render/                                 # Rendering logic
├── api/                                           # API classes
├── util/                                          # Utility classes
└── mixin/                                         # Mixin classes

fabric/src/main/java/com/example/                  # Fabric-specific
forge/src/main/java/com/example/                   # Forge-specific

common/src/main/resources/
├── assets/exile_overlay/textures/                 # GUI textures
├── mixin.exile_overlay.json                       # Mixin config
└── exile_overlay.accesswidener                    # Access wideners
```

## Development Guidelines

### Adding Features
1. **Always implement in `common`** first if platform-agnostic
2. Use `@ExpectPlatform` for platform-specific implementations
3. Add mixins to `common` with proper injection points
4. Test on both Fabric and Forge

### Rendering
- Use `GuiGraphics` for 2D rendering (1.20.1+)
- Enable blend mode: `RenderSystem.enableBlend()`
- Push/pop pose stack properly

### Thread Safety
- Rendering: **Render Thread**
- Network packets: **Netty Thread**
- Use `Minecraft.getInstance().execute()` for thread-safe GUI updates

### Error Handling
- Validate null checks for `Minecraft.getInstance().player`
- Check game mode before rendering UI elements
- Return early for null states: `if (player == null) return;`

## Dependencies

- Minecraft: 1.20.1
- Java: 17
- Architectury API: 9.2.14
- Fabric Loader: 0.18.4
- Fabric API: 0.92.7+1.20.1
- Forge: 1.20.1-47.4.10

## Common Issues

**Mixin not applying:** Check `exile_overlay.mixins.json` configuration and verify target class names.

**Texture not loading:** Verify path `assets/exile_overlay/textures/...` and ResourceLocation mod ID.

**ClassNotFoundException:** Run `./gradlew clean` and rebuild. Check for missing @ExpectPlatform implementations.

**Build failures:** Run `./gradlew clean`, verify Java 17, check Minecraft version compatibility.
