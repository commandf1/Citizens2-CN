package net.citizensnpcs.nms.v1_17_R1.entity;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftRabbit;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_17_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_17_R1.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_17_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RabbitController extends MobEntityController {
    public RabbitController() {
        super(EntityRabbitNPC.class);
    }

    @Override
    public org.bukkit.entity.Rabbit getBukkitEntity() {
        return (org.bukkit.entity.Rabbit) super.getBukkitEntity();
    }

    public static class EntityRabbitNPC extends Rabbit implements NPCHolder {
        boolean calledNMSHeight = false;

        private final CitizensNPC npc;

        public EntityRabbitNPC(EntityType<? extends Rabbit> types, Level level) {
            this(types, level, null);
        }

        public EntityRabbitNPC(EntityType<? extends Rabbit> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        protected boolean canRide(Entity entity) {
            if (npc != null && (entity instanceof Boat || entity instanceof AbstractMinecart)) {
                return !npc.isProtected();
            }
            return super.canRide(entity);
        }

        @Override
        public boolean causeFallDamage(float f, float f1, DamageSource damagesource) {
            if (npc == null || !npc.isFlyable()) {
                return super.causeFallDamage(f, f1, damagesource);
            }
            return false;
        }

        @Override
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        protected void checkFallDamage(double d0, boolean flag, BlockState iblockdata, BlockPos blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.checkFallDamage(d0, flag, iblockdata, blockposition);
            }
        }

        @Override
        public void customServerAiStep() {
            super.customServerAiStep();
            if (npc != null) {
                NMSImpl.updateMinecraftAIState(npc, this);
                if (npc.getNavigator().isNavigating()) {
                    NMS.setShouldJump(getBukkitEntity());
                }
                npc.update();
            }
        }

        @Override
        public void dismountTo(double d0, double d1, double d2) {
            NMS.enderTeleportTo(npc, () -> super.dismountTo(d0, d1, d2));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return NMSImpl.getSoundEffect(npc, super.getAmbientSound(), NPC.Metadata.AMBIENT_SOUND);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new RabbitNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return NMSImpl.getSoundEffect(npc, super.getDeathSound(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.getHurtSound(damagesource), NPC.Metadata.HURT_SOUND);
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public LivingEntity getTarget() {
            return npc != null ? null : super.getTarget();
        }

        @Override
        public boolean isLeashed() {
            return NMSImpl.isLeashed(npc, super::isLeashed, this);
        }

        @Override
        public boolean isPushable() {
            return npc == null ? super.isPushable()
                    : npc.data().<Boolean> get(NPC.Metadata.COLLIDABLE, !npc.isProtected());
        }

        @Override
        public void knockback(double strength, double dx, double dz) {
            NMS.callKnockbackEvent(npc, (float) strength, dx, dz, (evt) -> super.knockback((float) evt.getStrength(),
                    evt.getKnockbackVector().getX(), evt.getKnockbackVector().getZ()));
        }

        @Override
        protected AABB makeBoundingBox() {
            return NMSBoundingBox.makeBB(npc, super.makeBoundingBox());
        }

        @Override
        public boolean onClimbable() {
            if (npc == null || !npc.isFlyable()) {
                return super.onClimbable();
            } else {
                return false;
            }
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> datawatcherobject) {
            if (npc != null && !calledNMSHeight) {
                calledNMSHeight = true;
                NMSImpl.checkAndUpdateHeight(this, datawatcherobject);
                calledNMSHeight = false;
                return;
            }

            super.onSyncedDataUpdated(datawatcherobject);
        }

        @Override
        public void push(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.push(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean save(CompoundTag save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public void setRabbitType(int i) {
            if (npc != null) {
                if (NMSImpl.getRabbitTypeField() == null)
                    return;
                this.entityData.set(NMSImpl.getRabbitTypeField(), i);
                return;
            }
            super.setRabbitType(i);
        }

        @Override
        public void travel(Vec3 vec3d) {
            if (npc == null || !npc.isFlyable()) {
                super.travel(vec3d);
            } else {
                NMSImpl.flyingMoveLogic(this, vec3d);
            }
        }

        @Override
        public boolean updateFluidHeightAndDoFluidPushing(Tag<Fluid> Tag, double d0) {
            return NMSImpl.fluidPush(npc, this, () -> super.updateFluidHeightAndDoFluidPushing(Tag, d0));
        }
    }

    public static class RabbitNPC extends CraftRabbit implements ForwardingNPCHolder {
        public RabbitNPC(EntityRabbitNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
