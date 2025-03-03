package dev.dubhe.anvilcraft.mixin;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
        ItemStack itemStack = IonoCraftBackpackItem.getByPlayer(this);
        if (itemStack.is(ModItems.IONOCRAFT_BACKPACK)
            && anvilCraft$component.getPowerGrid() != null
            && anvilCraft$component.getPowerGrid().isWorking()
        ) {
            int flightTime = IonoCraftBackpackItem.getFlightTime(itemStack);
            flightTime = flightTime + AnvilCraft.config.ionoCraftBackpackMaxFlightTime / 120;
            IonoCraftBackpackItem.setFlightTime(itemStack, flightTime);
        }
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
