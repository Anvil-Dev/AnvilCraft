package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
    @Shadow
    @Final
    private BeaconMenu.PaymentSlot paymentSlot;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(
        method = "updateEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/BeaconMenu$PaymentSlot;"
                + "remove(I)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private void updateEffects(
        Optional<MobEffect> primaryEffect, Optional<MobEffect> secondaryEffect, CallbackInfo ci
    ) {
        ItemStack item = this.paymentSlot.getItem();
        if (!item.is(ModItems.CURSED_GOLD_INGOT.get())) return;
        this.access.execute((level, pos) -> {
            if (!(level instanceof ServerLevel serverLevel)) return;
            if (this.anvilcraft$toCorrupted(level, pos)) {
                serverLevel.setBlockAndUpdate(pos, ModBlocks.CORRUPTED_BEACON.getDefaultState());
            }
            MinecraftServer server = serverLevel.getServer();
            GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE);
            if (!rule.get()) return;
            serverLevel.setWeatherParameters(
                0, ServerLevel.THUNDER_DURATION.sample(serverLevel.getRandom()), true, true
            );
        });
    }

    @Unique
    private boolean anvilcraft$toCorrupted(@NotNull Level level, @NotNull BlockPos pos) {
        RandomSource random = level.getRandom();
        double chance = random.nextDouble();
        int levels = anvilcraft$updateBase(level, pos.getX(), pos.getY(), pos.getZ());
        return switch (levels) {
            case 1 -> chance < 0.02;
            case 2 -> chance < 0.05;
            case 3 -> chance < 0.2;
            case 4 -> true;
            default -> false;
        };
    }

    @Unique
    private static int anvilcraft$updateBase(Level level, int x, int y, int z) {
        int k;
        int i = 0;
        int j = 1;
        while (j <= 4 && (k = y - j) >= level.getMinBuildHeight()) {
            boolean bl = true;
            block1:
            for (int l = x - j; l <= x + j && bl; ++l) {
                for (int m = z - j; m <= z + j; ++m) {
                    if (level.getBlockState(new BlockPos(l, k, m)).is(ModBlocks.CURSED_GOLD_BLOCK.get())) continue;
                    bl = false;
                    continue block1;
                }
            }
            if (!bl) break;
            i = j++;
        }
        return i;
    }
}
