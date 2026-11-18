package greydesh.tactical.discs.entity;

import greydesh.tactical.discs.Config;
import greydesh.tactical.discs.init.ModEnchantments;
import greydesh.tactical.discs.init.ModEntities;
import greydesh.tactical.discs.init.ModItems;
import greydesh.tactical.discs.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DiscEntity extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(DiscEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(DiscEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOUNCE_COUNT =
            SynchedEntityData.defineId(DiscEntity.class, EntityDataSerializers.INT);

    private float damage = 5.0F;
    private double segmentDistanceTraveled = 0.0;
    private int returnTicks = 0;
    private double launchSpeed = 0.9;
    private boolean hasLumberjack = false;
    private boolean hasBouncing = false;
    private int fireAspectLevel = 0;
    private int sharpnessLevel = 0;
    private int baneOfArthropodsLevel = 0;
    private int smiteLevel = 0;
    private int knockbackLevel = 0;
    private int lootingLevel = 0;
    private int unbreakingLevel = 0;
    private InteractionHand thrownHand = InteractionHand.MAIN_HAND;
    private int originSlot = -1;
    private final Set<Integer> hitEntities = new HashSet<>();

    public DiscEntity(EntityType<? extends DiscEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
    }

    public DiscEntity(Level level, LivingEntity shooter, ItemStack itemStack) {
        this(level, shooter, itemStack, InteractionHand.MAIN_HAND, -1);
    }

    public DiscEntity(Level level, LivingEntity shooter, ItemStack itemStack, InteractionHand thrownHand) {
        this(level, shooter, itemStack, thrownHand, -1);
    }

    public DiscEntity(Level level, LivingEntity shooter, ItemStack itemStack, InteractionHand thrownHand, int originSlot) {
        this(ModEntities.DISC.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.15, shooter.getZ());
        this.setItem(itemStack);
        this.setThrownHand(thrownHand);
        this.originSlot = originSlot;
    }

    public void setThrownHand(InteractionHand hand) {
        this.thrownHand = hand != null ? hand : InteractionHand.MAIN_HAND;
    }

    public InteractionHand getThrownHand() {
        return this.thrownHand;
    }

    public void setOriginSlot(int slot) {
        this.originSlot = slot;
    }

    public int getOriginSlot() {
        return this.originSlot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM_STACK, new ItemStack(ModItems.IRON_DISC.get()));
        builder.define(DATA_RETURNING, false);
        builder.define(DATA_BOUNCE_COUNT, 0);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(DATA_ITEM_STACK, stack);
        this.updateEnchantmentFlags(stack);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ITEM_STACK.equals(key)) {
            this.updateEnchantmentFlags(this.getItem());
        }
    }

    private void updateEnchantmentFlags(ItemStack stack) {
        this.hasLumberjack = false;
        this.hasBouncing = false;
        this.fireAspectLevel = 0;
        this.sharpnessLevel = 0;
        this.baneOfArthropodsLevel = 0;
        this.smiteLevel = 0;
        this.knockbackLevel = 0;
        this.lootingLevel = 0;
        this.unbreakingLevel = 0;

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            if (holder.is(ModEnchantments.LUMBERJACK)) {
                this.hasLumberjack = true;
            } else if (holder.is(ModEnchantments.BOUNCING)) {
                this.hasBouncing = true;
            } else if (holder.is(Enchantments.FIRE_ASPECT)) {
                this.fireAspectLevel = level;
            } else if (holder.is(Enchantments.SHARPNESS)) {
                this.sharpnessLevel = level;
            } else if (holder.is(Enchantments.BANE_OF_ARTHROPODS)) {
                this.baneOfArthropodsLevel = level;
            } else if (holder.is(Enchantments.SMITE)) {
                this.smiteLevel = level;
            } else if (holder.is(Enchantments.KNOCKBACK)) {
                this.knockbackLevel = level;
            } else if (holder.is(Enchantments.LOOTING)) {
                this.lootingLevel = level;
            } else if (holder.is(Enchantments.UNBREAKING)) {
                this.unbreakingLevel = level;
            }
        }
    }

    public int getLootingLevel() {
        return this.lootingLevel;
    }

    public int getFireAspectLevel() {
        return this.fireAspectLevel;
    }

    public int getKnockbackLevel() {
        return this.knockbackLevel;
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM_STACK);
    }

    public boolean isReturning() {
        return this.entityData.get(DATA_RETURNING);
    }

    public void setReturning(boolean returning) {
        this.entityData.set(DATA_RETURNING, returning);
        if (returning) {
            this.noPhysics = true;
        }
    }

    public int getBounceCount() {
        return this.entityData.get(DATA_BOUNCE_COUNT);
    }

    public void setBounceCount(int bounces) {
        this.entityData.set(DATA_BOUNCE_COUNT, bounces);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        this.launchSpeed = Math.max(0.4, this.getDeltaMovement().length());
    }

    @Override
    public void tick() {
        super.tick();

        this.checkDiscCollision();

        if (this.isReturning()) {
            this.tickReturning();
        } else {
            this.tickFlying();
        }
    }

    private void checkDiscCollision() {
        if (this.isRemoved() || this.isReturning()) return;

        AABB box = this.getBoundingBox().inflate(0.35);
        List<DiscEntity> list = this.level().getEntitiesOfClass(DiscEntity.class, box, d -> d != this && !d.isRemoved() && !d.isReturning());
        for (DiscEntity other : list) {
            if (this.getId() < other.getId()) {
                this.handleDiscToDiscCollision(other);
                break;
            }
        }
    }

    private void handleDiscToDiscCollision(DiscEntity other) {
        Vec3 posA = this.position();
        Vec3 posB = other.position();
        Vec3 delta = posA.subtract(posB);
        Vec3 normal = delta.lengthSqr() > 1e-4 ? delta.normalize() : new Vec3(0, 1, 0);

        Vec3 motA = this.getDeltaMovement();
        Vec3 motB = other.getDeltaMovement();
        double speedA = Math.max(this.launchSpeed, motA.length());
        double speedB = Math.max(other.launchSpeed, motB.length());

        // Reflect velocities along contact normal
        double dotA = motA.dot(normal);
        Vec3 refA = motA.subtract(normal.scale(2.0 * dotA)).normalize().scale(speedA);
        if (refA.dot(normal) < 0.1) {
            refA = normal.scale(speedA);
        }

        Vec3 oppNormal = normal.scale(-1.0);
        double dotB = motB.dot(oppNormal);
        Vec3 refB = motB.subtract(oppNormal.scale(2.0 * dotB)).normalize().scale(speedB);
        if (refB.dot(oppNormal) < 0.1) {
            refB = oppNormal.scale(speedB);
        }

        // Apply new motions and nudge slightly apart
        this.setDeltaMovement(refA);
        this.setPos(posA.add(normal.scale(0.08)));
        this.hasImpulse = true;
        this.segmentDistanceTraveled = 0.0;
        this.hitEntities.clear();
        int bounceA = this.getBounceCount() + 1;
        this.setBounceCount(bounceA);
        if (this.hasBouncing) {
            this.damage = (float) (this.damage * 1.5);
        }

        other.setDeltaMovement(refB);
        other.setPos(posB.add(oppNormal.scale(0.08)));
        other.hasImpulse = true;
        other.segmentDistanceTraveled = 0.0;
        other.hitEntities.clear();
        int bounceB = other.getBounceCount() + 1;
        other.setBounceCount(bounceB);
        if (other.hasBouncing) {
            other.damage = (float) (other.damage * 1.5);
        }

        int maxBounces = Config.maxBounces > 0 ? Config.maxBounces : 5;
        if (bounceA >= maxBounces) {
            this.startReturning();
        }
        if (bounceB >= maxBounces) {
            other.startReturning();
        }

        // Audio & visual feedback: metallic clash!
        Vec3 mid = posA.add(posB).scale(0.5);
        this.level().playSound(null, mid.x, mid.y, mid.z,
                SoundEvents.VAULT_BREAK, SoundSource.PLAYERS, 1.2F, 1.8F + this.random.nextFloat() * 0.2F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    mid.x, mid.y, mid.z,
                    24, normal.x * 0.15, normal.y * 0.15, normal.z * 0.15, 0.15);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    mid.x, mid.y, mid.z,
                    16, 0.1, 0.1, 0.1, 0.1);
        }

        // Damage durability of both discs
        this.damageDiscItem(1);
        other.damageDiscItem(1);
    }

    private void tickReturning() {
        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive() || (owner instanceof Player player && player.isSpectator())) {
            if (!this.level().isClientSide) {
                this.spawnAtLocation(this.getItem());
                this.discard();
            }
            return;
        }

        Vec3 ownerEyePos = owner.getEyePosition().subtract(0, 0.2, 0);
        Vec3 toOwner = ownerEyePos.subtract(this.position());
        double dist = toOwner.length();

        // Pickup threshold
        if (dist < 1.3) {
            if (!this.level().isClientSide) {
                boolean added = false;
                if (owner instanceof Player player) {
                    ItemStack returnStack = this.getItem();
                    boolean placed = false;

                    // 1. Highest priority: return directly to the exact slot it was thrown from!
                    if (this.originSlot >= 0 && this.originSlot <= Inventory.SLOT_OFFHAND) {
                        if (player.getInventory().getItem(this.originSlot).isEmpty()) {
                            player.getInventory().setItem(this.originSlot, returnStack);
                            placed = true;
                        }
                    }

                    // 2. Second priority: if the origin slot became occupied
                    if (!placed) {
                        if (this.thrownHand == InteractionHand.OFF_HAND) {
                            if (player.getOffhandItem().isEmpty()) {
                                player.setItemInHand(InteractionHand.OFF_HAND, returnStack);
                                placed = true;
                            }
                        } else {
                            // If thrown from main hand, NEVER put into offhand!
                            if (player.getMainHandItem().isEmpty()) {
                                player.setItemInHand(InteractionHand.MAIN_HAND, returnStack);
                                placed = true;
                            }
                        }
                    }

                    // 3. Fallback: place in general inventory or drop if full
                    if (!placed && !player.getAbilities().instabuild) {
                        if (!player.getInventory().add(returnStack)) {
                            player.drop(returnStack, false);
                        }
                    }
                    added = true;
                } else {
                    this.spawnAtLocation(this.getItem());
                    added = true;
                }

                if (added) {
                    this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.3F);
                    this.discard();
                }
            }
            return;
        }

        // Accelerate smoothly towards owner (comfortably readable return speed)
        double speed = Math.min(1.05, 0.45 + this.returnTicks * 0.035);
        this.returnTicks++;
        Vec3 moveVec = toOwner.normalize().scale(speed);
        this.setDeltaMovement(moveVec);

        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(moveVec);

        // Pierce through entities on return flight if no bouncing enchantment!
        if (!this.hasBouncing) {
            AABB scanBox = this.getBoundingBox().expandTowards(moveVec).inflate(0.5);
            List<Entity> entities = this.level().getEntities(this, scanBox, this::canHitEntity);
            for (Entity entity : entities) {
                if (!this.hitEntities.contains(entity.getId())) {
                    AABB entityBox = entity.getBoundingBox().inflate(0.3);
                    Optional<Vec3> clip = entityBox.clip(currentPos, nextPos);
                    if (clip.isPresent() || entityBox.intersects(this.getBoundingBox())) {
                        this.hitEntities.add(entity.getId());
                        this.onHitEntityPierce(entity);
                    }
                }
            }
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
    }

    private boolean isLogBlock(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private boolean isVaseBlock(BlockState state) {
        return state.is(Blocks.DECORATED_POT);
    }

    private void tickFlying() {
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        double stepDist = motion.length();

        if (this.fireAspectLevel > 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX(), this.getY() + 0.05, this.getZ(), 0, 0, 0);
        }

        // Check segment distance limit (12 blocks)
        double maxDist = Config.maxSegmentDistance > 0 ? Config.maxSegmentDistance : 12.0;
        if (this.segmentDistanceTraveled + stepDist >= maxDist) {
            this.startReturning();
            return;
        }

        Vec3 nextPos = currentPos.add(motion);

        // Block raycast
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        Vec3 collisionEndPos = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getLocation() : nextPos;

        // Entity collisions along the step
        AABB scanBox = this.getBoundingBox().expandTowards(motion).inflate(0.5);
        List<Entity> entities = this.level().getEntities(this, scanBox, this::canHitEntity);
        Entity firstHitEntity = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (!this.hitEntities.contains(entity.getId())) {
                AABB entityBox = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> clip = entityBox.clip(currentPos, collisionEndPos);
                if (clip.isPresent() || entityBox.intersects(this.getBoundingBox())) {
                    double dist = entity.distanceToSqr(this);
                    if (dist < closestDist) {
                        closestDist = dist;
                        firstHitEntity = entity;
                    }
                    if (!this.hasBouncing) {
                        // Without bouncing enchantment: pierces through all targets in its path
                        this.hitEntities.add(entity.getId());
                        this.onHitEntityPierce(entity);
                    }
                }
            }
        }

        // With Bouncing enchantment: does NOT pierce through, but hits first target and chains to the nearest next entity!
        if (this.hasBouncing && firstHitEntity != null) {
            this.hitEntities.add(firstHitEntity.getId());
            this.handleEntityBounce(firstHitEntity);
            return;
        }

        // Handle block bounce or proceed
        if (blockHit.getType() != HitResult.Type.MISS) {
            BlockPos hitPos = blockHit.getBlockPos();
            BlockState hitState = this.level().getBlockState(hitPos);

            // Ring bell when hit by disc!
            if (hitState.is(Blocks.BELL) && hitState.getBlock() instanceof BellBlock bellBlock) {
                Player player = this.getOwner() instanceof Player p ? p : null;
                bellBlock.attemptToRing(player, this.level(), hitPos, blockHit.getDirection());
            }

            // Shatter decorated pot (vase) into sherds and contents, only decorated pot!
            if (this.isVaseBlock(hitState)) {
                if (!this.level().isClientSide) {
                    this.level().setBlock(hitPos, hitState.setValue(DecoratedPotBlock.CRACKED, true), 4);
                    this.level().destroyBlock(hitPos, true, this);
                }
                this.setPos(nextPos);
                this.segmentDistanceTraveled += stepDist;
            } else if (this.hasLumberjack && this.isLogBlock(hitState)) {
                // Lumberjack effect: breaks log block and drops it, then continues flying through!
                if (!this.level().isClientSide) {
                    this.level().destroyBlock(hitPos, true, this.getOwner());
                }
                this.setPos(nextPos);
                this.segmentDistanceTraveled += stepDist;
            } else {
                this.handleBlockRicochet(blockHit);
            }
        } else {
            this.setPos(nextPos);
            this.segmentDistanceTraveled += stepDist;
        }
    }

    private boolean damageDiscItem(int amount) {
        if (this.unbreakingLevel > 0) {
            if (this.random.nextInt(this.unbreakingLevel + 1) != 0) {
                return false;
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            Entity owner = this.getOwner();
            if (owner instanceof Player player && player.getAbilities().instabuild) {
                return false;
            }

            ItemStack stack = this.getItem();
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                ServerPlayer serverPlayer = owner instanceof ServerPlayer sp ? sp : null;
                boolean[] broken = new boolean[]{false};
                stack.hurtAndBreak(amount, serverLevel, serverPlayer, item -> {
                    broken[0] = true;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                            this.getX(), this.getY(), this.getZ(),
                            16, 0.1, 0.1, 0.1, 0.05);
                    this.discard();
                });

                if (broken[0]) {
                    return true;
                }

                this.setItem(stack);
            }
        }
        return false;
    }

    private void applyFlightEntityHit(Entity target) {
        if (!this.level().isClientSide) {
            float totalDamage = this.damage;

            // Sharpness bonus
            if (this.sharpnessLevel > 0) {
                totalDamage += 1.0F + (this.sharpnessLevel - 1) * 0.5F;
            }

            // Smite bonus
            if (this.smiteLevel > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
                totalDamage += this.smiteLevel * 2.5F;
            }

            // Bane of Arthropods bonus
            if (this.baneOfArthropodsLevel > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                totalDamage += this.baneOfArthropodsLevel * 2.5F;
            }

            target.hurt(this.damageSources().trident(this, this.getOwner()), totalDamage);

            // Fire Aspect
            if (this.fireAspectLevel > 0) {
                target.igniteForSeconds(this.fireAspectLevel * 4);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            target.getX(), target.getY(0.5), target.getZ(),
                            12, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Bane of Arthropods: slowness
            if (this.baneOfArthropodsLevel > 0 && target instanceof LivingEntity living && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                int duration = 20 + this.random.nextInt(10 * this.baneOfArthropodsLevel);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
            }

            // Knockback: pushes entity away along disc flight vector!
            if (this.knockbackLevel > 0 && target instanceof LivingEntity living) {
                Vec3 motion = this.getDeltaMovement();
                Vec3 dir = motion.lengthSqr() > 1.0E-6 ? motion.normalize() : this.getLookAngle();
                double strength = 0.5 + this.knockbackLevel * 0.5;
                living.knockback(strength, -dir.x, -dir.z);
                living.hasImpulse = true;
                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.2F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY(0.5), target.getZ(),
                        1, 0, 0, 0, 0);

                if (this.sharpnessLevel > 0 || this.smiteLevel > 0 || this.baneOfArthropodsLevel > 0) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY(0.5), target.getZ(),
                            10, 0.2, 0.2, 0.2, 0.1);
                }
            }

            this.damageDiscItem(1);
        }
    }

    private void onHitEntityPierce(Entity target) {
        this.applyFlightEntityHit(target);
    }

    private void handleEntityBounce(Entity target) {
        this.applyFlightEntityHit(target);
        if (this.isRemoved()) {
            return;
        }

        // Multiply damage on each bounce
        this.damage = (float) (this.damage * 1.5);

        int currentBounces = this.getBounceCount() + 1;
        this.setBounceCount(currentBounces);
        this.segmentDistanceTraveled = 0.0;

        int maxBounces = Config.maxBounces > 0 ? Config.maxBounces : 5;
        if (currentBounces >= maxBounces) {
            this.startReturning();
            return;
        }

        // Search for nearest other living entity within 12 blocks
        double searchRadius = 12.0;
        AABB targetBox = this.getBoundingBox().inflate(searchRadius);
        List<LivingEntity> potentialTargets = this.level().getEntitiesOfClass(LivingEntity.class, targetBox, e ->
                e.isAlive()
                && e != this.getOwner()
                && e != target
                && !this.hitEntities.contains(e.getId())
                && this.canHitEntity(e)
        );

        LivingEntity nextTarget = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (LivingEntity potential : potentialTargets) {
            double distSq = this.distanceToSqr(potential);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nextTarget = potential;
            }
        }

        if (nextTarget != null) {
            // Re-aim disc towards next target and maintain speed
            Vec3 targetCenter = nextTarget.getEyePosition().subtract(0, 0.2, 0);
            Vec3 newDir = targetCenter.subtract(this.position()).normalize();
            double speed = Math.max(this.launchSpeed, this.getDeltaMovement().length());
            this.setDeltaMovement(newDir.scale(speed));
            this.hasImpulse = true;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else {
            // No more nearby targets found in chain -> return back to owner
            this.startReturning();
        }
    }

    private void handleBlockRicochet(BlockHitResult hitResult) {
        if (this.damageDiscItem(1)) {
            return;
        }

        int currentBounces = this.getBounceCount();
        int maxBounces = Config.maxBounces > 0 ? Config.maxBounces : 5;

        if (currentBounces < maxBounces) {
            int newBounces = currentBounces + 1;
            this.setBounceCount(newBounces);
            this.segmentDistanceTraveled = 0.0;
            this.hitEntities.clear(); // Can hit enemies again on the rebound!

            // Bouncing enchantment effect: multiplies damage on every bounce!
            if (this.hasBouncing) {
                this.damage = (float) (this.damage * 1.5);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                            15, 0.2, 0.2, 0.2, 0.1);
                }
            }

            Direction face = hitResult.getDirection();
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 motion = this.getDeltaMovement();

            double speed = Math.max(this.launchSpeed * 0.95, motion.length());
            double dot = motion.dot(normal);

            // Reflection formula: v' = v - 2(v . n)n
            Vec3 reflected = motion.subtract(normal.scale(2.0 * dot)).normalize().scale(speed);

            this.setDeltaMovement(reflected);
            this.setPos(hitResult.getLocation().add(normal.scale(0.08)));
            this.hasImpulse = true;

            // Ricochet sound and particles
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.4F + this.random.nextFloat() * 0.3F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z,
                        12, normal.x * 0.15, normal.y * 0.15, normal.z * 0.15, 0.1);
            }

            if (newBounces >= maxBounces) {
                this.startReturning();
            }
        } else {
            this.startReturning();
        }
    }

    private void startReturning() {
        this.setReturning(true);
        this.hitEntities.clear(); // Clear so entities can be sliced again on return flight!
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target == this.getOwner()) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved() && !this.isReturning();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || this.isReturning()) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            ItemStack heldMain = player.getMainHandItem();
            boolean hasDiscInMainHand = heldMain.is(ModTags.DISCS);

            if (hasDiscInMainHand) {
                // Ricochet off the player's parry away into another direction!
                Vec3 normal = this.position().subtract(player.getEyePosition()).normalize();
                Vec3 motion = this.getDeltaMovement();
                double speed = Math.max(this.launchSpeed, motion.length());

                // Reflect: v' = v - 2(v . n)n
                double dot = motion.dot(normal);
                Vec3 reflected = motion.subtract(normal.scale(2.0 * dot)).normalize().scale(speed);

                // Ensure reflected velocity pushes away from player
                if (reflected.dot(normal) < 0.2) {
                    reflected = normal.add(player.getLookAngle().scale(0.5)).normalize().scale(speed);
                }

                this.setDeltaMovement(reflected);
                this.hasImpulse = true;
                this.segmentDistanceTraveled = 0.0;
                this.hitEntities.clear(); // Can hit enemies in new ricochet direction

                // Increment bounce count
                int currentBounces = this.getBounceCount() + 1;
                this.setBounceCount(currentBounces);

                // If disc has Bouncing enchantment, multiply damage!
                if (this.hasBouncing) {
                    this.damage = (float) (this.damage * 1.5);
                }

                int maxBounces = Config.maxBounces > 0 ? Config.maxBounces : 5;
                if (currentBounces >= maxBounces) {
                    this.startReturning();
                }

                // Sound feedback: block.vault.break with high pitch!
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VAULT_BREAK, SoundSource.PLAYERS, 1.2F, 1.75F + this.random.nextFloat() * 0.2F);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            this.getX(), this.getY(), this.getZ(),
                            20, normal.x * 0.2, normal.y * 0.2, normal.z * 0.2, 0.15);
                }

                // Consume 1 durability from the player's main hand disc
                if (!player.getAbilities().instabuild && this.level() instanceof ServerLevel serverLevel) {
                    ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
                    heldMain.hurtAndBreak(1, serverLevel, serverPlayer, item -> {
                        this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    });
                }

                // Also damage the flying disc by 1 from the clash
                this.damageDiscItem(1);

                return true;
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Damage", this.damage);
        compound.putInt("Bounces", this.getBounceCount());
        compound.putBoolean("Returning", this.isReturning());
        compound.putDouble("SegmentDist", this.segmentDistanceTraveled);
        compound.putDouble("LaunchSpeed", this.launchSpeed);
        compound.putBoolean("HasLumberjack", this.hasLumberjack);
        compound.putBoolean("HasBouncing", this.hasBouncing);
        compound.putString("ThrownHand", this.thrownHand.name());
        compound.putInt("OriginSlot", this.originSlot);
        if (!this.getItem().isEmpty()) {
            compound.put("Item", this.getItem().save(this.registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.damage = compound.getFloat("Damage");
        this.setBounceCount(compound.getInt("Bounces"));
        this.setReturning(compound.getBoolean("Returning"));
        this.segmentDistanceTraveled = compound.getDouble("SegmentDist");
        this.launchSpeed = compound.getDouble("LaunchSpeed");
        this.hasLumberjack = compound.getBoolean("HasLumberjack");
        this.hasBouncing = compound.getBoolean("HasBouncing");
        if (compound.contains("OriginSlot", 99)) {
            this.originSlot = compound.getInt("OriginSlot");
        }
        if (compound.contains("ThrownHand", 8)) {
            try {
                this.thrownHand = InteractionHand.valueOf(compound.getString("ThrownHand"));
            } catch (IllegalArgumentException ignored) {
                this.thrownHand = InteractionHand.MAIN_HAND;
            }
        }
        if (compound.contains("Item", 10)) {
            ItemStack stack = ItemStack.parse(this.registryAccess(), compound.getCompound("Item")).orElse(ItemStack.EMPTY);
            this.setItem(stack);
        }
    }
}
