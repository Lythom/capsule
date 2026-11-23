# Capsule mod by Lythom #

[![CircleCI](https://circleci.com/gh/Lythom/capsule.svg?style=svg)](https://circleci.com/gh/Lythom/capsule)

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

## Loaders ##

Minecraft 26.2 builds exist for **NeoForge** and **Fabric** (with Fabric API and Forge Config API Port), from the same
code: game logic in `common`, loader glue in `neoforge` and `fabric`. Minecraft 1.21.1 (NeoForge and Fabric) is on
branch `1.21.1`. Older Minecraft versions use Forge, each on its own branch (`1.20` for 1.20.1, `1.18` for 1.18.2,
`1.16` for 1.16.5).

Capsule recipes and information pages show in **JEI** and **REI** on both loaders (EMI on 1.21.1 only: it has no 26.x
version yet). Captures and deploys respect claim mods (Open Parties and Claims, Flan, Get Off My Lawn and others, see
[docs/CLAIMS.md](docs/CLAIMS.md)).

## Building and testing ##

`./gradlew build` builds `neoforge/build/libs/Capsule-neoforge-*.jar` and `fabric/build/libs/Capsule-fabric-*.jar` and
runs the unit tests and GameTests on both loaders, on Java 25 (Gradle downloads it if missing). `scripts/validate-all.sh`
runs every automated test layer. See [docs/TESTING.md](docs/TESTING.md).

## Mod page and downloads ##
[https://www.curseforge.com/minecraft/mc-mods/capsule](https://www.curseforge.com/minecraft/mc-mods/capsule)

## Wiki ##
[https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)

## Changelog ##

See [CHANGELOG.md](CHANGELOG.md) for the full changelog.
