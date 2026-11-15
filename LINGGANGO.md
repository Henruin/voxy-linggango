# Voxy for Linggango (Forge 1.20.1)

Fixes on top of the [m3t4f1v3/voxy](https://github.com/m3t4f1v3/voxy) backport (`multiversion` branch, commit `42d5037c`) so that Voxy works properly in the Linggango modpack (tested on Linggango 6.6.6hf, Forge 47.4.20, Java 25), with or without shaders.

No jars are published here: Voxy is "All rights reserved, do not redistribute". Build it yourself (see below).

## What this branch fixes

| Problem in game | Cause | Fix |
|---|---|---|
| Crash on the first frame with any shaderpack (`Found @Inject targetting a constructor`) | Forge's Mixin 0.8.5 rejects `@Inject` at INVOKE inside constructors | The three Iris constructor injections are `@Redirect`s now (`MixinIrisRenderingPipeline`, `MixinProgramSet`) |
| `/voxy import world` stops at 30-50% and never finishes, real chunks become invisible (you fall through the ground), `/voxy reload` and quitting the game freeze | A modded fluid block whose fluid's legacy block is itself made `ModelFactory.addEntry` recurse until `StackOverflowError`, which killed a worker while it held `enqueueLock`. Every Voxy worker, and the Sodium chunk builders that share them, then waited on that lock forever | Fluids are added one level deep only, a block that is its own fluid has no separate fluid, a missing fluid bake is logged instead of crashing, and the lock is released in `finally` |
| Ground missing when flying high, more of it the higher you go | Linggango Tweaks shortens Sodium's distance check (vertical and horizontal stretch). Voxy cuts every built section out of its LODs, so a section Sodium skips leaves a hole | New `MixinOcclusionCuller`: while Voxy is active, a loaded section is never dropped by distance (frustum and occlusion culling still apply; the shader shadow pass is left alone) |
| Crash `Tried to use destroyed RenderTargets` after resetting shaderpack options | Iris rebuilt its pipeline twice without notifying the level renderer, Voxy kept drawing into the destroyed one | Voxy marks its pipeline stale when Iris destroys it and rebuilds on the next tick |
| Flicker at the edge of the real render distance | Commit `9a2110ca` re-ingested every section Sodium built, commit `27185066` shrank the chunk bound box | Both reverted to the behaviour of the older build that worked |

Also added:

- `use_shader_pipeline` in `config/voxy-config.json` (default `true`). `false` draws LODs with Voxy's own pipeline even with shaders on. Most shaderpacks then hide the LODs, so keep it `true`; it is there for testing.
- With F3 open, a `GpuTime: [...]` line shows the GPU milliseconds of each Voxy pass.

## Setup in Linggango

1. Java 21 or newer (this jar targets Java 21).
2. Replace Linggango's render forks with the originals, **both together**:
   - remove `emberbium-*.jar` and `emberlens-*.jar`
   - add [Embeddium 0.3.31](https://www.curseforge.com/minecraft/mc-mods/embeddium) and Mekalus 1.8.0.1 (CurseForge project 1130957, the Oculus fork Linggango used before)
3. Add the Voxy jar built from this branch.
4. In `config/brute_force_culling_revived-client.toml` set `["Async chunk rebuild"] enabled = false`, or the ground flickers away for a few frames when you turn the camera fast.

Why swap the forks: in the same spot, with the same shader (Solas) and the same Voxy at 3440x1440 on an RTX 5080, Emberbium + Emberlens gave ~31 ms per frame (~32 FPS), Embeddium 0.3.31 + Mekalus gave ~9.6 ms (~104 FPS). Voxy's translucent pass alone went from 3.3 ms to 0.39 ms.

## Building

```
git clone -b linggango https://github.com/Henruin/voxy-linggango.git
cd voxy-linggango
.\gradlew :1.20.1-forge:jar
```

Needs a JDK 17 or newer (I used JDK 25). The jar ends up in `versions/1.20.1-forge/build/libs/`.

## Voxy SeedGen

SeedGen's Forge jar declares its mixins only in `mods.toml`, which Forge 47 ignores, so none of its 21 Voxy patches load. To make them load, add `MixinConfigs: voxyseedgen.mixins.json` to its `META-INF/MANIFEST.MF` and `"refmap": "voxyseedgen.refmap.json"` to `voxyseedgen.mixins.json`. On a server it only works when client and server have identical worldgen data.

## Credits

MCRcortex for Voxy, m3t4f1v3 for the 1.20.1 backport, Sash for the culling stretch hint. Fixes by Greed ([Henruin](https://github.com/Henruin)).
