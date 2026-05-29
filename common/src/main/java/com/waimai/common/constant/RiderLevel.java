package com.waimai.common.constant;

public class RiderLevel {
    public static final String BRONZE = "BRONZE";
    public static final String SILVER = "SILVER";
    public static final String GOLD = "GOLD";
    public static final String DIAMOND = "DIAMOND";

    public static String computeLevel(int levelScore) {
        if (levelScore >= 600) return DIAMOND;
        if (levelScore >= 300) return GOLD;
        if (levelScore >= 100) return SILVER;
        return BRONZE;
    }
}
