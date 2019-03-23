package net.bdew.wurm.fishy;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.SkillList;

public class Hooks {
    public static void doNotifySpawn(Creature performer, Creature fish, boolean spear) {
        if (performer.isPlayer() && performer.hasLink() && fish != null) {
            performer.getCommunicator().sendSafeServerMessage(
                    String.format("%s %s is %s!",
                            "aeiouAEIOU".contains(fish.getName().substring(0, 1)) ? "An" : "A",
                            fish.getName(),
                            spear ? "swimming by" : "biting"
                    ), (byte) 1);
        }
    }

    public static void checkSkillTick(Creature performer, Item source, Action act) {
        if (!act.justTickedSecond()) return;

        int tpl = source.getTemplateId();

        int period = Config.skillTickPeriodRod;
        if (tpl == ItemList.spearLong || tpl == ItemList.spearSteel)
            period = Config.skillTickPeriodSpear;
        else if (tpl == ItemList.netFishing)
            period = Config.skillTickPeriodNet;

        if (period > 0 && act.getSecond() % period == 0) {
            performer.getSkills().getSkillOrLearn(SkillList.FISHING).skillCheck(0, source, 0f, false, period);
            if (Config.debugLogging)
                FishyMod.logInfo(String.format("Fishing for %s - doing skill tick at %d/%d seconds", performer.getName(), act.getSecond(), period));
        }
    }

    public static float spearStrikeBonus(Creature performer, Item source, float distance) {
        distance = (float) Math.sqrt(distance);
        float bonus = 0;
        if (Config.spearBonusDistanceScale > 0 && Config.spearBonusMaxDistance > 0 && distance < Config.spearBonusMaxDistance) {
            bonus += (Config.spearBonusMaxDistance - distance) * Config.spearBonusDistanceScale;
            if (Config.debugLogging)
                FishyMod.logInfo(String.format("Fishing for %s - adding %.1f bonus for distance %.1f", performer.getName(), bonus, distance));
        }
        if (Config.spearBonusNimScale > 0) {
            float nim = source.getSpellNimbleness();
            if (nim > 0) {
                bonus += Config.spearBonusNimScale * source.getSpellNimbleness();
                if (Config.debugLogging)
                    FishyMod.logInfo(String.format("Fishing for %s - adding %.1f bonus for nimbleness %.1f", performer.getName(), Config.spearBonusNimScale * source.getSpellNimbleness(), nim));
            }
        }
        return bonus;
    }
}
