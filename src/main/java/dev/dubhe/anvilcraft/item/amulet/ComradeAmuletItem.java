package dev.dubhe.anvilcraft.item.amulet;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ComradeAmuletItem extends AbstractAmuletItem {
    public ComradeAmuletItem(Properties properties) {
        super(properties);
    }

    public static boolean registerPlayerToAmulet(ItemStack amulet, Player player) {
        try {
            HashBiMap<String, UUID> signedPlayers = getSignedPlayers(amulet);
            signedPlayers.put(player.getName().getString(), player.getUUID());
            amulet.set(ModComponents.SIGNED_PLAYERS, new SignedPlayers(signedPlayers));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean canIgnorePlayer(ItemStack amulet, UUID playerUUID) {
        return getSignedPlayers(amulet).containsValue(playerUUID);
    }

    public static HashBiMap<String, UUID> getSignedPlayers(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModComponents.SIGNED_PLAYERS))
            .map(signedPlayers -> HashBiMap.create(signedPlayers.playerInfos()))
            .orElse(HashBiMap.create());
    }

    @Override
    void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !getSignedPlayers(stack).isEmpty());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack amulet = player.getItemInHand(usedHand);

        if (registerPlayerToAmulet(amulet, player)) {
            return InteractionResultHolder.success(amulet);
        } else {
            return InteractionResultHolder.pass(amulet);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.anvilcraft.comrade_amulet.tooltip")
            .withStyle(ChatFormatting.GRAY));

        HashBiMap<String, UUID> signedPlayers = getSignedPlayers(stack);
        for (String playerName : signedPlayers.keySet()) {
            tooltipComponents.add(Component.literal("- " + playerName));
        }
    }

    public record SignedPlayers(Map<String, UUID> playerInfos) {
        public static final Codec<SignedPlayers> CODEC = Codec.unboundedMap(
            Codec.STRING, UUIDUtil.CODEC
        ).xmap(SignedPlayers::new, SignedPlayers::playerInfos);

        public static final StreamCodec<RegistryFriendlyByteBuf, SignedPlayers> STREAM_CODEC = StreamCodec.of(
            SignedPlayers::encode,
            SignedPlayers::decode
        );

        private static void encode(FriendlyByteBuf buf, SignedPlayers value) {
            buf.writeMap(value.playerInfos(), ByteBufCodecs.STRING_UTF8, UUIDUtil.STREAM_CODEC);
        }

        private static SignedPlayers decode(FriendlyByteBuf buf) {
            return new SignedPlayers(buf.readMap(ByteBufCodecs.STRING_UTF8, UUIDUtil.STREAM_CODEC));
        }
    }
}
