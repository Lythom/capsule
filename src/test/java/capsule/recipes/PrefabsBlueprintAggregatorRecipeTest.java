package capsule.recipes;

import capsule.recipes.PrefabsBlueprintAggregatorRecipe.PrefabsBlueprintCapsuleRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrefabsBlueprintAggregatorRecipeTest {

    @Test
    void templateIngredientsAreFoundInTheDefaultPattern() {
        assertEquals(List.of(0, 2, 4), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(List.of("2b3", "l1l", " p ")));
    }

    @Test
    void templateIngredientsAreFoundInAMovedAndShrunkPattern() {
        // the blank first column is removed by the recipe, leaving a 2 wide pattern
        assertEquals(List.of(0, 3, 5), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(List.of(" 1b", " l2", " p3")));
        assertEquals(List.of(1, 3), PrefabsBlueprintCapsuleRecipe.templateIngredientIndexes(List.of("   ", "b1l", "3  ")));
    }
}
