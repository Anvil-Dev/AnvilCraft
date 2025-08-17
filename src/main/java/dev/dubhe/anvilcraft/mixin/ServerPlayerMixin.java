package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements IDynamicPowerComponentHolder {
    @Shadow
    @Final
    public MinecraftServer server;
    @Unique
    private DynamicPowerComponent anvilCraft$component;

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void constructPowerComponent(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, CallbackInfo ci) {
        this.anvilCraft$component = new DynamicPowerComponent(
            this,
            this::anvilCraft$getPowerSupplyingBoundingBox
        );
    }

    @Unique
    public AABB anvilCraft$getPowerSupplyingBoundingBox() {
        return this.getBoundingBox().inflate(0.5);
    }

    @Override
    public void anvilCraft$gridTick() {
        ItemStack stack = IonoCraftBackpackItem.getByPlayer(this);
        if (IonoCraftBackpackItem.canModify(stack, this.anvilCraft$component) && IonoCraftBackpackItem.getFlightTime(stack) < AnvilCraft.config.ionoCraftBackpackMaxFlightTime) {
            IonoCraftBackpackItem.addFlightTime(stack, AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 120);
        }
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource modifySource(DamageSource value, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (value.getEntity() instanceof FallingBlockEntity falling
            && Util.instanceOfAny(falling.getBlockState().getBlock(), EmberAnvilBlock.class, TranscendenceAnvilBlock.class)
        ) {
            ServerPlayer killer = AnvilCraftFakePlayers.anvilCraftKiller.offerPlayer((ServerLevel) this.level());
            this.lastHurtByPlayer = killer;
            this.lastHurtByPlayerTime = 1;
            killerRef.set(killer);
            DamageSource source = new DamageSource(
                this.level().damageSources().playerAttack(killer).typeHolder(),
                falling, killer, value.getSourcePosition());
            if (falling.getBlockState().getBlock() instanceof TranscendenceAnvilBlock) {
                AnvilCraftFakePlayers.anvilCraftKiller.enableLooting5((ServerLevel) this.level(), killer);
            }
            return source;
        }
        return value;
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void disableKiller(DamageSource cause, CallbackInfo ci, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (killerRef.get() == null) return;
        AnvilCraftFakePlayers.anvilCraftKiller.disable(killerRef.get());
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        anvilCraft$component.switchTo(null);
    }

    @Override
    public DynamicPowerComponent anvilCraft$getPowerComponent() {
        return anvilCraft$component;
    }
}
