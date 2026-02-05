package com.tgskiv.skydiving.item;

import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PosterItem extends Item {
    private final RegistryKey<PaintingVariant> variantKey;

    public PosterItem(RegistryKey<PaintingVariant> variantKey, Settings settings) {
        super(settings);
        this.variantKey = variantKey;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        Direction side = context.getSide();
        if (side.getAxis() == Direction.Axis.Y) {
            return ActionResult.FAIL;
        }
        BlockPos pos = context.getBlockPos().offset(side);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.FAIL;
        }

        RegistryEntry<PaintingVariant> variant = serverWorld.getRegistryManager()
                .getOrThrow(RegistryKeys.PAINTING_VARIANT)
                .getEntry(variantKey.getValue())
                .orElse(null);
        if (variant == null) {
            return ActionResult.FAIL;
        }

        PaintingEntity painting = new PaintingEntity(serverWorld, pos, side, variant);
        if (!painting.canStayAttached()) {
            return ActionResult.FAIL;
        }
        painting.onPlace();
        serverWorld.spawnEntity(painting);
        world.playSound(null, pos, SoundEvents.ENTITY_PAINTING_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
