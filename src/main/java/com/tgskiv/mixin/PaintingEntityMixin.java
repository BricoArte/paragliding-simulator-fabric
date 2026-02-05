package com.tgskiv.mixin;

import com.tgskiv.skydiving.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingEntity.class)
public class PaintingEntityMixin {
    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
    private void paraglidingsimulator$onBreak(net.minecraft.server.world.ServerWorld world, Entity entity, CallbackInfo ci) {
        PaintingEntity self = (PaintingEntity) (Object) this;
        if (self.getVariant().matchesKey(ModItems.POSTER_VARIANT_KEY)) {
            self.dropStack(world, new ItemStack(ModItems.POSTER_ITEM), 0.0f);
            self.discard();
            ci.cancel();
        }
    }
}
