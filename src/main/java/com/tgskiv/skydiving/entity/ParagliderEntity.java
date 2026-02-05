package com.tgskiv.skydiving.entity;

import org.slf4j.LoggerFactory;

import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.flight.ParagliderForces;
import com.tgskiv.skydiving.registry.ModEntities;
import com.tgskiv.skydiving.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.tag.FluidTags;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParagliderEntity extends Entity implements FlyingItemEntity {

    public static final Logger LOGGER = LoggerFactory.getLogger("paraglidingsimulator");

    // Ajustes MVP
    // ~12 km/h fÃ­sicos (HUD mostrarÃ¡ inflado): 12 km/h -> 3.33 m/s -> ~0.167 bloques/tick
    public static double FORWARD_SPEED = 0.167;
    public static double SINK_RATE = 0.03;     // caida suave
    public static float TURN_RATE_DEG = 3.0f;  // giro por tick cuando pulsas A/D
    private static final double TURN_RAMP_STEP = 0.08; // aceleracion del giro por tick
    public static double TAXI_SPEED = 0.03;    // velocidad en suelo con WASD
    private static final double SPEED_RAMP_STEP = 0.025; // cambio mÃ¡ximo por tick del factor de velocidad/hundimiento
    public static final int SPIN_TRIGGER_TICKS = 40; // 2s a 20 tps
    public static final int SPIN_RAMP_TICKS = 60; // tiempo hasta maximo
    public static final float SPIN_MAX_TURN_MULT = 1.8f;
    public static final double SPIN_MAX_SINK_MULT = 6.0;

    private int turnInput = 0; // -1,0,+1
    private float groundForward = 0f;
    private float groundSideways = 0f;
    private boolean forceDismount = false;
    // Suavizado de acelerador/freno en vuelo
    private double targetSpeedFactor = 1.0;
    private double currentSpeedFactor = 1.0;
    private double targetSinkRate = SINK_RATE;
    private double currentSinkRate = SINK_RATE;
    private double targetTurnFactor = 0.0;
    private double currentTurnFactor = 0.0;
    private int spinTicks = 0;
    private static final int FOLD_HIT_REQUIRED = 8;
    private static final double FOLD_MAX_DIST_SQ = 16.0;
    private static final int FOLD_HIT_TIMEOUT_TICKS = 12;
    private static final TrackedData<Integer> FOLD_HIT_COUNT =
            DataTracker.registerData(ParagliderEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FOLD_HIT_AGE =
            DataTracker.registerData(ParagliderEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TURN_INPUT_TRACKED =
            DataTracker.registerData(ParagliderEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPIN_PROGRESS_TRACKED =
            DataTracker.registerData(ParagliderEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> FORWARD_INPUT_TRACKED =
            DataTracker.registerData(ParagliderEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private UUID foldingPlayerUuid = null;
    private int mountSuppressTicks = 0;
    private int clientNoMoveTicks = 0;
    private boolean clientWasMounted = false;
    private int lateralHitTicks = 0;
    private int lateralHitSoundCooldown = 0;
    private boolean lastGrounded = false;
    private boolean passengerYawInit = false;
    private float passengerYawLast = 0.0f;
    private boolean renderYawInit = false;
    private float renderYawLast = 0.0f;
    private float renderYawCurr = 0.0f;
    private static final float RENDER_YAW_LERP = 0.2f;
    private boolean renderResetRequested = false;
    private double trackedX = 0.0;
    private double trackedY = 0.0;
    private double trackedZ = 0.0;
    private float trackedYaw = 0.0f;
    private int lerpTicks = 0;
    private long lastTrackedUpdateMillis = 0L;
    private int lastInterpolationSteps = 0;

    public ParagliderEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setNoGravity(true); // controlamos la caida
    }

    public ParagliderEntity(World world, double x, double y, double z) {
        this(ModEntities.PARAGLIDER, world);
        this.setPosition(x, y, z);
    }

    protected boolean useAggressiveClientLerp() {
        return true;
    }

    public void setTurnInput(int turnInput) {
        this.turnInput = Integer.compare(turnInput, 0); // normaliza a -1/0/+1
        this.dataTracker.set(TURN_INPUT_TRACKED, this.turnInput);
    }

    public void setGroundInput(float forward, float sideways) {
        this.groundForward = MathHelper.clamp(forward, -1f, 1f);
        this.groundSideways = MathHelper.clamp(sideways, -1f, 1f);
        this.dataTracker.set(FORWARD_INPUT_TRACKED, this.groundForward);
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        this.lastTrackedUpdateMillis = Util.getMeasuringTimeMs();
        this.lastInterpolationSteps = Math.max(interpolationSteps, 1);
        if (!useAggressiveClientLerp()) {
            super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
            return;
        }
        this.trackedX = x;
        this.trackedY = y;
        this.trackedZ = z;
        this.trackedYaw = yaw;
        this.lerpTicks = Math.max(interpolationSteps, 1);
    }

    public long getMsSinceLastTrackedUpdate() {
        if (!this.getWorld().isClient() || this.lastTrackedUpdateMillis == 0L) {
            return -1L;
        }
        return Util.getMeasuringTimeMs() - this.lastTrackedUpdateMillis;
    }

    public int getLastInterpolationSteps() {
        return this.lastInterpolationSteps;
    }

    private void clientLerpTick() {
        this.setPosition(
                this.getX() + ((this.trackedX - this.getX()) / (double) this.lerpTicks),
                this.getY() + ((this.trackedY - this.getY()) / (double) this.lerpTicks),
                this.getZ() + ((this.trackedZ - this.getZ()) / (double) this.lerpTicks)
        );
        this.setYaw(this.getYaw() + (MathHelper.wrapDegrees(this.trackedYaw - this.getYaw()) / (float) this.lerpTicks));
        this.lerpTicks--;
    }

    private void updateTurnFactor(boolean turning) {
        targetTurnFactor = turning ? 1.0 : 0.0;
        double delta = MathHelper.clamp(targetTurnFactor - currentTurnFactor, -TURN_RAMP_STEP, TURN_RAMP_STEP);
        currentTurnFactor += delta;
    }


    @Override
    public void tick() {
        super.tick();

        

        if (this.getWorld().isClient()) {
            if (this.age == 1) {
                requestClientRenderReset();
            }
            updateRenderYaw();
            boolean mounted = !this.getPassengerList().isEmpty();
            if (!clientWasMounted && mounted) {
                requestClientRenderReset();
                clientNoMoveTicks = Math.max(clientNoMoveTicks, 8);
            } else if (clientWasMounted && !mounted) {
                requestClientRenderReset();
            }
            clientWasMounted = mounted;
            if (this.age < 5) {
                clientNoMoveTicks = Math.max(clientNoMoveTicks, 5);
            }
            if (useAggressiveClientLerp() && lerpTicks > 0) {
                clientLerpTick();
                return;
            }
            if (clientNoMoveTicks > 0) {
                clientNoMoveTicks--;
                return;
            }
            // En cliente, aplicar velocidad local para evitar saltos entre paquetes.
            this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
            return;
        }

        if (isInLavaSurface()) {
            burnInLava();
            return;
        }
        if (isInWaterSurface()) {
            sinkInWater();
            return;
        }

        if (mountSuppressTicks > 0) {
            mountSuppressTicks--;
        }
        if (lateralHitTicks > 0) {
            lateralHitTicks--;
        }
        if (lateralHitSoundCooldown > 0) {
            lateralHitSoundCooldown--;
        }

        if (foldingPlayerUuid != null) {
            PlayerEntity foldingPlayer = this.getWorld().getPlayerByUuid(foldingPlayerUuid);
            if (foldingPlayer == null || foldingPlayer.isRemoved()) {
                clearFolding();
            } else if (!this.getPassengerList().isEmpty()) {
                clearFolding();
            } else if (foldingPlayer.squaredDistanceTo(this) > FOLD_MAX_DIST_SQ) {
                clearFolding();
            } else {
                int age = this.dataTracker.get(FOLD_HIT_AGE) + 1;
                this.dataTracker.set(FOLD_HIT_AGE, age);
                if (age > FOLD_HIT_TIMEOUT_TICKS) {
                    clearFolding();
                }
            }
        }

        // Evitar dano de caida para entidad y pasajeros
        this.fallDistance = 0;
        for (Entity p : this.getPassengerList()) {
            if (p instanceof LivingEntity living) {
                living.fallDistance = 0;
            }
        }

        PlayerEntity driver = this.getControllingPlayer();

        // Si nadie lo monta, se queda quieto (como barca sin pasajero)
        if (driver == null) {
            this.setVelocity(this.getVelocity().multiply(0.8, 0.8, 0.8));
            spinTicks = 0;
            return;
        }

        boolean landed = this.isGrounded();
        if (landed) {
            if (!lastGrounded) {
                applyLandingImpact(driver);
                if (this.isRemoved()) {
                    lastGrounded = true;
                    return;
                }
            }
            spinTicks = 0;
            // Girar tipo barca en suelo tambien
            if (turnInput != 0) {
                updateTurnFactor(true);
                this.setYaw(this.getYaw() + (float)(turnInput * TURN_RATE_DEG * currentTurnFactor));
            } else {
                updateTurnFactor(false);
            }

            // Movimiento en suelo con WASD (datos desde payload), sin viento ni updraft
            float fwd = groundForward;
            float side = groundSideways;

            Vec3d forward = ParagliderForces.yawToHorizontalDir(this.getYaw());
            Vec3d left = new Vec3d(-forward.z, 0, forward.x);
            Vec3d vel = forward.multiply(fwd * TAXI_SPEED).add(left.multiply(side * TAXI_SPEED));

            // En suelo, sin componente vertical
            this.setVelocity(vel.x, 0, vel.z);

            // Mover en servidor (cliente interpola con la velocidad sincronizada)
            this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());


            lastGrounded = true;
            return;
        }
        lastGrounded = false;

        boolean spinInput = turnInput != 0 && groundForward > 0.01f;
        if (spinInput) {
            spinTicks = Math.min(spinTicks + 1, SPIN_TRIGGER_TICKS + SPIN_RAMP_TICKS);
        } else {
            spinTicks = 0;
        }
        double spinProgress = 0.0;
        if (spinTicks > SPIN_TRIGGER_TICKS) {
            double raw = (spinTicks - SPIN_TRIGGER_TICKS) / (double) SPIN_RAMP_TICKS;
            spinProgress = MathHelper.clamp(raw, 0.0, 1.0);
        }
        this.dataTracker.set(SPIN_PROGRESS_TRACKED, (float) spinProgress);

        // Girar tipo barca en vuelo
        if (turnInput != 0) {
            updateTurnFactor(true);
            float turnRate = (float)(TURN_RATE_DEG * currentTurnFactor);
            if (spinProgress > 0.0) {
                turnRate *= (1.0f + (float) spinProgress * (SPIN_MAX_TURN_MULT - 1.0f));
            }
            this.setYaw(this.getYaw() + (turnInput * turnRate));
        } else {
            updateTurnFactor(false);
        }

        // Acelerador/freno en vuelo: usa groundForward (W/S) enviado por el cliente
        if (groundForward > 0.01f) { // W
            targetSpeedFactor = 1.25;
            targetSinkRate = 0.05; // ~1 m/s
        } else if (groundForward < -0.01f) { // S
            targetSpeedFactor = 0.65;
            targetSinkRate = 0.0225; // ~0.45 m/s
        } else {
            targetSpeedFactor = 1.0;
            targetSinkRate = SINK_RATE; // 0.6 m/s
        }
        // Suavizar cambios
        double df = MathHelper.clamp(targetSpeedFactor - currentSpeedFactor, -SPEED_RAMP_STEP, SPEED_RAMP_STEP);
        currentSpeedFactor += df;
        double dsink = MathHelper.clamp(targetSinkRate - currentSinkRate, -SPEED_RAMP_STEP, SPEED_RAMP_STEP);
        currentSinkRate += dsink;

        Vec3d forward = ParagliderForces.yawToHorizontalDir(this.getYaw());

        // Base: avance fijo + caida suave
        double spinSinkMult = 1.0;
        if (spinProgress > 0.0) {
            spinSinkMult = 1.0 + spinProgress * (SPIN_MAX_SINK_MULT - 1.0);
        }
        double vy = -currentSinkRate * spinSinkMult;

        Vec3d vel = new Vec3d(
                forward.x * (FORWARD_SPEED * currentSpeedFactor),
                vy,
                forward.z * (FORWARD_SPEED * currentSpeedFactor)
        );

        // Viento actual del servidor
        Vec3d windDir = SkydivingHandler.getCurrentWindDirection();
        double windSpeed = SkydivingHandler.getCurrentWindSpeed();
        if (!this.getWorld().getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) {
            windDir = Vec3d.ZERO;
            windSpeed = 0.0;
        }

        // Aplicar viento con compensacion por altura al suelo
        vel = vel.add(ParagliderForces.windPush(this, windDir, windSpeed));

        // Updraft por relieve (server-side)
        double updraft = ParagliderForces.computeUpdraftStrength(this, windDir, windSpeed);
        vel = vel.add(0, updraft, 0);

        if (lateralHitTicks > 0) {
            vel = vel.add(0, -0.05, 0);
        }

        this.setVelocity(vel);

        // Mover
        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());

        if (this.horizontalCollision) {
            lateralHitTicks = Math.max(lateralHitTicks, 10);
            if (lateralHitSoundCooldown == 0) {
                this.getWorld().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_WOOL_HIT,
                        SoundCategory.PLAYERS,
                        0.7f,
                        0.9f + (this.getWorld().random.nextFloat() * 0.2f)
                );
                lateralHitSoundCooldown = 6;
            }
        }
    }

    private void updateRenderYaw() {
        float target = this.getYaw();
        if (!renderYawInit) {
            renderYawInit = true;
            renderYawLast = target;
            renderYawCurr = target;
            return;
        }
        renderYawLast = renderYawCurr;
        float delta = MathHelper.wrapDegrees(target - renderYawCurr);
        renderYawCurr = renderYawCurr + (delta * RENDER_YAW_LERP);
    }

    public float getRenderYaw(float tickDelta) {
        if (!renderYawInit) {
            return this.getYaw(tickDelta);
        }
        return MathHelper.lerp(tickDelta, renderYawLast, renderYawCurr);
    }

    private void requestClientRenderReset() {
        renderYawInit = false;
        passengerYawInit = false;
        renderResetRequested = true;
    }

    public void requestClientResync() {
        requestClientRenderReset();
        clientNoMoveTicks = Math.max(clientNoMoveTicks, 10);
        this.setVelocity(Vec3d.ZERO);
    }

    public boolean consumeRenderResetRequest() {
        if (!renderResetRequested) {
            return false;
        }
        renderResetRequested = false;
        return true;
    }


    @Override
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
        builder.add(FOLD_HIT_COUNT, 0);
        builder.add(FOLD_HIT_AGE, 0);
        builder.add(TURN_INPUT_TRACKED, 0);
        builder.add(SPIN_PROGRESS_TRACKED, 0.0f);
        builder.add(FORWARD_INPUT_TRACKED, 0.0f);
    }

    public int getTrackedTurnInput() {
        return this.dataTracker.get(TURN_INPUT_TRACKED);
    }

    public float getTrackedSpinProgress() {
        return this.dataTracker.get(SPIN_PROGRESS_TRACKED);
    }

    public float getTrackedForwardInput() {
        return this.dataTracker.get(FORWARD_INPUT_TRACKED);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.turnInput = nbt.getInt("TurnInput");
        this.dataTracker.set(TURN_INPUT_TRACKED, this.turnInput);
        this.dataTracker.set(SPIN_PROGRESS_TRACKED, 0.0f);
        this.dataTracker.set(FORWARD_INPUT_TRACKED, 0.0f);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("TurnInput", this.turnInput);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        // Mimic boat-like interaction: only mount on server, consume when successful
        if (player.isSneaking()) return ActionResult.CONSUME;
        if (player.shouldCancelInteraction()) return ActionResult.PASS; // crouching, etc.

        if (foldingPlayerUuid != null) {
            return ActionResult.CONSUME;
        }

        if (!player.getWorld().isClient()) {
            if (mountSuppressTicks > 0) {
                return ActionResult.CONSUME;
            }
            boolean mounted = player.startRiding(this);
            if (mounted) {
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.8f, 1.0f);
            }
            return mounted ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    private void startFoldWithoutItem(PlayerEntity player) {
        if (this.getWorld().isClient()) {
            return;
        }
        if (foldingPlayerUuid != null) {
            return;
        }
        foldingPlayerUuid = player.getUuid();
        this.dataTracker.set(FOLD_HIT_COUNT, 0);
        this.dataTracker.set(FOLD_HIT_AGE, 0);
    }

    private void clearFolding() {
        foldingPlayerUuid = null;
        this.dataTracker.set(FOLD_HIT_COUNT, 0);
        this.dataTracker.set(FOLD_HIT_AGE, 0);
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return passenger instanceof PlayerEntity && this.getFirstPassenger() == null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        // Coloca al jugador dentro de la hitbox del parapente en vez de encima
        Vec3d pos = this.getPos().add(0, -0.5, 0);
        positionUpdater.accept(passenger, pos.x, pos.y, pos.z);

        // Mantener la camara siguiendo el giro del parapente, con limite relativo +/-100 grados.
        float vehicleYaw = this.getYaw();
        if (!passengerYawInit) {
            passengerYawInit = true;
            passengerYawLast = vehicleYaw;
        }
        float yawDelta = MathHelper.wrapDegrees(vehicleYaw - passengerYawLast);
        passengerYawLast = vehicleYaw;

        float passengerYaw = passenger.getYaw() + yawDelta;
        float relativeYaw = MathHelper.wrapDegrees(passengerYaw - vehicleYaw);
        relativeYaw = MathHelper.clamp(relativeYaw, -100f, 100f);
        float clampedYaw = vehicleYaw + relativeYaw;
        passenger.setYaw(clampedYaw);
        passenger.setHeadYaw(clampedYaw);

        if (passenger instanceof LivingEntity living) {
            living.setBodyYaw(living.getYaw());
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
        return new EntitySpawnS2CPacket(this, entry);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.isOf(DamageTypes.LAVA)) {
            burnInLava();
            return true;
        }
        Entity attacker = source.getAttacker();

        if (attacker instanceof PlayerEntity player) {
            if (this.getPassengerList().isEmpty()) {
                if (foldingPlayerUuid == null || foldingPlayerUuid.equals(player.getUuid())) {
                    startFoldWithoutItem(player);
                    int hits = this.dataTracker.get(FOLD_HIT_COUNT) + 1;
                    this.dataTracker.set(FOLD_HIT_COUNT, hits);
                    this.dataTracker.set(FOLD_HIT_AGE, 0);
                    if (hits >= FOLD_HIT_REQUIRED) {
                        this.dropItem(world, ModItems.PARAGLIDER_ITEM);
                        this.remove(RemovalReason.KILLED);
                    }
                }
            }
            return true;
        }

        return false; // ignora otras fuentes de dano
    }

    @Override
    public void removePassenger(Entity passenger) {
        if (!this.forceDismount && passenger instanceof PlayerEntity player && !this.getWorld().isClient()) {
            boolean creative = player.getAbilities().creativeMode;
            boolean grounded = this.isGrounded();
            if (!creative && !grounded) {
                player.sendMessage(Text.translatable("message.paraglidingsimulator.dismount_flight_creative_or_ground"), true);
                return;
            }
        }
        super.removePassenger(passenger);
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(ModItems.PARAGLIDER_ITEM);
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    public int getFoldHitCount() {
        return this.dataTracker.get(FOLD_HIT_COUNT);
    }

    public int getFoldHitAge() {
        return this.dataTracker.get(FOLD_HIT_AGE);
    }

    public static int getFoldHitRequired() {
        return FOLD_HIT_REQUIRED;
    }

    public static int getFoldHitTimeoutTicks() {
        return FOLD_HIT_TIMEOUT_TICKS;
    }

    public void setMountSuppressTicks(int ticks) {
        this.mountSuppressTicks = Math.max(this.mountSuppressTicks, ticks);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean clientDamage(DamageSource source) {
        // Ignore client-side attack handling to avoid LivingEntity casts from other hooks
        return false;
    }



    private boolean isInLavaSurface() {
        BlockPos pos = this.getBlockPos();
        return this.getWorld().getFluidState(pos).isIn(FluidTags.LAVA)
                || this.getWorld().getFluidState(pos.down()).isIn(FluidTags.LAVA);
    }

    private boolean isInWaterSurface() {
        BlockPos pos = this.getBlockPos();
        return this.getWorld().getFluidState(pos).isIn(FluidTags.WATER)
                || this.getWorld().getFluidState(pos.down()).isIn(FluidTags.WATER);
    }

    private void burnInLava() {
        if (this.getWorld().isClient()) return;
        forceDismount = true;
        this.removeAllPassengers();
        this.dropItem((ServerWorld) this.getWorld(), ModItems.PARAGLIDER_ITEM);
        this.remove(RemovalReason.KILLED);
    }

    private void sinkInWater() {
        if (this.getWorld().isClient()) return;
        forceDismount = true;
        this.removeAllPassengers();
        this.dropItem((ServerWorld) this.getWorld(), ModItems.PARAGLIDER_ITEM);
        this.remove(RemovalReason.KILLED);
    }

    private PlayerEntity getControllingPlayer() {
        Entity first = this.getFirstPassenger();
        return first instanceof PlayerEntity p ? p : null;
    }

    private void applyLandingImpact(PlayerEntity driver) {
        if (driver == null) return;
        double verticalSpeed = this.getVelocity().y * 20.0; // m/s
        if (verticalSpeed > -1.0) return;

        double clamped = MathHelper.clamp(verticalSpeed, -6.0, -1.0);
        double t = (-clamped - 1.0) / 5.0;
        float damage = (float) (1.0 + (13.0 * t)); // 0.5 corazones -> 7 corazones
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            driver.damage(serverWorld, this.getWorld().getDamageSources().fall(), damage);
        }

        if (verticalSpeed <= -4.0) {
            forceDismount = true;
            this.removeAllPassengers();
            if (!this.getWorld().isClient()) {
                this.dropItem((ServerWorld) this.getWorld(), ModItems.PARAGLIDER_ITEM);
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    public boolean isGrounded() {
        if (this.isOnGround()) return true;

        Vec3d start = this.getPos().add(0, 0.1, 0);
        Vec3d end = start.add(0, -0.6, 0);
        BlockHitResult hit = this.getWorld().raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                this
        ));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
