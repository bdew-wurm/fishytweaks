package net.bdew.wurm.fishy;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FishyMod implements WurmServerMod, Configurable, PreInitable, Initable {
    private static final Logger logger = Logger.getLogger("FishyMod");

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    @Override
    public void configure(Properties properties) {
        Config.load(properties);
    }


    @Override
    public void preInit() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            CtClass ctFishing = classPool.get("com.wurmonline.server.behaviours.MethodsFishing");

            if (Config.onScreenNotify) {
                ctFishing.getMethod("makeFish", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/skills/Skill;B)Z")
                        .instrument(new ExprEditor() {
                            @Override
                            public void edit(MethodCall m) throws CannotCompileException {
                                if (m.getMethodName().equals("makeFishCreature")) {
                                    m.replace("$_ = $proceed($$); net.bdew.wurm.fishy.Hooks.doNotifySpawn($1, $5, $_);");
                                    logInfo(String.format("Injecting doNotifySpawn into makeFish() at %d", m.getLineNumber()));
                                }
                            }
                        });
            }

            if (Config.skillTickPeriodRod > 0 || Config.skillTickPeriodNet > 0 || Config.skillTickPeriodSpear > 0) {
                ctFishing.getMethod("fish", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFLcom/wurmonline/server/behaviours/Action;)Z")
                        .insertAfter("net.bdew.wurm.fishy.Hooks.checkSkillTick($1, $2, $7);");
                logInfo("Added skill tick check to fish()");
            }

            boolean strikeBonus = Config.spearBonusNimScale > 0 || (Config.spearBonusDistanceScale > 0 && Config.spearBonusMaxDistance > 0);

            if (Config.disableSpearHardMiss || strikeBonus) {
                ctFishing.getMethod("processSpearStrike", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/skills/Skill;Lcom/wurmonline/server/items/Item;FF)Z")
                        .instrument(new ExprEditor() {
                            @Override
                            public void edit(MethodCall m) throws CannotCompileException {
                                if (Config.disableSpearHardMiss && m.getMethodName().equals("sqrt")) {
                                    m.replace("$_ = $proceed($$); if ($_>1.0) $_=1.0;");
                                    logInfo(String.format("Disabling hard miss in processSpearStrike() at %d", m.getLineNumber()));
                                } else if (strikeBonus && m.getMethodName().equals("getDifficulty")) {
                                    m.replace("$_ = $proceed($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, net.bdew.wurm.fishy.Hooks.spearStrikeBonus($1,$6,dx * dx + dy * dy), $13);");
                                }

                            }
                        });
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
    }
}
