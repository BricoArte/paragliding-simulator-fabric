package com.tgskiv.skydiving.worldgen;

import com.tgskiv.ParaglidingSimulator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

public final class ModStructures {
    public static final StructureType<LaunchSiteStructure> LAUNCH_SITE =
            Registry.register(Registries.STRUCTURE_TYPE,
                    Identifier.of(ParaglidingSimulator.MOD_ID, "launch_site"),
                    () -> LaunchSiteStructure.CODEC);

    public static final StructurePieceType LAUNCH_SITE_PIECE =
            Registry.register(Registries.STRUCTURE_PIECE,
                    Identifier.of(ParaglidingSimulator.MOD_ID, "launch_site"),
                    LaunchSitePiece::new);

    public static final StructurePieceType LAUNCH_SITE_CORRIDOR_PIECE =
            Registry.register(Registries.STRUCTURE_PIECE,
                    Identifier.of(ParaglidingSimulator.MOD_ID, "launch_site_corridor"),
                    LaunchSiteCorridorPiece::new);

    private ModStructures() {}

    public static void register() {
        // Static init is enough; keeping explicit register for clarity.
    }
}
