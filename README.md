# Bean Blaster Calculator 1.0.11

Client-side Fabric mod for Minecraft 1.21.11.

## Build

Use the normal Gradle wrapper from the project root:

```powershell
.\gradlew.bat remapJar
```

The finished mod jar will be in:

```text
build\libs\bean-blaster-calculator-1.0.11.jar
```

## Commands

```text
/blaster setup
/blaster <x> <z>
/blaster help
```

`/blaster setup` saves the magma block you are looking at. `/blaster <x> <z>` calculates the Wind Charges for the target X/Z.

## Notes

This is a client-side mod. It uses Fabric client commands, so it can be used on multiplayer servers without server-side installation.

The setup config is saved in:

```text
.minecraft/config/bean-blaster-calculator.json
```

The build uses Minecraft 1.21.11, Java 21, Fabric Loader, Fabric API, Yarn mappings, and Fabric Loom.


## 1.0.11 fix

This version removes `dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) }` from `settings.gradle` and declares repositories normally in `build.gradle`.
Fabric Loom adds an internal project repository called `LoomLocalRemappedMods`; strict settings-level repository mode blocks that repository and causes the previous build failure.

Build with:

```powershell
.\gradlew.bat remapJar
```
