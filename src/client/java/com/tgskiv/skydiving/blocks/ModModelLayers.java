package com.tgskiv.skydiving.blocks;

import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {

    public static final EntityModelLayer WINDSOCK_LAYER = new EntityModelLayer(
            Identifier.of("paraglidingsimulator", "windsock"), "main");

    public static final EntityModelLayer PARAGLIDER_LAYER = new EntityModelLayer(
            Identifier.of("paraglidingsimulator", "paraglider"), "main");
}
