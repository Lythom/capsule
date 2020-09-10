package capsule.loot;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.Files;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.ILootGenerator;
import net.minecraft.world.storage.loot.conditions.ILootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

/**
 * @author Lythom
 */
public class CapsuleLootEntry extends ILootGenerator {

    public static String[] COLOR_PALETTE = new String[]{
            "0xCCCCCC", "0x549b57", "0xe08822", "0x5e8eb7", "0x6c6c6c", "0xbd5757", "0x99c33d", "0x4a4cba", "0x7b2e89", "0x95d5e7", "0xffffff"
    };
    private static Random random = new Random();
    private String templatesPath = null;

    /**
     * @param templatesPath
     * @param weightIn
     * @param qualityIn
     * @param conditionsIn
     * @param entryName
     */
    protected CapsuleLootEntry(String templatesPath, int weightIn, int qualityIn, ILootCondition[] conditionsIn, String entryName) {
        super(weightIn, qualityIn, conditionsIn, entryName);
        this.templatesPath = templatesPath;
    }

    public static int getRandomColor() {
        return Integer.decode(COLOR_PALETTE[(int) (Math.random() * COLOR_PALETTE.length)]);
    }

    /**
     * Add all eligible capsuleList to the list to be picked from.
     */
    @Override
    public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
        if (this.templatesPath == null) return;

        if (LootConditionManager.testAllConditions(this.conditions, rand, context) && Config.lootTemplatesData.containsKey(this.templatesPath)) {

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
                    stacks.add(capsule);
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
                    stacks.add(capsule);
                }
            }

        }

    }

    @Nullable
    public Pair<String, CapsuleTemplate> getRandomTemplate(LootContext context) {
        LootPathData lpd = Config.lootTemplatesData.get(this.templatesPath);
        if (lpd == null || lpd.files == null) {
            Files.populateAndLoadLootList(Config.configDir, Config.lootTemplatesPaths, Config.lootTemplatesData);
            lpd = Config.lootTemplatesData.get(this.templatesPath);
        }
        if (lpd == null || lpd.files == null || lpd.files.isEmpty()) return null;

        int size = lpd.files.size();
        int initRand = random.nextInt(size);

        for (int i = 0; i < lpd.files.size(); i++) {
            int ri = (initRand + i) % lpd.files.size();
            String structureName = lpd.files.get(ri);
            CapsuleTemplate template = StructureSaver.getTemplateForReward(context.getWorld().getMinecraftServer(), this.templatesPath + "/" + structureName).getRight();
            if (template != null) return Pair.of(this.templatesPath + "/" + structureName, template);
        }
        return null;
    }

    @Override
    protected void serialize(JsonObject json, JsonSerializationContext context) {

    }

}
