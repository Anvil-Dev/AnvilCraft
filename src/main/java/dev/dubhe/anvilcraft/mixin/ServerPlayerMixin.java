package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements IDynamicPowerComponentHolder {
    @Shadow
    @Final
    public MinecraftServer server;
    @Unique
    private DynamicPowerComponent anvilcraft$component;

    public ServerPlayerMixin(Level level, BlockPos pos, float rotY, GameProfile gameProfile) {
        super(level, pos, rotY, gameProfile);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void constructPowerComponent(
        MinecraftServer server,
        ServerLevel level,
        GameProfile gameProfile,
        ClientInformation clientInformation,
        CallbackInfo ci
    ) {
        this.anvilcraft$component = new DynamicPowerComponent(
            this,
            this::anvilcraft$getPowerSupplyingBoundingBox
        );
    }

    @Unique
    public AABB anvilcraft$getPowerSupplyingBoundingBox() {
        return this.getBoundingBox().inflate(0.5);
    }

    @Override
    public void anvilcraft$gridTick() {
        ItemStack stack = IonoCraftBackpackItem.getByPlayer(this);
        if (IonoCraftBackpackItem.canModify(
            stack,
            this.anvilcraft$component
        ) && IonoCraftBackpackItem.getEnergyStored(stack) < IonoCraftBackpackItem.MAX_ENERGY) {
            PowerGrid powerGrid = this.anvilcraft$component.getPowerGrid();
            if (powerGrid != null && powerGrid.isWorking()) {
                int chargeAmount = 0;
                int consumption = this.anvilcraft$component.getPowerConsumption();
                
                if (consumption >= 512) {
                    chargeAmount = 192;
                } else if (consumption >= 256) {
                    chargeAmount = 96;
                } else if (consumption >= 128) {
                    chargeAmount = 48;
                } else if (consumption >= 64) {
                    chargeAmount = 24;
                }
                
                IonoCraftBackpackItem.addEnergy(stack, chargeAmount * IonoCraftBackpackItem.FLIGHT_CONSUMPTION);
            }
        }
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource modifySource(DamageSource value, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (value.getEntity() instanceof FallingBlockEntity falling
            && Util.instanceOfAny(falling.getBlockState().getBlock(), EmberAnvilBlock.class, TranscendenceAnvilBlock.class)
        ) {
            ServerPlayer killer = AnvilCraftFakePlayers.anvilcraftKiller.offerPlayer((ServerLevel) this.level());
            this.lastHurtByPlayer = killer;
            this.lastHurtByPlayerTime = 1;
            killerRef.set(killer);
            DamageSource source = new DamageSource(
                this.level().damageSources().playerAttack(killer).typeHolder(),
                falling, killer, value.getSourcePosition()
            );
            if (falling.getBlockState().getBlock() instanceof TranscendenceAnvilBlock) {
                AnvilCraftFakePlayers.anvilcraftKiller.enableLooting5((ServerLevel) this.level(), killer);
            }
            return source;
        }
        return value;
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void disableKiller(DamageSource cause, CallbackInfo ci, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (killerRef.get() == null) return;
        AnvilCraftFakePlayers.anvilcraftKiller.disable(killerRef.get());
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        anvilcraft$component.switchTo(null);
    }

    @Override
    public DynamicPowerComponent anvilcraft$getPowerComponent() {
        return anvilcraft$component;
    }
}