package capsule.plugins.jei;

import capsule.Config;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


@JEIPlugin
public class CapsulePlugin implements IModPlugin {

    final private static int UPGRADE_STEP = 2;

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(CapsuleItems.capsule, new CapsuleSubtypeInterpreter());
    }

    @Override
    public void register(@Nonnull IModRegistry registry) {

        // normally you should ignore nbt per-item, but these tags are universally understood
        // and apply to many vanilla and modded items
        List<IRecipe> recipes = new ArrayList<>();
        // upgrade
        ItemStack ironCapsule = CapsuleItems.createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize);
        ItemStack goldCapsule = CapsuleItems.createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize);
        ItemStack diamondCapsule = CapsuleItems.createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize);
        ItemStack opCapsule = CapsuleItems.createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize);
        opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));


        Ingredient cfp = Ingredient.fromItem(Items.CHORUS_FRUIT_POPPED);

        for (int upLevel = 1; upLevel < Math.min(8, Config.upgradeLimit); upLevel++) {
            registerUpgrade(recipes, ironCapsule, cfp, upLevel);
            registerUpgrade(recipes, goldCapsule, cfp, upLevel);
            registerUpgrade(recipes, diamondCapsule, cfp, upLevel);
            registerUpgrade(recipes, opCapsule, cfp, upLevel);
        }

        ItemStack unlabelledCapsule = getUnlabelledCapsule(ironCapsule);
        ItemStack recoveryCapsule = registerRecovery(recipes, ironCapsule, unlabelledCapsule);

        registry.addRecipes(recipes, VanillaRecipeCategoryUid.CRAFTING);
        registry.addIngredientInfo(ironCapsule, ItemStack.class, "jei.capsule.desc.capsule");
        registry.addIngredientInfo(unlabelledCapsule, ItemStack.class, "jei.capsule.desc.linkedCapsule");
        registry.addIngredientInfo(recoveryCapsule, ItemStack.class, "jei.capsule.desc.recoveryCapsule");
        registry.addIngredientInfo(opCapsule, ItemStack.class, "jei.capsule.desc.opCapsule");
        registry.addIngredientInfo(new ItemStack(CapsuleBlocks.blockCapsuleMarker), ItemStack.class, "jei.capsule.desc.capsuleMarker");

    }

    private ItemStack registerRecovery(List<IRecipe> recipes, ItemStack emptyCapsule, ItemStack unlabelledCapsule) {
        Ingredient unlabelledCapsuleIng = Ingredient.fromStacks(unlabelledCapsule);
        ItemStack recoveryCapsule = getRecoveryCapsule(emptyCapsule);
        recipes.add(new ShapelessRecipes("capsule", recoveryCapsule, NonNullList.from(Ingredient.EMPTY, unlabelledCapsuleIng, Ingredient.fromItem(Items.GLASS_BOTTLE))));
        recipes.add(new ShapelessRecipes("capsule", emptyCapsule, NonNullList.from(Ingredient.EMPTY, unlabelledCapsuleIng)));
        return recoveryCapsule;
    }

    private void registerUpgrade(List<IRecipe> recipes, ItemStack capsule, Ingredient upgradeIng, int upLevel) {
        ItemStack capsuleUp = getUpgradedCapsule(capsule, upLevel);
        Ingredient ironCapsuleIng = Ingredient.fromStacks(capsule);
        NonNullList<Ingredient> ingredients = NonNullList.withSize(upLevel + 1, upgradeIng);
        ingredients.set(0, ironCapsuleIng);
        recipes.add(new ShapelessRecipes("capsule", capsuleUp, ingredients));
    }

    private ItemStack getUnlabelledCapsule(ItemStack capsule) {
        ItemStack unlabelledCapsule = capsule.copy();
        unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "JEIExemple");
        return unlabelledCapsule;
    }

    private ItemStack getRecoveryCapsule(ItemStack capsule) {
        ItemStack recoveryCapsule = capsule.copy();
        CapsuleItem.setOneUse(recoveryCapsule);
        CapsuleItem.setStructureName(recoveryCapsule, "JEIExemple");
        return recoveryCapsule;
    }

    private ItemStack getUpgradedCapsule(ItemStack ironCapsule, int upLevel) {
        ItemStack capsuleUp = ironCapsule.copy();
        CapsuleItem.setSize(capsuleUp, CapsuleItem.getSize(ironCapsule) + upLevel * UPGRADE_STEP);
        CapsuleItem.setUpgradeLevel(capsuleUp, upLevel);
        capsuleUp.setTagInfo("upgraded", new NBTTagInt(upLevel));
        return capsuleUp;
    }

    private static class CapsuleSubtypeInterpreter implements ISubtypeRegistry.ISubtypeInterpreter {

        @Override
        public String apply(ItemStack itemStack) {
            if (!(itemStack.getItem() instanceof CapsuleItem)) return null;
            String isOP = String.valueOf(itemStack.getTagCompound() != null && itemStack.getTagCompound().hasKey("overpowered") && itemStack.getTagCompound().getBoolean("overpowered"));
            String capsuleState = String.valueOf(itemStack.getItemDamage());
            String capsuleColor = String.valueOf(CapsuleItem.getMaterialColor(itemStack));
            return capsuleState + capsuleColor + isOP;
        }
    }
}
