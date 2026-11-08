package me.cortex.voxy.client.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//High priority so this runs after any other mod shrinking the distance check
@Mixin(value = OcclusionCuller.class, remap = false, priority = 2000)
public class MixinOcclusionCuller {
    //Voxy cuts every built section out of its lods, so one dropped here by distance leaves a hole in the world.
    //The shadow pass is left alone: nothing is seen through it and drawing everything there costs a lot of frames
    @ModifyReturnValue(method = "isWithinRenderDistance", at = @At("RETURN"))
    private static boolean voxy$keepSectionsCutFromLods(boolean original) {
        return original || (IGetVoxyRenderSystem.getNullable() != null && !IrisUtil.irisShadowActive());
    }
}
