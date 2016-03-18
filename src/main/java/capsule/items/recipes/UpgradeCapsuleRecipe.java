package capsule.items.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.World;

public class UpgradeCapsuleRecipe implements IRecipe {
	/** Is the ItemStack that you repair. */
	private final Item upgradeItem;
	private int upgradesMaxCount;

	public UpgradeCapsuleRecipe(Item upgradeItem, int upgradesMaxCount) {
		this.upgradeItem = upgradeItem;
		this.upgradesMaxCount = upgradesMaxCount;
	}

	public ItemStack getRecipeOutput() {
		return null;
	}

	public ItemStack[] getRemainingItems(InventoryCrafting inv) {
		ItemStack[] aitemstack = new ItemStack[inv.getSizeInventory()];

		for (int i = 0; i < aitemstack.length; ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			aitemstack[i] = net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack);
		}

		return aitemstack;
	}

	/**
	 * Used to check if a recipe matches current crafting inventory
	 */
	public boolean matches(InventoryCrafting craftingGrid, World worldIn) {

		for (int i = 0; i < craftingGrid.getHeight(); ++i) {
			for (int j = 0; j < craftingGrid.getWidth(); ++j) {
				if (!(i == 1 && j == 1)) {
					ItemStack stack = craftingGrid.getStackInRowAndColumn(i, j);
					if (stack == null || stack.getItem() != this.upgradeItem) {
						return false;
					}
				}
			}
		}

		ItemStack middleitemstack = craftingGrid.getStackInRowAndColumn(1, 1);
		return middleitemstack != null && middleitemstack.getItem() instanceof CapsuleItem
				&& middleitemstack.getItemDamage() == CapsuleItem.STATE_EMPTY
				&& (!middleitemstack.getTagCompound().hasKey("upgraded")
					|| middleitemstack.getTagCompound().getInteger("upgraded") < this.upgradesMaxCount);
	}

	/**
	 * Returns an Item that is the result of this recipe
	 */
	public ItemStack getCraftingResult(InventoryCrafting invC) {

		ItemStack middleitemstack = invC.getStackInRowAndColumn(1, 1);

		if (middleitemstack != null && middleitemstack.getItem() instanceof CapsuleItem
				&& middleitemstack.getItemDamage() == CapsuleItem.STATE_EMPTY) {
			ItemStack copy = middleitemstack.copy();
			CapsuleItem item = (CapsuleItem) copy.getItem();
			copy.setTagInfo("size", new NBTTagInt(middleitemstack.getTagCompound().getInteger("size") + 2));
			copy.setTagInfo("upgraded", new NBTTagInt(middleitemstack.getTagCompound().hasKey("upgraded")
					? middleitemstack.getTagCompound().getInteger("upgraded") + 1 : 1));
			return copy;
		}

		return null;
	}

	/**
	 * Returns the size of the recipe area
	 */
	public int getRecipeSize() {
		return 10;
	}
}