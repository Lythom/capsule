package capsule.recipes;

import capsule.recipes.PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrefabsBlueprintAggregatorRecipeTest {

    @Test
    void templateIngredientsAreFoundInTheDefaultPattern() {
        assertEquals(Arrays.asList(0, 2, 4), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(Arrays.asList("2b3", "l1l", " p ")));
    }

    @Test
    void templateIngredientsAreFoundInAMovedAndShrunkPattern() {
        // the blank first column is removed by the recipe, leaving a 2 wide pattern
        assertEquals(Arrays.asList(0, 3, 5), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(Arrays.asList(" 1b", " l2", " p3")));
        assertEquals(Arrays.asList(1, 3), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(Arrays.asList("   ", "b1l", "3  ")));
    }
}
