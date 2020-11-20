package capsule.loot;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.Files;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.StandaloneLootEntry;
import net.minecraft.world.storage.loot.conditions.ILootCondition;
import net.minecraft.world.storage.loot.functions.ILootFunction;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Consumer;

/**
 * @author Lythom
 */
public class CapsuleLootEntry extends StandaloneLootEntry {

    public static final int DEFAULT_WEIGHT = 3;
    public static String[] COLOR_PALETTE = new String[]{
            "0xCCCCCC", "0x549b57", "0xe08822", "0x5e8eb7", "0x6c6c6c", "0xbd5757", "0x99c33d", "0x4a4cba", "0x7b2e89", "0x95d5e7", "0xffffff"
    };
    private static final Random random = new Random();
    private String templatesPath = null;

    public static LootEntry.Builder<?> builder(String templatePath) {
        return builder((p_216169_1_, p_216169_2_, p_216169_3_, p_216169_4_) -> {
            int weight = findConfiguredWeight(templatePath);
            return new CapsuleLootEntry(templatePath, weight);
        });
    }

    public static int findConfiguredWeight(String path) {
        int weight = DEFAULT_WEIGHT;
        if (Config.lootTemplatesData.containsKey(path)) {
            weight = Config.lootTemplatesData.get(path).weight;
        }
        return weight;
    }

    /**
     * @param templatesPath
     * @param weightIn
     */
    protected CapsuleLootEntry(String templatesPath, int weightIn) {
        super(weightIn, 0, new ILootCondition[0], new ILootFunction[0]);
        this.templatesPath = templatesPath;
    }

    public static int getRandomColor() {
        return Integer.decode(COLOR_PALETTE[(int) (Math.random() * COLOR_PALETTE.length)]);
    }

    /**
     * Add all eligible capsuleList to the list to be picked from.
     */
    @Override
    public void func_216154_a(Consumer<ItemStack> stacks, LootContext context) {
        if (this.templatesPath == null) return;

        if (Config.lootTemplatesData.containsKey(this.templatesPath)) {

            Pair<String, CapsuleTemplate> templatePair = getRandomTemplate(context);

            if (templatePair != null) {
                CapsuleTemplate template = templatePair.getRight();
                String templatePath = templatePair.getLeft();
                int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));

                if (template.entities.isEmpty() && Config.allowBlueprintReward) {
                    // blueprint if there is no entities in the capsule
                    ItemStack capsule = Capsule.newLinkedCapsuleItemStack(
                            templatePath,
                            getRandomColor(),
                            getRandomColor(),
                            size,
                            false,
                            Capsule.labelFromPath(templatePath),
                            0);
                    CapsuleItem.setAuthor(capsule, template.getAuthor());
                    CapsuleItem.setState(capsule, CapsuleItem.STATE_BLUEPRINT);
                    CapsuleItem.setBlueprint(capsule);
                    CapsuleItem.setCanRotate(capsule, template.canRotate());
                    stacks.accept(capsule);
                } else {
                    // one use if there are entities and a risk of dupe
                    ItemStack capsule = Capsule.newRewardCapsuleItemStack(
                            templatePath,
                            getRandomColor(),
                            getRandomColor(),
                            size,
                            Capsule.labelFromPath(templatePath),
                            template.getAuthor());
                    CapsuleItem.setCanRotate(capsule, template.canRotate());
                    stacks.accept(capsule);
                }
            }

        }

    }

    @Nullable
    public Pair<String, CapsuleTemplate> getRandomTemplate(LootContext context) {
        Config.LootPathData lpd = Config.lootTemplatesData.get(this.templatesPath);
        if (lpd == null || lpd.files == null) {
            Files.populateAndLoadLootList(Config.getCapsuleConfigDir().toFile(), Config.lootTemplatesData);
            lpd = Config.lootTemplatesData.get(this.templatesPath);
        }
        if (lpd == null || lpd.files == null || lpd.files.isEmpty()) return null;

        int size = lpd.files.size();
        int initRand = random.nextInt(size);

        for (int i = 0; i < lpd.files.size(); i++) {
            int ri = (initRand + i) % lpd.files.size();
            String structureName = lpd.files.get(ri);
            CapsuleTemplate template = StructureSaver.getTemplateForReward(context.getWorld().getServer(), this.templatesPath + "/" + structureName).getRight();
            if (template != null) return Pair.of(this.templatesPath + "/" + structureName, template);
        }
        return null;
    }
}
