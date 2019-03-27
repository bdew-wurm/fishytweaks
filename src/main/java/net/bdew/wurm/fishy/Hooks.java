package net.bdew.wurm.fishy;

import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.Set;

public class Hooks {
    private static Field fItemsWatched;

    static void init() throws NoSuchFieldException {
        fItemsWatched = ReflectionUtil.getField(Player.class, "itemsWatched");
    }

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
                FishyMod.logInfo(String.format("Fishing for %s - doing skill tick at %d/%d seconds - state %d", performer.getName(), act.getSecond(), period, act.getTickCount()));
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

    public static float speedModOverride(byte startCmd, float current) {
        if (startCmd == 20)
            return current * Config.fishSpeedModSpear;
        else if (startCmd == -10)
            return current * Config.fishSpeedModRodPull;
        else
            return current * Config.fishSpeedModRod;
    }

    public static long modifyTiming(String method, int callNr, Creature performer, Item source, Action action, long newVal) {
        Config.Timing override = null;

        if (method.equals("processFishCasted") || method.equals("processFishSwamAway") || (method.equals("makeFish") && callNr == 0)) {
            override = Config.rodFishSpawnTime;
        } else if (method.equals("processSpearSwamAway") || (method.equals("fish") && action.getTickCount() == 21)) {
            override = Config.spearFishSpawnTime;
        } else if (method.equals("processNetPhase") || (method.equals("fish") && action.getTickCount() == 40)) {
            override = Config.netFishCheckInterval;
        }

        if (override != null) {
            int ticks = (int) (10f * override.calculate(source.getCurrentQualityLevel(), source.getSpellSpeedBonus()));
            if (Config.debugLogging)
                FishyMod.logInfo(String.format("Overriding timer for %s at %s(%d) from %.1f to %.1f sec (state=%d)",
                        performer.getName(), method, callNr, 0.1f * newVal - action.currentSecond(), 0.1f * ticks, action.getTickCount()));
            newVal = (long) (action.getCounterAsFloat() * 10 + ticks);
        } else {
            if (Config.debugLogging)
                FishyMod.logInfo(String.format("Not overriding timer for %s at %s(%d) - %.1f sec (state=%d)",
                        performer.getName(), method, callNr, 0.1f * newVal - action.currentSecond(), action.getTickCount()));
        }

        return newVal;
    }

    private static boolean tryToPutFishInOpenKeepNet(Creature performer, Item fish) {
        if (performer.isPlayer()) {
            try {
                Set<Item> watched = ReflectionUtil.getPrivateField(performer, fItemsWatched);
                if (watched != null) {
                    return watched.stream().anyMatch(net -> tryToPutFishInKeepNet(performer, net, fish, false));
                }
            } catch (IllegalAccessException e) {
                FishyMod.logException(String.format("Error accessing watched items for %s", performer.getName()), e);
            }
        }
        return false;
    }

    private static boolean tryToPutFishInKeepNet(Creature performer, Item net, Item fish, boolean attached) {
        if (net.getTemplateId() == ItemList.netKeep
                && net.mayCreatureInsertItem()
                && net.insertItem(fish)
                && (attached || (net.isPlanted() && net.getParentId() == -10L))) {
            fish.setLastOwnerId(performer.getWurmId());
            if (!attached)
                performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the nearby %s.", fish.getName(), net.getName()));
            return true;
        } else return false;
    }

    private static boolean tryToPutFishInVehicle(Creature performer, Item vehicle, Item fish) {
        // Check attached keep net
        if (vehicle.getExtra() != -10L) {
            if (Items.getItemOptional(vehicle.getExtra()).filter(net -> tryToPutFishInKeepNet(performer, net, fish, true)).isPresent()) {
                performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the net attached to your %s.", fish.getName(), vehicle.getName()));
                return true;
            }
        }

        // If no net - try putting in crate/fsb
        if (fish.getTemplate().isBulk() && fish.getRarity() == 0) {
            for (Item container : vehicle.getAllItems(true)) {
                if (container.isCrate() && container.canAddToCrate(fish) && container.mayAccessHold(performer)) {
                    if (fish.AddBulkItemToCrate(performer, container)) {
                        performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the %s in your %s.", fish.getName(), container.getName(), vehicle.getName()));
                        return true;
                    }
                } else if (container.getTemplateId() == ItemList.hopper && container.hasSpaceFor(fish.getVolume()) && container.mayAccessHold(performer)) {
                    if (fish.AddBulkItem(performer, container)) {
                        performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the %s in your %s.", fish.getName(), container.getName(), vehicle.getName()));
                        return true;
                    }
                }
            }
        }

        // If all above failed try putting it in the hold
        if (vehicle.mayCreatureInsertItem() && vehicle.insertItem(fish)) {
            performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the hold of %s.", fish.getName(), vehicle.getName()));
            return true;
        }

        return false;
    }


    public static boolean catchFishHook(Creature performer, Item target, Item fish) {
        // First try to put in open planted keep net
        if (tryToPutFishInOpenKeepNet(performer, fish)) return true;

        // If that failed see if player is in boat and try to put it into something there
        if (performer.getVehicle() != -10 &&
                Items.getItemOptional(performer.getVehicle())
                        .filter(boat -> boat.isHollow() && boat.mayAccessHold(performer) && tryToPutFishInVehicle(performer, boat, fish))
                        .isPresent()) return true;

        // And finally try player inventory, if that fails default to the original target
        return performer.getInventory().insertItem(fish) || target.insertItem(fish);
    }
}
