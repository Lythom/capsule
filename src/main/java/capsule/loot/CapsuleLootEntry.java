package capsule.loot;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.helpers.Files;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;

/**
 * @author Lythom
 */
public class CapsuleLootEntry extends LootPoolSingletonContainer {

    public static final MapCodec<CapsuleLootEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.STRING.fieldOf("templates_path").forGetter(entry -> entry.templatesPath))
            .and(singletonFields(instance))
            .apply(instance, CapsuleLootEntry::new));

    private static final DeferredRegister<LootPoolEntryType> ENTRY_TYPES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, CapsuleMod.MODID);
    public static final Supplier<LootPoolEntryType> TYPE = ENTRY_TYPES.register("capsule", () -> new LootPoolEntryType(CODEC));

    public static void registerEntryType(IEventBus modEventBus) {
        ENTRY_TYPES.register(modEventBus);
    }

    public static final int DEFAULT_WEIGHT = 3;
    public static String[] COLOR_PALETTE = new String[]{
            "0xCCCCCC", "0x549b57", "0xe08822", "0x5e8eb7", "0x6c6c6c", "0xbd5757", "0x99c33d", "0x4a4cba", "0x7b2e89", "0x95d5e7", "0xffffff"
    };
    private static final Random random = new Random();
    private final String templatesPath;

    public static LootPoolEntryContainer.Builder<?> builder(String templatePath) {
        return simpleBuilder((p_216169_1_, p_216169_2_, p_216169_3_, p_216169_4_) -> {
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
        this(templatesPath, weightIn, 0, List.of(), List.of());
    }

    private CapsuleLootEntry(String templatesPath, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.templatesPath = templatesPath;
    }

    public static int getRandomColor() {
        return Integer.decode(COLOR_PALETTE[new java.util.Random().nextInt(COLOR_PALETTE.length)]);
    }

    /**
     * Add all eligible capsuleList to the list to be picked from.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> stacks, LootContext context) {
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
                    CapsuleItem.setState(capsule, BLUEPRINT);
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
            Files.populateAndLoadLootList(Config.getCapsuleConfigDir().toFile(), Config.lootTemplatesData, context.getLevel().getServer().getResourceManager());
            lpd = Config.lootTemplatesData.get(this.templatesPath);
        }
        if (lpd == null || lpd.files == null || lpd.files.isEmpty()) return null;

        int size = lpd.files.size();
        int initRand = random.nextInt(size);

        for (int i = 0; i < lpd.files.size(); i++) {
            int ri = (initRand + i) % lpd.files.size();
            String structureName = lpd.files.get(ri);
            CapsuleTemplate template = StructureSaver.getTemplateForReward(context.getLevel().getServer(), this.templatesPath + "/" + structureName).getRight();
            if (template != null) return Pair.of(this.templatesPath + "/" + structureName, template);
        }
        return null;
    }

    @Override
    public LootPoolEntryType getType() {
        return TYPE.get();
    }
}
