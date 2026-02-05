package com.tgskiv.skydiving.item;

import com.tgskiv.skydiving.entity.ParagliderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParagliderItem extends Item {
    private static final int PHASE1_TICKS = 60; // 3s
    private static final int PHASE2_TICKS = 30; // 1.5s
    private static final int TOTAL_TICKS = PHASE1_TICKS + PHASE2_TICKS;
    private static final double FOLD_REACH = 5.0;

    private static final Map<UUID, UseState> ACTIVE_USES = new HashMap<>();

    private static class UseState {
        final String mode; // "deploy" or "fold"
        final BlockPos deployPos;
        final UUID targetUuid;
        int lastPhase;
        long lastSoundTick;

        UseState(String mode, BlockPos deployPos, UUID targetUuid) {
            this.mode = mode;
            this.deployPos = deployPos;
            this.targetUuid = targetUuid;
        }
    }

    public static void startFold(PlayerEntity player, ParagliderEntity target, Hand hand) {
        if (target == null) {
            return;
        }
        player.setCurrentHand(hand);
        if (!player.getWorld().isClient()) {
            ACTIVE_USES.put(player.getUuid(), new UseState("fold", null, target.getUuid()));
        }
    }

    public ParagliderItem(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return TOTAL_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getBlockPos().up();

        if (player == null) return ActionResult.PASS;
        if (hasParagliderInSight(player, world)) {
            return ActionResult.PASS;
        }
        if (hasParagliderAt(world, pos)) {
            if (!world.isClient()) {
                player.sendMessage(Text.translatable("message.paraglidingsimulator.no_deploy_on_paraglider"), true);
            }
            return ActionResult.PASS;
        }
        player.setCurrentHand(context.getHand());
        if (!world.isClient()) {
            ACTIVE_USES.put(player.getUuid(), new UseState("deploy", pos, null));
        }
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        Vec3d start = user.getCameraPosVec(1.0f);
        Vec3d look = user.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(FOLD_REACH));
        Box box = user.getBoundingBox().stretch(look.multiply(FOLD_REACH)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(user, start, end, box,
                entity -> entity instanceof ParagliderEntity, FOLD_REACH * FOLD_REACH);
        if (hit != null && hit.getEntity() instanceof ParagliderEntity target) {
            user.setCurrentHand(hand);
            if (!world.isClient()) {
                ACTIVE_USES.put(user.getUuid(), new UseState("fold", null, target.getUuid()));
            }
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }


    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient() || !(user instanceof PlayerEntity player)) {
            return;
        }
        UseState state = ACTIVE_USES.get(player.getUuid());
        if (state == null) return;
        int elapsed = TOTAL_TICKS - remainingUseTicks;
        int phase = (elapsed < PHASE1_TICKS) ? 1 : 2;
        if (phase != state.lastPhase) {
            state.lastPhase = phase;
        }
        if ("deploy".equals(state.mode)) {
            long now = world.getTime();
            if (now - state.lastSoundTick >= 10) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.5f, 1.2f);
                state.lastSoundTick = now;
            }
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient() || !(user instanceof PlayerEntity player)) {
            return stack;
        }
        UseState state = ACTIVE_USES.remove(player.getUuid());
        if (state == null) {
            return stack;
        }
        if ("deploy".equals(state.mode)) {
            BlockPos pos = state.deployPos;
            if (pos == null) return stack;
            if (hasParagliderAt(world, pos)) {
                player.sendMessage(Text.translatable("message.paraglidingsimulator.no_deploy_on_paraglider"), true);
                return stack;
            }
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            ParagliderEntity e = new ParagliderEntity(world, x + 0.5, y + 0.2, z + 0.5);
            e.setYaw(player.getYaw());
            e.setMountSuppressTicks(10);
            world.spawnEntity(e);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        } else if ("fold".equals(state.mode)) {
            UUID target = state.targetUuid;
            if (target != null) {
                for (Entity entity : world.getEntitiesByClass(ParagliderEntity.class, player.getBoundingBox().expand(6.0), e -> true)) {
                    if (entity.getUuid().equals(target) && entity.getPassengerList().isEmpty()) {
                        entity.remove(Entity.RemovalReason.KILLED);
                        break;
                    }
                }
            }
        }
        return stack;
    }

    private static boolean hasParagliderAt(World world, BlockPos pos) {
        Box box = new Box(pos).expand(1.0);
        return !world.getEntitiesByClass(ParagliderEntity.class, box, e -> true).isEmpty();
    }

    private static boolean hasParagliderInSight(PlayerEntity player, World world) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(FOLD_REACH));
        Box box = player.getBoundingBox().stretch(look.multiply(FOLD_REACH)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end, box,
                entity -> entity instanceof ParagliderEntity, FOLD_REACH * FOLD_REACH);
        return hit != null && hit.getEntity() instanceof ParagliderEntity;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && user instanceof PlayerEntity player) {
            ACTIVE_USES.remove(player.getUuid());
        }
        return true;
    }
}
