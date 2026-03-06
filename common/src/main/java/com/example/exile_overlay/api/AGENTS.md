# AGENTS.md - API Module

Public API interfaces and MOD compatibility layer for `exile_overlay`.

## Overview

This module provides:
- **IModDataProvider**: Interface for external MOD data integration
- **ModDataProviderRegistry**: Central registry for all data providers
- **Data abstraction**: Unified access to RPG MOD data (Mine and Slash, etc.)
- **Fallback mechanism**: Vanilla data when no compatible MOD is present

## Structure

```
api/
├── IModDataProvider.java          # Core provider interface
├── AbstractModDataProvider.java   # Base class with caching
├── ModDataProviderRegistry.java   # Registry & entry point
├── ModDataProviderBuilder.java    # Builder pattern for providers
├── ModData.java                   # Data container (key-value)
├── DataType.java                  # Standard data types enum
├── DataTypeRegistry.java          # Custom type registration
├── MineAndSlashDataProvider.java  # Mine and Slash integration
├── VanillaDataProvider.java       # Vanilla fallback
└── [supporting classes]
```

## Where to Look

| Task | Class | Notes |
|------|-------|-------|
| Add new MOD support | `ModDataProviderBuilder` or extend `AbstractModDataProvider` | See `MOD_COMPATIBILITY_API.md` |
| Fetch player data | `ModDataProviderRegistry` | Static methods for common queries |
| Define new data type | `DataTypeRegistry` | Register custom `DataType` |
| Custom data container | `ModData` | Key-value with type safety |
| Provider priority | `IModDataProvider.getPriority()` | Higher = preferred |

## Key Interfaces

### IModDataProvider
```java
String getId();                          // Provider identifier
int getPriority();                       // 0-200+ (vanilla=0, RPG=100+)
boolean isAvailable();                   // MOD loaded check
float getCurrentHealth(Player p);        // Health query
float getCurrentMana(Player p);          // Mana query
// ... etc
```

### DataType
Standard types: `CURRENT_HEALTH`, `MAX_HEALTH`, `CURRENT_MANA`, `MAX_MANA`, `LEVEL`, `EXPERIENCE`

## Provider Priority Guidelines

| Range | Use Case |
|-------|----------|
| 200+ | Addons/Extension MODs |
| 100-199 | RPG MODs (Mine and Slash = 100) |
| 50-99 | Utility MODs |
| 0-49 | Vanilla/Fallback (Vanilla = 0) |

## Usage Pattern

```java
// Direct registry access
float health = ModDataProviderRegistry.getCurrentHealth(player);
ModData data = ModDataProviderRegistry.getModData(player);

// Custom provider
ModDataProviderBuilder.create("my_mod")
    .priority(150)
    .checkModLoaded("my_mod")
    .withMana(player -> MyModAPI.getMana(player))
    .register();
```

## Thread Safety

- `ModDataProviderRegistry`: Thread-safe (ConcurrentHashMap)
- `ModDataCache`: Thread-safe with TTL
- Provider implementations: Must be thread-safe

## Error Handling

- `Result<T>`: Wrapper for success/failure
- `CircuitBreaker`: Prevents cascade failures
- `ErrorLogCache`: Deduplicated error logging
- Failed fetches return default values (0, false, etc.)

## Related Documentation

- Root: `../AGENTS.md`
- Full API spec: `../../MOD_COMPATIBILITY_API.md`
