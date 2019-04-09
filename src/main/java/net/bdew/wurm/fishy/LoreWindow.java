package net.bdew.wurm.fishy;

import com.wurmonline.server.behaviours.FishEnums;
import com.wurmonline.server.behaviours.MethodsFishing;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.utils.BMLBuilder;
import com.wurmonline.server.zones.WaterType;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LoreWindow implements ModQuestion {
    private final Creature performer;
    private final Item source;
    private final List<Row> fishTable;
    private final int x, y;
    private final SortMode sorting;
    private final Optional<Byte> fishType;

    public enum SortMode {
        CHANCE_ASC(Comparator.comparing(Row::getChance)),
        CHANCE_DESC(Comparator.comparing(Row::getChance).reversed()),
        NAME_ASC(Comparator.comparing(Row::getName)),
        NAME_DESC(Comparator.comparing(Row::getName).reversed()),
        DIFF_ASC(Comparator.comparing(Row::getDifficulty)),
        DIFF_DESC(Comparator.comparing(Row::getDifficulty).reversed());

        public final Comparator<Row> comparator;

        SortMode(Comparator<Row> comparator) {
            this.comparator = comparator;
        }
    }

    public static class Row {
        public final byte id;
        public final String name;
        public final float chance, difficulty;

        public Row(byte id, String name, float chance, float difficulty) {
            this.id = id;
            this.name = name;
            this.chance = chance;
            this.difficulty = difficulty;
        }

        public byte getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public float getChance() {
            return chance;
        }

        public float getDifficulty() {
            return difficulty;
        }
    }

    private static Method getWaterDepth, addDifficultyDepth, addDifficultyFeeding, addDifficultyTimeOfDay, addDifficultyBait,
            getChanceDefault, multChanceDepth, multChanceFeeding, multChanceTimeOfDay, multChanceRod;

    static {
        try {
            getWaterDepth = ReflectionUtil.getMethod(FishEnums.class, "getWaterDepth");
            addDifficultyDepth = ReflectionUtil.getMethod(FishEnums.FishData.class, "addDifficultyDepth");
            addDifficultyFeeding = ReflectionUtil.getMethod(FishEnums.FishData.class, "addDifficultyFeeding");
            addDifficultyTimeOfDay = ReflectionUtil.getMethod(FishEnums.FishData.class, "addDifficultyTimeOfDay");
            addDifficultyBait = ReflectionUtil.getMethod(FishEnums.FishData.class, "addDifficultyBait");
            getChanceDefault = ReflectionUtil.getMethod(FishEnums.FishData.class, "getChanceDefault");
            multChanceDepth = ReflectionUtil.getMethod(FishEnums.FishData.class, "multChanceDepth");
            multChanceFeeding = ReflectionUtil.getMethod(FishEnums.FishData.class, "multChanceFeeding");
            multChanceTimeOfDay = ReflectionUtil.getMethod(FishEnums.FishData.class, "multChanceTimeOfDay");
            multChanceRod = ReflectionUtil.getMethod(FishEnums.FishData.class, "multChanceRod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public LoreWindow(Creature performer, Item source, List<Row> fishTable, int x, int y, SortMode sorting, Optional<Byte> fishType) {
        this.performer = performer;
        this.source = source;
        this.fishTable = fishTable;
        this.x = x;
        this.y = y;
        this.sorting = sorting;
        this.fishType = fishType;
    }

    private void addHorizontal(BMLBuilder target, Consumer<BMLBuilder> build) {
        BMLBuilder content = BMLBuilder.createGenericBuilder();
        build.accept(content);
        target.addString(BMLBuilder.createHorizArrayNode(false).addString(content.toString()).toString());
    }

    private void addItem(BMLBuilder target, String header, Item item) {
        target.addLabel(header, null, BMLBuilder.TextType.BOLD, null);
        if (item == null)
            target.addLabel("None", null, null, Color.red);
        else
            target.addLabel(item.getName());
    }

    private void addValue(BMLBuilder target, String header, String value) {
        target.addLabel(header, null, BMLBuilder.TextType.BOLD, null);
        target.addLabel(value);
    }

    private void addSortingHeader(BMLBuilder bml, String name, SortMode asc, SortMode desc, int width) {
        if (sorting == asc) {
            bml.addButton("S_" + desc.toString(), "[^] " + name, width, 20, true);
        } else if (sorting == desc) {
            bml.addButton("S_" + asc.toString(), "[v] " + name, width, 20, true);
        } else {
            bml.addButton("S_" + asc.toString(), name, width, 20, true);
        }
    }

    @Override
    public void sendQuestion(Question question) {
        BMLBuilder content = BMLBuilder.createGenericBuilder();
        addGeneralInfo(content);
        if (fishType.isPresent()) {
            addDetails(content, fishType.get());
        } else {
            addFishTable(content);
        }
        BMLBuilder bml = BMLBuilder.createNoQuestionWindow(String.valueOf(question.getId()), content);
        performer.getCommunicator().sendBml(500, fishType.isPresent() ? 500 : 300, true, true, bml.toString(), 200, 200, 200, question.getTitle());
    }

    private void addGeneralInfo(BMLBuilder content) {
        int depth;
        try {
            depth = ReflectionUtil.callPrivateMethod(null, getWaterDepth, x * 4 + 2, y * 4 + 2, performer.isOnSurface());
        } catch (IllegalAccessException | InvocationTargetException e) {
            FishyMod.logException("Error getting depth", e);
            content.addText("Something went wrong, try again later or open a /support ticket.", null, BMLBuilder.TextType.BOLD, Color.RED);
            return;
        }

        addHorizontal(content, bml -> {
            addItem(bml, "Tool:", source);
            addValue(bml, "Water:", String.format("%s (%dm)", WaterType.getWaterTypeString(x, y, performer.isOnSurface()), depth / 10));
            addValue(bml, "Skill:", String.format("%.2f", performer.getSkills().getSkillOrLearn(SkillList.FISHING).getKnowledge()));
            addValue(bml, "Time:", FishEnums.TimeOfDay.getTimeOfDay().name().toLowerCase());
        });

        if (source.getTemplateId() == ItemList.fishingRod || source.getTemplateId() == ItemList.fishingPole) {
            Item[] items = source.getFishingItems();
            addHorizontal(content, bml -> {
                addItem(bml, "Reel:", items[0]);
                addItem(bml, "Line:", items[1]);
                addItem(bml, "Float:", items[2]);
            });
            addHorizontal(content, bml -> {
                addItem(bml, "Hook:", items[3]);
                addItem(bml, "Bait:", items[4]);
            });
        }
    }

    public static void addModifier(BMLBuilder table, String header, float value, boolean negativeIsGood, boolean percent, String prefers) {
        if (percent) value = (value - 1f) * 100f;
        table.addLabel(header, null, BMLBuilder.TextType.BOLD, null);
        Color col = Color.WHITE;
        if ((value > 0 && !negativeIsGood) || (value < 0 && negativeIsGood)) col = Color.GREEN;
        else if ((value > 0 && negativeIsGood) || (value < 0 && !negativeIsGood)) col = Color.RED;
        table.addLabel(String.format("%s%.1f%s", value > 0 ? "+" : "", value, percent ? "%" : ""), null, null, col);
        table.addLabel(prefers);
    }

    private void addDetails(BMLBuilder content, byte fishType) {
        FishEnums.FishData data = FishEnums.FishData.fromInt(fishType);
        double skill = performer.getSkills().getSkillOrLearn(SkillList.FISHING).getKnowledge();
        Item[] items = source.getFishingItems(); // 0 - Reel | 1 - line | 2 - float | 3 - hook | 4 - bait

        content.addText("");
        content.addHeader(String.format("Details for %s", data.getName()));
        content.addText("");
        content.addText("Difficulty:", null, BMLBuilder.TextType.BOLD, null);
        content.addText("");
        if (data == FishEnums.FishData.CLAM) {
            content.addText("Clams difficulty is hardcoded to skill - 10.");
        } else {
            BMLBuilder table = BMLBuilder.createTable(3);

            table.addLabel("Base:", null, BMLBuilder.TextType.BOLD, null);
            table.addLabel(String.format("%.1f", data.getTemplateDifficulty()));
            table.addLabel("");

            try {
                addModifier(table, "Depth:", ReflectionUtil.callPrivateMethod(data, addDifficultyDepth, x * 4 + 2, y * 4 + 2, performer.isOnSurface()), true, false, LoreInfoHelper.fishPreferredWater(data));
                addModifier(table, "Time:", ReflectionUtil.callPrivateMethod(data, addDifficultyTimeOfDay), true, false, LoreInfoHelper.fishPreferredTime(data));
                addModifier(table, "Float:", ReflectionUtil.callPrivateMethod(data, addDifficultyFeeding, items[2]), true, false, LoreInfoHelper.fishPreferredFloat(data));
                addModifier(table, "Bait:", ReflectionUtil.callPrivateMethod(data, addDifficultyBait, items[4]), true, false, LoreInfoHelper.fishPreferredBait(data));

                LoreInfoHelper.addRodModifiers(data, source, items[0], items[1], items[3], table);

                table.addLabel("Total:", null, BMLBuilder.TextType.BOLD, null);
                table.addLabel(String.format("%.1f", data.getDifficulty((float) skill, x * 4 + 2, y * 4 + 2, performer.isOnSurface(), source, items[0], items[1], items[2], items[3], items[4])));
                table.addLabel("");
            } catch (IllegalAccessException | InvocationTargetException e) {
                FishyMod.logException("Error in fish details", e);
                content.addText("Something went wrong, try again later or open a /support ticket.", null, BMLBuilder.TextType.BOLD, Color.RED);
                return;
            }

            content.addString(table.toString());
        }

        content.addText("");
        content.addText("Chance:", null, BMLBuilder.TextType.BOLD, null);
        content.addText("");

        if (data == FishEnums.FishData.CLAM) {
            content.addText("Clams are special and the chance to catch them will be 1% most of the time");
            content.addText(" (depending on other available fish).");
        } else {
            try {
                BMLBuilder table = BMLBuilder.createTable(3);

                table.addLabel("Base:", null, BMLBuilder.TextType.BOLD, null);
                table.addLabel(String.format("%.1f", ReflectionUtil.<Float>callPrivateMethod(data, getChanceDefault, (float) skill)));
                table.addLabel("(From difficulty and skill level)");

                addModifier(table, "Depth:", ReflectionUtil.callPrivateMethod(data, multChanceDepth, x * 4f + 2f, y * 4f + 2f, performer.isOnSurface()), false, true, "");
                addModifier(table, "Float:", ReflectionUtil.callPrivateMethod(data, multChanceFeeding, items[2]), false, true, "");
                addModifier(table, "Time:", ReflectionUtil.callPrivateMethod(data, multChanceTimeOfDay), false, true, "");
                addModifier(table, "Rod:", ReflectionUtil.callPrivateMethod(data, multChanceRod, source, items[0], items[1], items[3], items[4]), false, true, "");
                if (data.isSpecialFish()) {
                    addModifier(table, "Distance:", LoreInfoHelper.specialFishChanceMod(data, x, y, performer.isOnSurface()), false, true, "(from special spot)");
                }

                content.addString(table.toString());
                content.addText("Note: final chance is normalized along with other fish chances.");
            } catch (IllegalAccessException | InvocationTargetException e) {
                FishyMod.logException("Error in fish details", e);
                content.addText("Something went wrong, try again later or open a /support ticket.", null, BMLBuilder.TextType.BOLD, Color.RED);
                return;
            }
        }

        content.addText("");

        addHorizontal(content, bml -> {
            bml.addButton("BACK", "<< Back", 100, 20, true);
            bml.addLabel("");
        });
    }

    private void addFishTable(BMLBuilder content) {
        List<Row> sorted = fishTable.stream()
                .filter(r -> r.chance > 0)
                .sorted(sorting.comparator)
                .collect(Collectors.toList());

        if (sorted.size() == 0) {
            content.addText("You can't catch anything here!", null, BMLBuilder.TextType.BOLD, Color.RED);
        } else {
            double skill = performer.getSkills().getSkillOrLearn(SkillList.FISHING).getKnowledge();
            boolean showDetails = Config.fishLoreSkillDetails >= 0 && skill >= Config.fishLoreSkillDetails;

            BMLBuilder table = BMLBuilder.createTable(showDetails ? 4 : 3);

            addSortingHeader(table, "Fish", SortMode.NAME_ASC, SortMode.NAME_DESC, showDetails ? 230 : 280);
            addSortingHeader(table, "Chance", SortMode.CHANCE_ASC, SortMode.CHANCE_DESC, 100);
            addSortingHeader(table, "Difficulty", SortMode.DIFF_ASC, SortMode.DIFF_DESC, 100);
            if (showDetails) table.addLabel("");

            for (Row row : sorted) {
                Color diffCol = Color.WHITE;
                if (row.difficulty >= skill + 15) diffCol = Color.ORANGE;
                if (row.difficulty >= skill + 35) diffCol = Color.RED;
                if (row.difficulty <= skill - 15) diffCol = Color.GREEN;

                table.addLabel(row.getName());
                table.addLabel(String.format("%.1f%%", row.getChance()));
                table.addLabel(String.format("%.1f", row.difficulty), null, null, diffCol);
                if (showDetails)
                    table.addString(
                            BMLBuilder.createHorizArrayNode(false)
                                    .addButton("D_" + row.id, "Details")
                                    .addText(" ")
                                    .toString()
                    );
            }

            content.addString(table.toString());
        }
    }

    @Override
    public void answer(Question question, Properties answers) {
        if (answers.containsKey("BACK")) {
            send(performer, source, fishTable, x, y, sorting);
        } else {
            for (String k : answers.stringPropertyNames()) {
                if (k.startsWith("D_")) {
                    try {
                        byte val = Byte.parseByte(k.substring(2), 10);
                        if (fishTable.stream().anyMatch(r -> r.id == val))
                            send(performer, source, fishTable, x, y, sorting, val);
                        else
                            performer.getCommunicator().sendAlertServerMessage("Invalid selection");
                    } catch (NumberFormatException e) {
                        performer.getCommunicator().sendAlertServerMessage("Invalid selection");
                    }
                    return;
                }
            }
        }
        for (SortMode sortMode : SortMode.values()) {
            if (answers.containsKey("S_" + sortMode.toString())) {
                send(performer, source, fishTable, x, y, sortMode);
                return;
            }
        }
    }

    private static List<Row> prepare(Creature performer, Item source, MethodsFishing.FishRow[] table, int x, int y) {
        double skill = performer.getSkills().getSkillOrLearn(SkillList.FISHING).getKnowledge();
        Item[] items = source.getFishingItems();
        return Arrays.stream(table).map(row -> {
            FishEnums.FishData data = FishEnums.FishData.fromInt(row.getFishTypeId());
            return new Row(row.getFishTypeId(), row.getName(), row.getChance(),
                    data.getDifficulty((float) skill, x * 4 + 2, y * 4 + 2, performer.isOnSurface(), source, items[0], items[1], items[2], items[3], items[4]));
        }).collect(Collectors.toList());
    }

    public static void send(Creature performer, Item source, List<Row> table, int x, int y, SortMode sorting) {
        ModQuestions.createQuestion(performer, "Fish Lore", "", -10, new LoreWindow(performer, source, table, x, y, sorting, Optional.empty())).sendQuestion();
    }

    public static void send(Creature performer, Item source, List<Row> table, int x, int y, SortMode sorting, byte fishType) {
        ModQuestions.createQuestion(performer, "Fish Lore", "", -10, new LoreWindow(performer, source, table, x, y, sorting, Optional.of(fishType))).sendQuestion();
    }

    public static void send(Creature performer, Item source, MethodsFishing.FishRow[] table, int x, int y, SortMode sorting) {
        send(performer, source, prepare(performer, source, table, x, y), x, y, sorting);
    }

    public static void send(Creature performer, Item source, MethodsFishing.FishRow[] table, int x, int y, SortMode sorting, byte fishType) {
        send(performer, source, prepare(performer, source, table, x, y), x, y, sorting, fishType);
    }

}
