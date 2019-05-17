package capsule.recipes;

import capsule.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CapsuleRecipeFactory implements IRecipeFactory {
    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {

        // recipe default value
        JsonObject result = JsonUtils.getJsonObject(json, "result");
        JsonObject nbt = JsonUtils.getJsonObject(result, "nbt");
        int defaultSize = JsonUtils.getInt(nbt, "size");

        // config value
        JsonArray conditions = json.get("conditions").getAsJsonArray();
        int configSize = defaultSize;
        for (JsonElement condition : conditions) {
            String conditionType = JsonUtils.getString(condition.getAsJsonObject(), "type");
            if ("capsule:is_enabled".equals(conditionType)) {
                String property = JsonUtils.getString(condition.getAsJsonObject(), "property");
                if (Config.capsuleSizes.containsKey(property)) {
                    configSize = Config.capsuleSizes.get(property);
                }
            }
        }

        // override recipe size using Config value
        if (defaultSize != configSize) {
            nbt.addProperty("size", configSize);
        }
        return ShapedOreRecipe.factory(context, json);
    }
}