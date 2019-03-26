package net.bdew.wurm.fishy;

import com.wurmonline.server.Server;

import java.util.Properties;

public class Config {
    public static boolean debugLogging;
    public static boolean onScreenNotify, autoStoreFish;
    public static int skillTickPeriodRod, skillTickPeriodSpear, skillTickPeriodNet;
    public static boolean disableSpearHardMiss;
    public static float spearBonusMaxDistance, spearBonusDistanceScale, spearBonusNimScale;
    public static float fishSpeedModRod, fishSpeedModRodPull, fishSpeedModSpear;
    public static Timing rodFishSpawnTime, spearFishSpawnTime, netFishCheckInterval;

    public static class Timing {
        public final float min, max, enchantScale, qlScale;

        public Timing(float min, float max, float enchantScale, float qlScale) {
            this.min = min;
            this.max = max;
            this.enchantScale = enchantScale;
            this.qlScale = qlScale;
        }

        public float calculate(float ql, float enchant) {
            return (min + Server.rand.nextFloat() * (max - min)) * (1f - ql * qlScale) * (1f - enchant * enchantScale);
        }

        public static Timing from(String s) {
            String[] split = s.split(",");
            if (split.length == 1) {
                float f = Float.parseFloat(split[0]);
                return new Timing(f, f, 0, 0);
            } else if (split.length == 2) {
                return new Timing(Float.parseFloat(split[0]), Float.parseFloat(split[1]), 0, 0);
            } else if (split.length == 3) {
                return new Timing(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), 0);
            } else if (split.length == 4) {
                return new Timing(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]));
            } else {
                throw new RuntimeException(String.format("Invalid timing specification: '%s'", s));
            }
        }
    }

    static void load(Properties props) {
        debugLogging = Boolean.parseBoolean(props.getProperty("debugLogging", "false"));

        onScreenNotify = Boolean.parseBoolean(props.getProperty("onScreenNotify", "false"));
        autoStoreFish = Boolean.parseBoolean(props.getProperty("autoStoreFish", "false"));

        skillTickPeriodRod = Integer.parseInt(props.getProperty("skillTickPeriodRod", "-1"));
        skillTickPeriodSpear = Integer.parseInt(props.getProperty("skillTickPeriodSpear", "-1"));
        skillTickPeriodNet = Integer.parseInt(props.getProperty("skillTickPeriodNet", "-1"));

        disableSpearHardMiss = Boolean.parseBoolean(props.getProperty("disableSpearHardMiss", "false"));

        spearBonusMaxDistance = Float.parseFloat(props.getProperty("spearBonusMaxDistance", "0"));
        spearBonusDistanceScale = Float.parseFloat(props.getProperty("spearBonusDistanceScale", "0"));
        spearBonusNimScale = Float.parseFloat(props.getProperty("spearBonusNimScale", "0"));

        fishSpeedModRod = Float.parseFloat(props.getProperty("fishSpeedModRod", "1"));
        fishSpeedModRodPull = Float.parseFloat(props.getProperty("fishSpeedModRodPull", "1"));
        fishSpeedModSpear = Float.parseFloat(props.getProperty("fishSpeedModSpear", "1"));

        if (props.containsKey("rodFishSpawnTime"))
            rodFishSpawnTime = Timing.from(props.getProperty("rodFishSpawnTime"));
        if (props.containsKey("spearFishSpawnTime"))
            spearFishSpawnTime = Timing.from(props.getProperty("spearFishSpawnTime"));
        if (props.containsKey("netFishCheckInterval"))
            netFishCheckInterval = Timing.from(props.getProperty("netFishCheckInterval"));
    }
}
