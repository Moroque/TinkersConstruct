package slimeknights.tconstruct.tables.block.entity.table;

import io.github.fabricators_of_create.porting_lib.event.common.ItemCraftedCallback;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.RecipeManagerAccessor;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.transfer.item.ItemHandlerHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.shared.inventory.ConfigurableInvWrapperCapability;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.block.entity.inventory.PartBuilderContainerWrapper;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PartBuilderBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
  /** First slot containing materials */
  public static final int MATERIAL_SLOT = 0;
  /** Second slot containing the patterns */
  public static final int PATTERN_SLOT = 1;
  /** Title for the GUI */
  private static final Component NAME = TConstruct.makeTranslation("gui", "part_builder");

  /** Result inventory, lazy loads results */
  @Getter
  private final LazyResultContainer craftingResult;
  /** Crafting inventory for the recipe calls */
  @Getter
  private final PartBuilderContainerWrapper inventoryWrapper;

  /* Current buttons to display */
  @Nullable
  private Map<Pattern,IPartBuilderRecipe> recipes = null;
  @Nullable
  private List<Pattern> sortedButtons = null;
  /** Currently selected recipe index */
  private Pattern selectedPattern = null;
  /** Index of the currently selected pattern */
  private int selectedPatternIndex = -2;

  public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.partBuilderTile.get(), pos, state, NAME, 2);
    this.itemHandler = new ConfigurableInvWrapperCapability(this, false, false);
    this.itemHandlerCap = LazyOptional.of(() -> this.itemHandler);
    this.inventoryWrapper = new PartBuilderContainerWrapper(this);
    this.craftingResult = new LazyResultContainer(this);
  }

  /**
   * Gets a map of all recipes for the current inputs
   * @return  List of recipes for the current inputs
   */
  protected Map<Pattern,IPartBuilderRecipe> getCurrentRecipes() {
    if (level == null) {
      return Collections.emptyMap();
    }
    if (recipes == null) {
      // no recipes if we lack a pattern
      if (getItem(PATTERN_SLOT).isEmpty()) {
        recipes = Collections.emptyMap();
        sortedButtons = Collections.emptyList();
      } else {
        record PatternRecipe(Pattern pattern, IPartBuilderRecipe recipe) {}
        // fetch all recipes that can match these inputs, the map ensures the patterns are unique
        recipes = ((RecipeManagerAccessor)level.getRecipeManager()).port_lib$byType(TinkerRecipeTypes.PART_BUILDER.get()).values().stream()
                       .filter(r -> r instanceof IPartBuilderRecipe)
                       .map(r -> (IPartBuilderRecipe)r)
                       .filter(r -> r.partialMatch(inventoryWrapper))
                       .sorted(Comparator.comparing(Recipe::getId))
                       .flatMap(r -> r.getPatterns(inventoryWrapper).map(p -> new PatternRecipe(p, r)))
                       .collect(Collectors.toMap(PatternRecipe::pattern, PatternRecipe::recipe, (a, b) -> a));
        sortedButtons = recipes.entrySet()
                               .stream()
                               .sorted(Comparator.<Entry<Pattern,IPartBuilderRecipe>>comparingInt(ent -> ent.getValue().getCost()).thenComparing(Entry::getKey))
                               .map(Entry::getKey).collect(Collectors.toList());
      }
    }
    return recipes;
  }

  /** Gets the list of sorted buttons */
  public List<Pattern> getSortedButtons() {
    if (level == null) {
      return Collections.emptyList();
    }
    if (sortedButtons == null) {
      getCurrentRecipes();
    }
    return sortedButtons;
  }

  /** Gets the index of the selected pattern */
  public int getSelectedIndex() {
    if (selectedPatternIndex == -2) {
      if (selectedPattern != null) {
        selectedPatternIndex = getSortedButtons().indexOf(selectedPattern);
      } else {
        selectedPatternIndex = -1;
      }
    }
    return selectedPatternIndex;
  }

  /**
   * Gets the currently selected recipe
   * @return  Selected recipe, or null if invalid or no recipe
   */
  @Nullable
  public IPartBuilderRecipe getPartRecipe() {
    if (selectedPattern != null) {
      return getCurrentRecipes().get(selectedPattern);
    }
    return null;
  }

  /** Gets the first available recipe */
  @Nullable
  public IPartBuilderRecipe getFirstRecipe() {
    List<Pattern> sortedButtons = getSortedButtons();
    if (sortedButtons.isEmpty()) {
      return null;
    }
    return getCurrentRecipes().get(sortedButtons.get(0));
  }

  /**
   * Gets the material recipe for the material slot
   * @return  Material slot
   */
  @Nullable
  public IMaterialValue getMaterialRecipe() {
    return inventoryWrapper.getMaterial();
  }

  /**
   * Refreshes the current recipe
   * @param refreshRecipeList  If true, refreshes the full recipe list too
   */
  private void refresh(boolean refreshRecipeList) {
    if (refreshRecipeList) {
      this.recipes = null;
      this.sortedButtons = null;
    }
    this.selectedPatternIndex = -2;
    this.craftingResult.clearContent();
    // update screen display
    if (refreshRecipeList && level != null && !level.isClientSide) {
      syncToRelevantPlayers(this::syncScreen);
    }
  }

  /**
   * Selects a recipe in the table
   * @param pattern  New pattern
   */
  public void selectRecipe(@Nullable Pattern pattern) {
    if (pattern != null && getCurrentRecipes().containsKey(pattern)) {
      selectedPattern = pattern;
    } else {
      selectedPattern = null;
    }
    refresh(false);
  }

  /**
   * Selects a pattern by index
   * @param index  New index
   */
  public void selectRecipe(int index) {
    if (index < 0) {
      selectedPattern = null;
    } else {
      List<Pattern> list = getSortedButtons();
      if (index < list.size()) {
        selectedPattern = list.get(index);
      } else {
        selectedPattern = null;
      }
    }
    refresh(false);
  }

  boolean tagMatches(ItemStack itemStack, ItemStack itemStack2) {
    if (itemStack.isEmpty() && itemStack2.isEmpty()) {
      return true;
    } else if (itemStack.isEmpty() || itemStack2.isEmpty()) {
      return false;
    } else if (itemStack.tag == null && itemStack2.tag != null) {
      return false;
    } else {
      return itemStack.tag == null || itemStack.tag.equals(itemStack2.tag);
    }
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    ItemStack original = getItem(slot);
    super.setItem(slot, stack);
    if (slot == MATERIAL_SLOT) {
      // if item or NBT changed, update
      if (original.getItem() != stack.getItem() || !tagMatches(original, stack)) {
        this.inventoryWrapper.refreshMaterial();
        refresh(true);
        // if size changed, we are still the same material but might no longer have enough
      } else if (original.getCount() != stack.getCount()) {
        this.craftingResult.clearContent();
      }
      // any other slot, only an item change means update
    } else if (original.getItem() != stack.getItem()) {
      refresh(true);
    }
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new PartBuilderContainerMenu(menuId, playerInventory, this);
  }

  @Override
  public ItemStack calcResult(@Nullable Player player) {
    if (level != null) {
      IPartBuilderRecipe recipe = getPartRecipe();
      if (recipe != null && recipe.matches(inventoryWrapper, level)) {
        return recipe.assemble(inventoryWrapper, selectedPattern, level.registryAccess());
      }
    }
    return ItemStack.EMPTY;
  }

  /**
   * Shrinks the given slot
   * @param slot    Slot
   * @param amount  Amount to shrink
   */
  private void shrinkSlot(int slot, int amount, Player player) {
    ItemStack stack = getItem(slot);
    if (!stack.isEmpty()) {
      ItemStack container = stack.getRecipeRemainder().copy();
      if (amount > 0) {
        container.setCount(container.getCount() * amount);
      }
      if (stack.getCount() <= amount) {
        setItem(slot, container);
      } else {
        stack.shrink(amount);
        ItemHandlerHelper.giveItemToPlayer(player, container);
      }
    }
  }

  @Override
  public void onCraft(Player player, ItemStack result, int amount) {
    if (amount == 0 || this.level == null) {
      return;
    }
    // the recipe should match if we got this far, but being null is a problem
    IPartBuilderRecipe recipe = getPartRecipe();
    if (recipe == null) {
      return;
    }

    // we are definitely crafting at this point
    result.onCraftedBy(this.level, player, amount);
    ItemCraftedCallback.EVENT.invoker().onCraft(player, result, this.inventoryWrapper);
    this.playCraftSound(player);

    // give the player any leftovers
    if (level != null && !level.isClientSide) {
      ItemStack leftover = recipe.getLeftover(inventoryWrapper, selectedPattern);
      if (!leftover.isEmpty()) {
        player.getInventory().placeItemBackInInventory(leftover);}
    }

    // shrink the inputs
    shrinkSlot(MATERIAL_SLOT, recipe.getItemsUsed(inventoryWrapper), player);
    if (!getItem(PATTERN_SLOT).is(TinkerTags.Items.REUSABLE_PATTERNS)) {
      shrinkSlot(PATTERN_SLOT, 1, player);
    }

    // sync display, mainly for the material value
    if (level != null && !level.isClientSide) {
      syncToRelevantPlayers(this::syncScreen);
    }
  }
}
