package dev.dubhe.anvilcraft.util;

public class UnitUtil {
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
                return String.format("%.0f MW", truncated);
            } else {
                return String.format("%.2f MW", truncated);
            }
        } else {
            double gwValue = (double) power / 1000000;
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
}
