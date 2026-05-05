package dev.dubhe.anvilcraft.util;

public class UnitUtil {
    public static String energyUnit(int energy, boolean original) {
        if (original) {
            return energy + " kJ";
        }
        if (energy < 1000) {
            return String.format("%d kJ", energy);
        } else {
            double mjValue = (double) energy / 1000;
            double truncated = Math.floor(mjValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0F MJ", truncated);
            } else {
                return String.format("%.2F MJ", truncated);
            }
        }
    }

    public static String electricityUnit(int power, boolean original) {
        if (original) {
            return power + " kW";
        }
        if (power < 1000) {
            return String.format("%d kW", power);
        } else if (power < 1000000) {
            double mwValue = (double) power / 1000;
            double truncated = Math.floor(mwValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0F MW", truncated);
            } else {
                return String.format("%.2F MW", truncated);
            }
        } else {
            double gwValue = (double) power / 1000000;
            double truncated = Math.floor(gwValue * 100) / 100;
            if (truncated == Math.floor(truncated)) {
                return String.format("%.0F GW", truncated);
            } else {
                return String.format("%.2F GW", truncated);
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
                return String.format("%.0F/%.0F MW", consumeTruncated, generateTruncated);
            } else {
                return String.format("%.2F/%.2F MW", consumeTruncated, generateTruncated);
            }
        } else {
            double consumeMW = (double) consume / 1000000;
            double generateMW = (double) generate / 1000000;
            double consumeTruncated = Math.floor(consumeMW * 100) / 100;
            double generateTruncated = Math.floor(generateMW * 100) / 100;

            if (consumeTruncated == Math.floor(consumeTruncated) && generateTruncated == Math.floor(generateTruncated)) {
                return String.format("%.0F/%.0F GW", consumeTruncated, generateTruncated);
            } else {
                return String.format("%.2F/%.2F GW", consumeTruncated, generateTruncated);
            }
        }
    }
}
