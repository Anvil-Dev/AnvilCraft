package dev.dubhe.anvilcraft.item.amulet;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.init.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ComradeAmuletItem extends AmuletItem {
    public ComradeAmuletItem(Properties properties) {
        super(properties.component(ModComponents.SIGNED_PLAYERS, SignedPlayers.EMPTY));
    }

    @Override
    public Holder<AmuletType> getType() {
        return ModAmuletTypes.COMRADE;
    }

    @SuppressWarnings("unused")
    public static void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
        amulet.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !ComradeAmuletItem.getSignedPlayers(amulet).isEmpty());
    }

    public static boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
        ItemStack comrade = Optional.of(InventoryUtil.getFirstItem(player.getInventory(), ModItems.COMRADE_AMULET))
            .filter(ItemStack::isEmpty)
            .orElse(InventoryUtil.getItemInCompat(player, stack -> stack.is(ModItems.COMRADE_AMULET)));
        return Optional.ofNullable(source.getEntity())
            .map(Entity::getUUID)
            .filter(uuid -> !comrade.isEmpty() && ComradeAmuletItem.canIgnorePlayer(comrade, uuid))
            .isPresent();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack amulet = player.getItemInHand(usedHand);

        if (ComradeAmuletItem.registerPlayerToAmulet(amulet, player)) {
            return InteractionResultHolder.success(amulet);
        } else {
            return InteractionResultHolder.pass(amulet);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.anvilcraft.comrade_amulet.tooltip").withStyle(ChatFormatting.GRAY));

        HashBiMap<Component, UUID> signedPlayers = ComradeAmuletItem.getSignedPlayers(stack);
        for (Component playerName : signedPlayers.keySet()) {
            tooltipComponents.add(Component.literal("- ").append(playerName.copy()));
        }
    }

    public static boolean registerPlayerToAmulet(ItemStack amulet, Player player) {
        HashBiMap<Component, UUID> signedPlayers = ComradeAmuletItem.getSignedPlayers(amulet);
        signedPlayers.put(player.getName(), player.getUUID());
        amulet.set(ModComponents.SIGNED_PLAYERS, new SignedPlayers(signedPlayers));
        return true;
    }

    public static boolean canIgnorePlayer(ItemStack amulet, UUID playerUUID) {
        return ComradeAmuletItem.getSignedPlayers(amulet).containsValue(playerUUID);
    }

    public static HashBiMap<Component, UUID> getSignedPlayers(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SIGNED_PLAYERS, SignedPlayers.EMPTY).playerInfos();
    }

    public record SignedPlayers(HashBiMap<Component, UUID> playerInfos) {
        public static final SignedPlayers EMPTY = new SignedPlayers(HashBiMap.create());
        public static final Codec<SignedPlayers> CODEC = Codec.unboundedMap(ComponentSerialization.FLAT_CODEC, UUIDUtil.CODEC)
            .xmap(HashBiMap::create, Function.identity())
            .xmap(SignedPlayers::new, SignedPlayers::playerInfos);
        public static final StreamCodec<RegistryFriendlyByteBuf, SignedPlayers> STREAM_CODEC = ByteBufCodecs.map(
            HashBiMap::create, ComponentSerialization.STREAM_CODEC, UUIDUtil.STREAM_CODEC
        ).map(SignedPlayers::new, SignedPlayers::playerInfos);
    }
}
