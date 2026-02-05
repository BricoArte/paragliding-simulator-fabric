package com.tgskiv.skydiving.item;

import com.tgskiv.skydiving.menu.VarioScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class VarioHelmetItem extends ArmorItem {

    public VarioHelmetItem(ArmorMaterial material, EquipmentType slot, Settings settings) {
        super(material, slot, settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, player) -> new VarioScreenHandler(syncId, inventory),
                    Text.translatable("screen.paraglidingsimulator.vario_helmet")));
        }
        return ActionResult.SUCCESS;
    }
}
