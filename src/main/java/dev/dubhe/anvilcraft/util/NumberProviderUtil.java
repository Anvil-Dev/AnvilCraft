package dev.dubhe.anvilcraft.util;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class NumberProviderUtil {
    private static final byte CONSTANT_TYPE = 1;
    private static final byte UNIFORM_TYPE = 2;
    private static final byte BINOMIAL_TYPE = 3;
    private static final byte UNKNOWN_TYPE = -1;

    public static void toNetwork(RegistryFriendlyByteBuf buf, NumberProvider numberProvider) {
        switch (numberProvider) {
            case ConstantValue constantValue -> {
                buf.writeByte(CONSTANT_TYPE);
                buf.writeFloat(constantValue.value());
            }
            case UniformGenerator uniformGenerator -> {
                buf.writeByte(UNIFORM_TYPE);
                toNetwork(buf, uniformGenerator.min());
                toNetwork(buf, uniformGenerator.max());
            }
            case BinomialDistributionGenerator binomialDistributionGenerator -> {
                buf.writeByte(BINOMIAL_TYPE);
                toNetwork(buf, binomialDistributionGenerator.n());
                toNetwork(buf, binomialDistributionGenerator.p());
            }
            case null, default -> buf.writeByte(UNKNOWN_TYPE);
        }
    }

    public static NumberProvider fromNetwork(RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case CONSTANT_TYPE -> ConstantValue.exactly(buf.readFloat());
            case UNIFORM_TYPE -> new UniformGenerator(fromNetwork(buf), fromNetwork(buf));
            case BINOMIAL_TYPE -> new BinomialDistributionGenerator(fromNetwork(buf), fromNetwork(buf));
            default -> ConstantValue.exactly(1);
        };
    }
}
