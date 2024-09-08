package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

public class GeodeItem extends Item {
    public GeodeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand usedHand
    ) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        BlockPos pos = player.getOnPos().below();
        player.getCooldowns().addCooldown(itemStack.getItem(), AnvilCraft.config.geodeCooldown * 20);
        if (!level.isClientSide) return InteractionResultHolder.success(itemStack);
        int interval = AnvilCraft.config.geodeInterval;
        int radius = AnvilCraft.config.geodeRadius;
        block:
        for (int x = -radius; x <= radius; x += interval) {
            for (int z = -radius; z <= radius; z += interval) {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + x, pos.getZ() + z);
                for (int y = level.getMinBuildHeight(); y <= height; y += interval) {
                    BlockPos offsetPos = new BlockPos(pos.getX(), 0, pos.getZ()).offset(x, y, z);
                    BlockState state = level.getBlockState(offsetPos);
                    if (!state.is(BlockTags.CRYSTAL_SOUND_BLOCKS)) continue;
                    MutableComponent component = ComponentUtils.wrapInSquareBrackets(Component.translatable(
                            "chat.coordinates",
                            offsetPos.getX(),
                            offsetPos.getY(),
                            offsetPos.getZ()
                        ))
                        .withStyle(text -> text.withColor(ChatFormatting.GREEN)
                            .withClickEvent(
                                new ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    "/tp @s "
                                        + offsetPos.getX()
                                        + " "
                                        + offsetPos.getY()
                                        + " "
                                        + offsetPos.getZ()
                                ))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("chat.coordinates.tooltip")
                            )));
                    player.sendSystemMessage(Component.translatable(
                        "item.anvilcraft.geode.find", component
                    ));
                    IBlockHighlightUtil.highlightBlock(level, offsetPos);
                    break block;
                }
            }
        }
        player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE);
        return InteractionResultHolder.success(itemStack);
    }
}
