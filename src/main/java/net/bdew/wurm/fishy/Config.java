package net.bdew.wurm.fishy;

import java.util.Properties;

public class Config {
    public static boolean debugLogging;
    public static boolean onScreenNotify;
    public static int skillTickPeriodRod, skillTickPeriodSpear, skillTickPeriodNet;
    public static boolean disableSpearHardMiss;
    public static float spearBonusMaxDistance, spearBonusDistanceScale, spearBonusNimScale;

    static void load(Properties props) {
        debugLogging = Boolean.parseBoolean(props.getProperty("debugLogging", "false"));

        onScreenNotify = Boolean.parseBoolean(props.getProperty("onScreenNotify", "false"));

        skillTickPeriodRod = Integer.parseInt(props.getProperty("skillTickPeriodRod", "-1"));
        skillTickPeriodSpear = Integer.parseInt(props.getProperty("skillTickPeriodSpear", "-1"));
        skillTickPeriodNet = Integer.parseInt(props.getProperty("skillTickPeriodNet", "-1"));

        disableSpearHardMiss = Boolean.parseBoolean(props.getProperty("disableSpearHardMiss", "false"));

        spearBonusMaxDistance = Float.parseFloat(props.getProperty("spearBonusMaxDistance", "0"));
        spearBonusDistanceScale = Float.parseFloat(props.getProperty("spearBonusDistanceScale", "0"));
        spearBonusNimScale = Float.parseFloat(props.getProperty("spearBonusNimScale", "0"));
    }
}
