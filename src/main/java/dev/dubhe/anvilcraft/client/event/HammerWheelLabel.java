package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilLaserInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.FacingWithAxis;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.block.utility.redstone.MountedRedstone;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;

final class HammerWheelLabel {
    private HammerWheelLabel() {
    }

    static <T extends Comparable<T>> Component describe(BlockState state, Property<T> property) {
        T value = state.getValue(property);
        if (value instanceof Direction facing) {
            var page = StateUtil.hammerPageProperty(state, property);
            if (page == MountedRedstone.ATTACHMENT) {
                String face = state.getValue(MountedRedstone.ATTACHMENT).getName();
                return label("face." + face, direction(MountedRedstone.inputDirection(state)));
            }
            if (page == BlockStateProperties.HALF) {
                return label(state.getValue(BlockStateProperties.HALF) == Half.TOP ? "top" : "bottom", direction(facing));
            }
            if (page != null) {
                return label(Boolean.TRUE.equals(state.getValue(page)) ? "inverted" : "upright", direction(facing));
            }
            return direction(facing);
        }
        if (value instanceof Direction.Axis axis) {
            return label("axis." + axis.getSerializedName());
        }
        if (value instanceof FacingWithAxis facing) {
            return label("axis_facing", label("axis." + facing.getAxis().getSerializedName()), direction(facing.getFacing().getOpposite()));
        }
        if (value instanceof Orientation custom) {
            return orientation(FrontAndTop.valueOf(custom.name()));
        }
        if (value instanceof FrontAndTop orientation) {
            return orientation(orientation);
        }
        if (value instanceof Boolean enabled) {
            if (state.getBlock() instanceof CelestialForgingAnvilInterfaceBlock && property == CelestialForgingAnvilInterfaceBlock.ACTIVE) {
                if (!enabled) {
                    return label("passive");
                }
                return label(state.getBlock() instanceof CelestialForgingAnvilLaserInterfaceBlock ? "active_emit" : "active_output");
            }
            return label(enabled ? "on" : "off");
        }
        String key = state.getBlock().getDescriptionId() + ".property." + property.getName() + "." + property.getName(value);
        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return label("option", property.getPossibleValues().indexOf(value) + 1);
    }

    private static Component direction(Direction direction) {
        return Component.translatable("screen.anvilcraft.button.direction." + direction.getName());
    }

    private static Component orientation(FrontAndTop orientation) {
        return switch (orientation.front()) {
            case UP -> label("up", direction(orientation.top()));
            case DOWN -> label("down", direction(orientation.top()));
            default -> label("upright", direction(orientation.front()));
        };
    }

    private static Component label(String key, Object... arguments) {
        return Component.translatable("screen.anvilcraft.hammer." + key, arguments);
    }
}
