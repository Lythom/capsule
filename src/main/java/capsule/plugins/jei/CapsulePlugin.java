package capsule.plugins.jei;

import capsule.Config;
import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import mezz.jei.api.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JEIPlugin
public class CapsulePlugin extends BlankModPlugin {

    @Override
    public void register(@Nonnull IModRegistry registry) {

        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        jeiHelpers.getSubtypeRegistry().registerNbtInterpreter(CapsuleItemsRegistrer.capsule, new CapsuleSubtypeInterpreter());

        // normally you should ignore nbt per-item, but these tags are universally understood
        // and apply to many vanilla and modded items

        List<IRecipe> recipes = new ArrayList<IRecipe>();
        // upgrade
        ItemStack ironCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize);
        ItemStack ironCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, Config.ironCapsuleSize + 2);
        ironCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
        ItemStack ironCapsuleUpUp = ironCapsuleUp.copy();
        ironCapsuleUpUp.setTagInfo("upgraded", new NBTTagInt(2));
        ItemStack ironCapsuleUpUpUp = ironCapsuleUpUp.copy();
        ironCapsuleUpUpUp.setTagInfo("upgraded", new NBTTagInt(3));
        ItemStack ironCapsuleUpUpUpUp = ironCapsuleUpUpUp.copy();
        ironCapsuleUpUpUpUp.setTagInfo("upgraded", new NBTTagInt(4));

        ItemStack goldCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize);
        ItemStack goldCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, Config.goldCapsuleSize + 2);
        goldCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
        goldCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(goldCapsuleUp, "JEIExemple");
        ItemStack diamondCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize);
        ItemStack diamondCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, Config.diamondCapsuleSize + 2);
        diamondCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
        diamondCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(diamondCapsuleUp, "JEIExemple");
        ItemStack opCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize);
        opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
        ItemStack opCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, Config.opCapsuleSize + 2);
        opCapsuleUp.setTagInfo("overpowered", new NBTTagByte((byte) 1));
        opCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(opCapsuleUp, "JEIExemple");

        ItemStack unlabelledCapsule = ironCapsule.copy();
        unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsule, "JEIExemple");
        ItemStack unlabelledCapsuleGold = goldCapsule.copy();
        unlabelledCapsuleGold.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsuleGold, "JEIExemple");
        ItemStack unlabelledCapsuleDiamond = diamondCapsule.copy();
        unlabelledCapsuleDiamond.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsuleDiamond, "JEIExemple");
        ItemStack unlabelledCapsuleOP = opCapsule.copy();
        unlabelledCapsuleOP.setItemDamage(CapsuleItem.STATE_LINKED);
        CapsuleItem.setStructureName(unlabelledCapsuleOP, "JEIExemple");

        ItemStack recoveryCapsule = ironCapsule.copy();
        CapsuleItem.setOneUse(recoveryCapsule);
        CapsuleItem.setStructureName(recoveryCapsule, "JEIExemple");


        ItemStack chorusFruitIS = new ItemStack(Items.CHORUS_FRUIT_POPPED);
        if (Config.upgradeLimit > 0)
            recipes.add(new ShapelessRecipes(ironCapsuleUp, Arrays.asList(new ItemStack[]{ironCapsule, chorusFruitIS})));
        if (Config.upgradeLimit > 1)
            recipes.add(new ShapelessRecipes(ironCapsuleUpUp, Arrays.asList(new ItemStack[]{ironCapsule, chorusFruitIS, chorusFruitIS})));
        if (Config.upgradeLimit > 2)
            recipes.add(new ShapelessRecipes(ironCapsuleUpUpUp, Arrays.asList(new ItemStack[]{ironCapsule, chorusFruitIS, chorusFruitIS, chorusFruitIS})));
        if (Config.upgradeLimit > 3)
            recipes.add(new ShapelessRecipes(ironCapsuleUpUpUpUp, Arrays.asList(new ItemStack[]{ironCapsule, chorusFruitIS, chorusFruitIS, chorusFruitIS, chorusFruitIS})));

        recipes.add(new ShapelessRecipes(recoveryCapsule, Arrays.asList(new ItemStack[]{unlabelledCapsule, new ItemStack(Items.GLASS_BOTTLE)})));
        recipes.add(new ShapelessRecipes(ironCapsule, Arrays.asList(new ItemStack[]{unlabelledCapsule})));

        registry.addRecipes(recipes);

        registry.addDescription(ironCapsule, "jei.capsule.desc.capsule");
        registry.addDescription(unlabelledCapsule, "jei.capsule.desc.linkedCapsule");
        registry.addDescription(recoveryCapsule, "jei.capsule.desc.recoveryCapsule");
        registry.addDescription(opCapsule, "jei.capsule.desc.opCapsule");
        registry.addDescription(new ItemStack(CapsuleBlocksRegistrer.blockCapsuleMarker), "jei.capsule.desc.capsuleMarker");

    }

    private static class CapsuleSubtypeInterpreter implements ISubtypeRegistry.ISubtypeInterpreter {

        @Override
        public String getSubtypeInfo(@Nonnull ItemStack itemStack) {
            if (itemStack == null || !(itemStack.getItem() instanceof CapsuleItem)) return null;
            return String.valueOf(itemStack.getItemDamage()) + itemStack.getTagCompound().getBoolean("overpowered");
        }
    }
}
