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
import mezz.jei.api.JEIPlugin;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;

@JEIPlugin
public class CapsulePlugin extends BlankModPlugin {

	@Override
    public void register(@Nonnull IModRegistry registry)
    {
		
		IJeiHelpers jeiHelpers = registry.getJeiHelpers();

		// normally you should ignore nbt per-item, but these tags are universally understood
		// and apply to many vanilla and modded items
		jeiHelpers.getNbtIgnoreList().ignoreNbtTagNames(
				"color",
				"size",
				"upgraded"
		);

        List<IRecipe> recipes = new ArrayList<IRecipe>();
        // upgrade
        ItemStack ironCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, CapsuleItemsRegistrer.ironCapsuleSize.getInt());
        ItemStack ironCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xCCCCCC, CapsuleItemsRegistrer.ironCapsuleSize.getInt() + 2);
        ironCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
		ItemStack goldCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, CapsuleItemsRegistrer.goldCapsuleSize.getInt());
		ItemStack goldCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFD700, CapsuleItemsRegistrer.goldCapsuleSize.getInt() + 2);
		goldCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
		ItemStack diamondCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, CapsuleItemsRegistrer.diamondCapsuleSize.getInt());
		ItemStack diamondCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0x00FFF2, CapsuleItemsRegistrer.diamondCapsuleSize.getInt() + 2);
		diamondCapsuleUp.setTagInfo("upgraded", new NBTTagInt(1));
		ItemStack opCapsule = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, CapsuleItemsRegistrer.opCapsuleSize.getInt());
		opCapsule.setTagInfo("overpowered", new NBTTagByte((byte) 1));
		ItemStack opCapsuleUp = CapsuleItemsRegistrer.createCapsuleItemStack(0xFFFFFF, CapsuleItemsRegistrer.opCapsuleSize.getInt() + 2);
		opCapsuleUp.setTagInfo("overpowered", new NBTTagByte((byte) 1));
		
		ItemStack unlabelledCapsule = ironCapsule.copy();
		unlabelledCapsule.setItemDamage(CapsuleItem.STATE_LINKED);
		unlabelledCapsule.getTagCompound().setTag("linkPosition", new NBTTagCompound());
		
		ItemStack recoveryCapsule = ironCapsule.copy();
		recoveryCapsule.setItemDamage(CapsuleItem.STATE_ONE_USE);
		recoveryCapsule.getTagCompound().setBoolean("oneUse", true);
		recoveryCapsule.getTagCompound().setTag("linkPosition", new NBTTagCompound());
		

        ItemStack EnderPearlIS = new ItemStack(Items.ENDER_PEARL);
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,ironCapsule,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, ironCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,goldCapsule,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, goldCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,diamondCapsule,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, diamondCapsuleUp));
        recipes.add(new ShapedRecipes(3,3, new ItemStack[]{EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS,opCapsule,EnderPearlIS,EnderPearlIS,EnderPearlIS,EnderPearlIS}, opCapsuleUp));
        
        recipes.add(new ShapelessRecipes(recoveryCapsule, Arrays.asList(new ItemStack[] { unlabelledCapsule, new ItemStack(Items.GLASS_BOTTLE) })));
        recipes.add(new ShapelessRecipes(ironCapsule, Arrays.asList(new ItemStack[] { unlabelledCapsule })));

        registry.addRecipes(recipes);
        
        registry.addDescription(ironCapsule, "jei.capsule.desc.capsule");
        registry.addDescription(unlabelledCapsule, "jei.capsule.desc.linkedCapsule");
        registry.addDescription(recoveryCapsule, "jei.capsule.desc.recoveryCapsule");
        registry.addDescription(opCapsule, "jei.capsule.desc.opCapsule");
        registry.addDescription(new ItemStack(CapsuleBlocksRegistrer.blockCapsuleMarker), "jei.capsule.desc.capsuleMarker");
 
    }
}
