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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FishLoreWindow implements ModQuestion {
    private final Creature performer;
    private final Item source;
    private final MethodsFishing.FishRow[] table;
    private final int x, y;
    private final SortMode sorting;

    public enum SortMode {
        CHANCE_ASC(Comparator.comparing(MethodsFishing.FishRow::getChance)),
        CHANCE_DESC(Comparator.comparing(MethodsFishing.FishRow::getChance).reversed()),
        NAME_ASC(Comparator.comparing(MethodsFishing.FishRow::getName)),
        NAME_DESC(Comparator.comparing(MethodsFishing.FishRow::getName).reversed()),
        DIFF_ASC(Comparator.comparing(r -> FishEnums.FishData.fromInt(r.getFishTypeId()).getTemplateDifficulty())),
        DIFF_DESC(Comparator.comparing(r -> -FishEnums.FishData.fromInt(r.getFishTypeId()).getTemplateDifficulty()));

        public final Comparator<MethodsFishing.FishRow> comparator;

        SortMode(Comparator<MethodsFishing.FishRow> comparator) {
            this.comparator = comparator;
        }
    }

    public FishLoreWindow(Creature performer, Item source, MethodsFishing.FishRow[] table, int x, int y, SortMode sorting) {
        this.performer = performer;
        this.source = source;
        this.table = table;
        this.x = x;
        this.y = y;
        this.sorting = sorting;
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

        int depth;
        try {
            depth = ReflectionUtil.callPrivateMethod(null,
                    ReflectionUtil.getMethod(FishEnums.class, "getWaterDepth", new Class[]{Float.TYPE, Float.TYPE, Boolean.TYPE})
                    , x * 4 + 2, y * 4 + 2, performer.isOnSurface());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            FishyMod.logException("Error getting depth", e);
            return;
        }

        double skill = performer.getSkills().getSkillOrLearn(SkillList.FISHING).getKnowledge();

        addHorizontal(content, bml -> {
            addItem(bml, "Tool:", source);
            addValue(bml, "Water:", String.format("%s (%dm)", WaterType.getWaterTypeString(x, y, performer.isOnSurface()), depth / 10));
            addValue(bml, "Skill:", String.format("%.2f", skill));
            addValue(bml, "Time:", FishEnums.TimeOfDay.getTimeOfDay().name());
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

        List<MethodsFishing.FishRow> data = Arrays.stream(table)
                .filter(r -> r.getChance() > 0)
                .sorted(sorting.comparator)
                .collect(Collectors.toList());

        if (data.size() == 0) {
            content.addText("You can't catch anything here!", null, BMLBuilder.TextType.BOLD, Color.RED);
        } else {
            BMLBuilder rows = BMLBuilder.createTable(3);

            addSortingHeader(rows, "Fish", SortMode.NAME_ASC, SortMode.NAME_DESC, 300);
            addSortingHeader(rows, "Chance", SortMode.CHANCE_ASC, SortMode.CHANCE_DESC, 100);
            addSortingHeader(rows, "Difficulty", SortMode.DIFF_ASC, SortMode.DIFF_DESC, 100);

            for (MethodsFishing.FishRow row : data) {
                float diff = FishEnums.FishData.fromInt(row.getFishTypeId()).getTemplateDifficulty();
                Color diffCol = Color.WHITE;
                if (diff >= skill + 15) diffCol = Color.ORANGE;
                if (diff >= skill + 35) diffCol = Color.RED;
                if (diff <= skill - 15) diffCol = Color.GREEN;

                rows.addText(row.getName());
                rows.addText(String.format("%.1f%%", row.getChance()));
                rows.addText(String.format("%.1f", diff), null, null, diffCol);
            }

            content.addString(rows.toString());
        }

        BMLBuilder bml = BMLBuilder.createNoQuestionWindow(String.valueOf(question.getId()), content);
        performer.getCommunicator().sendBml(500, 300, true, true, bml.toString(), 200, 200, 200, question.getTitle());
    }

    @Override
    public void answer(Question question, Properties answers) {
        for (SortMode sortMode : SortMode.values()) {
            if (answers.containsKey("S_" + sortMode.toString())) {
                send(performer, source, table, x, y, sortMode);
                return;
            }
        }
    }

    public static void send(Creature performer, Item source, MethodsFishing.FishRow[] table, int x, int y, SortMode sorting) {
        ModQuestions.createQuestion(performer, "Fish Lore", "", -10, new FishLoreWindow(performer, source, table, x, y, sorting)).sendQuestion();
    }
}
