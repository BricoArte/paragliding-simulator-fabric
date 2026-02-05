package com.tgskiv.skydiving.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class VarioScreenHandler extends ScreenHandler {

    public VarioScreenHandler(int syncId, PlayerInventory inventory) {
        super(ModScreenHandlers.VARIO, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
