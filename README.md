# Capsule mod by Lythom #

[![CircleCI](https://circleci.com/gh/Lythom/capsule.svg?style=svg)](https://circleci.com/gh/Lythom/capsule)

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

## Loaders ##

Minecraft 1.21.1 builds exist for **NeoForge** and **Fabric** (with Fabric API and Forge Config API Port), from the same
code: game logic in `common`, loader glue in `neoforge` and `fabric`. Older Minecraft versions use Forge.

## Building and testing ##

`./gradlew build` builds `neoforge/build/libs/Capsule-neoforge-*.jar` and `fabric/build/libs/Capsule-fabric-*.jar` and
runs the unit tests and GameTests on both loaders. See [docs/TESTING.md](docs/TESTING.md).

## Mod page and downloads ##
[https://www.curseforge.com/minecraft/mc-mods/capsule](https://www.curseforge.com/minecraft/mc-mods/capsule)

## Wiki ##
[https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)

## Changelog ##

See [CHANGELOG.md](CHANGELOG.md) for the full changelog.
