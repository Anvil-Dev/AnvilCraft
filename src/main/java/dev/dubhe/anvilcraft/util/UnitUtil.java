package dev.dubhe.anvilcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class UnitUtil {
    public static final String INFINITE_POWER = "∞";

    public static Component energyUnit(int energy, boolean original) {
        if (original) {
            return Component.literal(String.valueOf(energy)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" FE").withStyle(ChatFormatting.GRAY));
        }
        if (energy < 1000) {
            return Component.literal(String.valueOf(energy)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" FE").withStyle(ChatFormatting.GRAY));
        } else if (energy < 1000000) {
            double kfeValue = (double) energy / 1000;
            double truncated = Math.floor(kfeValue * 100) / 100;
            MutableComponent number = truncated == Math.floor(truncated)
                ? Component.literal(String.format("%.0f", truncated)).withStyle(ChatFormatting.GOLD)
                : Component.literal(String.format("%.2f", truncated)).withStyle(ChatFormatting.GOLD);
            return number.append(Component.literal(" kFE").withStyle(ChatFormatting.GRAY));
        } else {
            double mfeValue = (double) energy / 1000000;
            double truncated = Math.floor(mfeValue * 100) / 100;
            MutableComponent number = truncated == Math.floor(truncated)
                ? Component.literal(String.format("%.0f", truncated)).withStyle(ChatFormatting.GOLD)
                : Component.literal(String.format("%.2f", truncated)).withStyle(ChatFormatting.GOLD);
            return number.append(Component.literal(" MFE").withStyle(ChatFormatting.GRAY));
        }
    }

    public static String electricityUnit(int amount, boolean original, boolean infinite) {
        if (infinite) {
            return INFINITE_POWER;
        }
        if (original) {
            return amount + " kW";
        }
        if (amount < 1000) {
            return String.format("%d kW", amount);
        } else if (amount < 1000000) {
            double mwValue = (double) amount / 1000;
            double truncated = Math.floor(mwValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0f MW", truncated);
            } else {
                return String.format("%.2f MW", truncated);
            }
        } else {
            double gwValue = (double) amount / 1000000;
            double truncated = Math.floor(gwValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0f GW", truncated);
            } else {
                return String.format("%.2f GW", truncated);
            }
        }
    }

    public static String electricityUnit(int consume, int generate, boolean original) {
        if (original) {
            return consume + "/" + generate + " kW";
        }

        if (generate < 1000) {
            return String.format("%d/%d kW", consume, generate);
        } else if (generate < 1000000) {
            double consumeMW = (double) consume / 1000;
            double generateMW = (double) generate / 1000;
            double consumeTruncated = Math.floor(consumeMW * 100) / 100;
            double generateTruncated = Math.floor(generateMW * 100) / 100;

            if (consumeTruncated == Math.floor(consumeTruncated) && generateTruncated == Math.floor(generateTruncated)) {
                return String.format("%.0f/%.0f MW", consumeTruncated, generateTruncated);
            } else {
                return String.format("%.2f/%.2f MW", consumeTruncated, generateTruncated);
            }
        } else {
            double consumeMW = (double) consume / 1000000;
            double generateMW = (double) generate / 1000000;
            double consumeTruncated = Math.floor(consumeMW * 100) / 100;
            double generateTruncated = Math.floor(generateMW * 100) / 100;

            if (consumeTruncated == Math.floor(consumeTruncated) && generateTruncated == Math.floor(generateTruncated)) {
                return String.format("%.0f/%.0f GW", consumeTruncated, generateTruncated);
            } else {
                return String.format("%.2f/%.2f GW", consumeTruncated, generateTruncated);
            }
        }
    }

    public static String fluidUnit(int amount, boolean original) {
        if (original) {
            return amount + " mB";
        }
        if (amount < 1000) {
            return String.format("%d mB", amount);
        } else if (amount < 1000000) {
            double bucketValue = (double) amount / 1000;
            double truncated = Math.floor(bucketValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0f B", truncated);
            } else {
                return String.format("%.2f B", truncated);
            }
        } else {
            double kiloBucketValue = (double) amount / 1000000;
            double truncated = Math.floor(kiloBucketValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0f KB", truncated);
            } else {
                return String.format("%.2f KB", truncated);
            }
        }
    }
}
