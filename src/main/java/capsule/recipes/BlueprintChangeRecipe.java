package capsule.recipes;

import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

public class BlueprintChangeRecipe extends SpecialRecipe {

    public BlueprintChangeRecipe(ResourceLocation id) {
        super(id);
    }

    public ItemStack getRecipeOutput() {
        ItemStack bp = new ItemStack(CapsuleItems.CAPSULE, 1);
        CapsuleItem.setState(bp, DEPLOYED);
        CapsuleItem.setBlueprint(bp);
        CapsuleItem.setBaseColor(bp, 3949738);
        CapsuleItem.setStructureName(bp, "config/capsule/rewards/example");
        return bp;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return CapsuleRecipes.BLUEPRINT_CHANGE_SERIALIZER;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        ItemStack blueprintCapsule = null;
        ItemStack templateCapsule = null;
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            if (blueprintCapsule == null && CapsuleItem.isBlueprint(itemstack)) {
                blueprintCapsule = itemstack;
            } else if (CapsuleItem.hasStructureLink(itemstack)) {
                templateCapsule = itemstack;
                nonnulllist.set(i, templateCapsule.copy());
            }

        }

        return nonnulllist;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(CraftingInventory inv, World worldIn) {
        int sourceCapsule = 0;
        int blueprint = 0;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);

            if (blueprint == 0 && CapsuleItem.isBlueprint(itemstack)) {
                blueprint++;

                // Any capsule having a template is valid except Deployed capsules (empty template) unless it is a blueprint (template never empty)
            } else if (CapsuleItem.hasStructureLink(itemstack) && (DEPLOYED != CapsuleItem.getState(itemstack) || CapsuleItem.isBlueprint(itemstack))) {
                sourceCapsule++;
            } else if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return sourceCapsule == 1 && blueprint == 1;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        String templateStructure = null;
        Integer templateSize = null;
        ItemStack blueprintCapsule = null;
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getStackInSlot(i);
            if (blueprintCapsule == null && CapsuleItem.isBlueprint(itemstack)) {
                blueprintCapsule = itemstack.copy();
            } else if (CapsuleItem.hasStructureLink(itemstack)) {
                templateStructure = CapsuleItem.getStructureName(itemstack);
                templateSize = CapsuleItem.getSize(itemstack);
            }
        }
        if (templateStructure != null && blueprintCapsule != null) {
            if (blueprintCapsule.getTag() != null) {
                blueprintCapsule.getTag().putString("prevStructureName", CapsuleItem.getStructureName(blueprintCapsule));
            }
            CapsuleItem.setStructureName(blueprintCapsule, templateStructure);
            CapsuleItem.setState(blueprintCapsule, DEPLOYED);
            CapsuleItem.setSize(blueprintCapsule, templateSize);
            CapsuleItem.cleanDeploymentTags(blueprintCapsule);
            return blueprintCapsule;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

}