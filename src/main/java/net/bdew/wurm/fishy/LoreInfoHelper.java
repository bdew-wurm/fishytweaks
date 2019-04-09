package net.bdew.wurm.fishy;

import com.wurmonline.server.Point;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.behaviours.FishEnums;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.utils.BMLBuilder;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zones;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LoreInfoHelper {
    public static String fishPreferredWater(FishEnums.FishData data) {
        List<String> opts = new LinkedList<>();
        if (data.inLake()) opts.add("lake");
        if (data.inPond()) opts.add("pond");
        if (data.inSea()) opts.add("sea");
        if (data.inShallows()) opts.add("shallows");
        return String.format("Likes: %s  [%d - %d meter depth]", String.join(", ", opts), data.getMinDepth() / 10, data.getMaxDepth() / 10);
    }

    public static String fishPreferredTime(FishEnums.FishData data) {
        int[] times = data.feeds();
        int best = Arrays.stream(times).max().orElse(0);
        return "Likes: " + Arrays.stream(FishEnums.TimeOfDay.values())
                .filter(t -> times[t.getTypeId()] == best)
                .map(t -> t.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }

    public static String fishPreferredBait(FishEnums.FishData data) {
        int[] baits = data.baits();
        int best = Arrays.stream(baits).max().orElse(0);
        return "Likes: " + Arrays.stream(FishEnums.BaitType.values())
                .filter(t -> baits[t.getTypeId()] == best)
                .map(t -> {
                    try {
                        return ItemTemplateFactory.getInstance().getTemplate(t.getTemplateId()).getName();
                    } catch (NoSuchTemplateException e) {
                        return t.name().toLowerCase();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    public static String fishPreferredFloat(FishEnums.FishData data) {
        switch (data.getFeedHeight()) {
            case TOP:
                return "Likes: feather";
            case BOTTOM:
                return "Likes: moss";
            case ANY:
                return "Likes: twig";
            case TIME:
                switch (FishEnums.TimeOfDay.getTimeOfDay()) {
                    case MORNING:
                        return "Likes: bark or feather (time based)";
                    case EVENING:
                        return "Likes: bark or moss (time based)";
                    case AFTERNOON:
                    case NIGHT:
                        return "Likes: bark or twig (time based)";
                }
        }
        return "???";
    }

    public static void addRodModifiers(FishEnums.FishData data, Item rod, Item reel, Item line, Item hook, BMLBuilder table) {
        if (rod.getTemplateId() == ItemList.netFishing) {
            if (data.useFishingNet())
                LoreWindow.addModifier(table, "Fishing net:", 0, true, false, "");
            else
                LoreWindow.addModifier(table, "Fishing net:", 1000, true, false, "Can't catch with net!");
        } else if (rod.getTemplateId() == ItemList.spearLong || rod.getTemplateId() == ItemList.spearSteel) {
            if (data.useSpear())
                if (rod.getTemplateId() == ItemList.spearSteel)
                    LoreWindow.addModifier(table, "Spear:", 0, true, false, "");
                else
                    LoreWindow.addModifier(table, "Spear:", 2, true, false, "(penalty for wooden spear)");
            else
                LoreWindow.addModifier(table, "Spear:", 1000, true, false, "Can't catch with spear!");
        } else {
            if (line == null || hook == null) return; // shouldn't happen
            float diff = 1000.0f;
            if (rod.getTemplateId() == ItemList.fishingPole) {
                if (data.useFishingPole()) {
                    LoreWindow.addModifier(table, "Pole:", diff = -10, true, false, "Likes: pole");
                } else if (data.useReelBasic()) {
                    LoreWindow.addModifier(table, "Pole:", diff = 5, true, false, "Likes: basic rod");
                } else if (data.useReelFine()) {
                    LoreWindow.addModifier(table, "Pole:", diff = 10, true, false, "Likes: fine rod");
                } else if (data.useReelProfessional()) {
                    LoreWindow.addModifier(table, "Pole:", diff = 30, true, false, "Likes: professional rod");
                } else if (data.useReelWater()) {
                    LoreWindow.addModifier(table, "Pole:", diff = 30, true, false, "Likes: deep water rod");
                } else {
                    LoreWindow.addModifier(table, "Pole:", diff = 30, true, false, "Likes: ???");
                }
            } else {
                switch (reel.getTemplateId()) {
                    case ItemList.fishingReelLight: {
                        if (data.useReelBasic()) {
                            LoreWindow.addModifier(table, "Rod:", diff = -10, true, false, "Likes: basic rod");
                        } else if (data.useFishingPole()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 5, true, false, "Likes: fishing pole");
                        } else if (data.useReelFine()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 10, true, false, "Likes: fine rod");
                        } else if (data.useReelProfessional()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: professional rod");
                        } else if (data.useReelWater()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: deep water rod");
                        } else {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: ???");
                        }
                        break;
                    }
                    case ItemList.fishingReelMedium: {
                        if (data.useReelFine()) {
                            LoreWindow.addModifier(table, "Rod:", diff = -10, true, false, "Likes: fine rod");
                        } else if (data.useReelBasic()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 5, true, false, "Likes: basic rod");
                        } else if (data.useReelWater()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 10, true, false, "Likes: deep water rod");
                        } else if (data.useFishingPole()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: fishing pole");
                        } else if (data.useReelProfessional()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: professional rod");
                        } else {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: ???");
                        }
                        break;
                    }
                    case ItemList.fishingReelDeepWater: {
                        if (data.useReelWater()) {
                            LoreWindow.addModifier(table, "Rod:", diff = -10, true, false, "Likes: deep water rod");
                        } else if (data.useReelFine()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 5, true, false, "Likes: fine rod");
                        } else if (data.useReelProfessional()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 10, true, false, "Likes: professional rod");
                        } else if (data.useReelBasic()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 15, true, false, "Likes: basic rod");
                        } else if (data.useFishingPole()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: fishing pole");
                        } else {
                            LoreWindow.addModifier(table, "Rod:", diff = 30, true, false, "Likes: ???");
                        }
                        break;
                    }
                    case ItemList.fishingReelProfessional: {
                        if (data.useReelProfessional()) {
                            LoreWindow.addModifier(table, "Rod:", diff = -10, true, false, "Likes: professional rod");
                        } else if (data.useReelWater()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 5, true, false, "Likes: deep water rod");
                        } else if (data.useReelFine()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 15, true, false, "Likes: fine rod");
                        } else if (data.useReelBasic()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 50, true, false, "Likes: basic rod");
                        } else if (data.useFishingPole()) {
                            LoreWindow.addModifier(table, "Rod:", diff = 50, true, false, "Likes: fishing pole");
                        } else {
                            LoreWindow.addModifier(table, "Rod:", diff = 50, true, false, "Likes: ???");
                        }
                        break;
                    }
                }
            }
            if (diff > 0.0f) {
                switch (hook.getTemplateId()) {
                    case ItemList.hookMetal:
                        LoreWindow.addModifier(table, "Hook:", 0, true, false, "");
                        break;
                    case ItemList.hookBone:
                        LoreWindow.addModifier(table, "Hook:", 0.1f * diff, true, false, "10% penalty (use metal instead)");
                        break;
                    case ItemList.hookWood:
                        LoreWindow.addModifier(table, "Hook:", 0.2f * diff, true, false, "20% penalty (use metal instead)");
                        break;
                }
            } else {
                LoreWindow.addModifier(table, "Hook:", 0, true, false, "");
            }
        }
    }

    public static float specialFishChanceMod(FishEnums.FishData data, int x, int y, boolean surface) {
        int season = WurmCalendar.getSeasonNumber();
        Point specialSpot = data.getSpecialSpot(x / 128, y / 128, season);
        int dx = Math.abs(specialSpot.getX() - x);
        int dy = Math.abs(specialSpot.getY() - y);
        int dist = Math.max(dx, dy);
        if (dist <= 15) {
            try {
                float ht = Zones.calculateHeight(x * 4 + 2, y * 4 + 2, surface) * 10.0F;
                if (ht < -100)
                    return (float) Math.cos(Math.toRadians((double) ((float) dist * 6.0F)));
            } catch (NoSuchZoneException ignored) {
            }
        }
        return 0;
    }
}
