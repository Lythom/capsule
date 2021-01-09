package capsule.recipes;

import capsule.items.CapsuleItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class CapsuleIngredient extends Ingredient
{
    private final ItemStack referenceStack;

    protected CapsuleIngredient(ItemStack stack)
    {
        super(Stream.of(new Ingredient.SingleItemList(stack)));
        this.referenceStack = stack;
    }

    @Override
    public boolean test(@Nullable ItemStack input)
    {
        if (input == null || !(input.getItem() instanceof CapsuleItem))
            return false;
        return this.referenceStack.getItem() == input.getItem()
                && CapsuleItem.hasState(input, CapsuleItem.getState(referenceStack))
                && CapsuleItem.isBlueprint(input) == CapsuleItem.isBlueprint(referenceStack);
    }

    @Override
    public boolean isSimple()
    {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer()
    {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement serialize()
    {
        JsonObject json = new JsonObject();
        json.addProperty("type", CraftingHelper.getID(Serializer.INSTANCE).toString());
        json.addProperty("item", referenceStack.getItem().getRegistryName().toString());
        json.addProperty("count", referenceStack.getCount());
        if (referenceStack.hasTag())
            json.addProperty("nbt", referenceStack.getTag().toString());
        return json;
    }

    public static class Serializer implements IIngredientSerializer<CapsuleIngredient>
    {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CapsuleIngredient parse(PacketBuffer buffer) {
            return new CapsuleIngredient(buffer.readItemStack());
        }

        @Override
        public CapsuleIngredient parse(JsonObject json) {
            return new CapsuleIngredient(CraftingHelper.getItemStack(json, true));
        }

        @Override
        public void write(PacketBuffer buffer, CapsuleIngredient ingredient) {
            buffer.writeItemStack(ingredient.referenceStack);
        }
    }
}