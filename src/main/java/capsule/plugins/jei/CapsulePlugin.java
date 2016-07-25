package capsule.plugins.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagInt;

@JEIPlugin
public class CapsulePlugin extends BlankModPlugin {

	@Override
    public void register(@Nonnull IModRegistry registry)
    {
		
		IJeiHelpers jeiHelpers = registry.getJeiHelpers();
		jeiHelpers.getSubtypeRegistry().registerNbtInterpreter(CapsuleItemsRegistrer.capsule, new CapsuleSubtypeInterpreter());

		// normally you should ignore nbt per-item, but these tags are universally understood
		// and apply to many vanilla and modded items

        List<IRecipe> recipes = new ArrayList<IRecipe>();
        // upgrade
        ItemStack ironCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, CapsuleItemsRegistrer.ironCapsuleSize.getInt());
        ItemStack ironCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, CapsuleItemsRegistrer.ironCapsuleSize.getInt() + 2);
        ironCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
        ironCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
        ironCapsuleUp.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack goldCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, CapsuleItemsRegistrer.goldCapsuleSize.getInt());
		ItemStack goldCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, CapsuleItemsRegistrer.goldCapsuleSize.getInt() + 2);
		goldCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
		goldCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
		goldCapsuleUp.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack diamondCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, CapsuleItemsRegistrer.diamondCapsuleSize.getInt());
		ItemStack diamondCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, CapsuleItemsRegistrer.diamondCapsuleSize.getInt() + 2);
		diamondCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
		diamondCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
		diamondCapsuleUp.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack opCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, CapsuleItemsRegistrer.opCapsuleSize.getInt());
		opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
		ItemStack opCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, CapsuleItemsRegistrer.opCapsuleSize.getInt() + 2);
		opCapsuleUp.setTagInfo("overpowered", new NBTTagByte((byte) 1));
		opCapsuleUp.setItemDamage(CapsuleItem.STATE_LINKED);
		opCapsuleUp.getTagCompound().setString("structureName", "JEIExemple");
		
		ItemStack unlabelledCapsule = ironCapsule.copy();
		unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
		unlabelledCapsule.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack unlabelledCapsuleGold = goldCapsule.copy();
		unlabelledCapsuleGold.setItemDamage(CapsuleItem.STATE_LINKED);
		unlabelledCapsuleGold.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack unlabelledCapsuleDiamond = diamondCapsule.copy();
		unlabelledCapsuleDiamond.setItemDamage(CapsuleItem.STATE_LINKED);
		unlabelledCapsuleDiamond.getTagCompound().setString("structureName", "JEIExemple");
		ItemStack unlabelledCapsuleOP = opCapsule.copy();
		unlabelledCapsuleOP.setItemDamage(CapsuleItem.STATE_LINKED);
		unlabelledCapsuleOP.getTagCompound().setString("structureName", "JEIExemple");
		
		ItemStack recoveryCapsule = ironCapsule.copy();
		recoveryCapsule.setItemDamage(CapsuleItem.STATE_ONE_USE);
		recoveryCapsule.getTagCompound().setBoolean("oneUse", true);
		recoveryCapsule.getTagCompound().setString("structureName", "JEIExemple");
		

        ItemStack EnderPearlIS = new ItemStack(Items.ENDER_PEARL);
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,unlabelledCapsule,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, ironCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,unlabelledCapsuleGold,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, goldCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,unlabelledCapsuleDiamond,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, diamondCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,unlabelledCapsuleOP,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, opCapsuleUp));
        
        recipes.add(new ShapelessRecipes(recoveryCapsule, Arrays.asList(new ItemStack[] { unlabelledCapsule, new ItemStack(Items.GLASS_BOTTLE) })));
        recipes.add(new ShapelessRecipes(ironCapsule, Arrays.asList(new ItemStack[] { unlabelledCapsule })));

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
        	if(itemStack == null || !(itemStack.getItem() instanceof CapsuleItem)) return null;
        	CapsuleItem item = (CapsuleItem) itemStack.getItem();
        	return String.valueOf(itemStack.getItemDamage()) + itemStack.getTagCompound().getBoolean("overpowered");
        }
    }
}
