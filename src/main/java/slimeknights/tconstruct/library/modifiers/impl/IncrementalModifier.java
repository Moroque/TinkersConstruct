package slimeknights.tconstruct.library.modifiers.impl;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.List;

/**
 * Modifier which can take just part of an input instead of the whole input
 * TODO: consider moving incremental max to a field, serialized in JSON
 */
public class IncrementalModifier extends Modifier {
  @Override
  public Component getDisplayName(IToolStackView tool, int level) {
    int neededPerLevel = ModifierRecipeLookup.getNeededPerLevel(this.getId());
    Component name = this.getDisplayName(level);
    if (neededPerLevel > 0) {
      int amount = getAmount(tool);
      if (amount < neededPerLevel) {
        return name.copy().append(": " + amount + " / " + neededPerLevel);
      }
    }
    return name;
  }

  @Override
  public void onRemoved(IToolStackView tool) {
    // remove current progress in incremental modifiers
    tool.getPersistentData().remove(getId());
  }

  /* Helpers */

  /**
   * Gets the amount of value applied to the tool thus far
   * @param persistentData  Tool persistent mod NBT
   * @param modifier        Modifier instance
   * @return  Amount applied to the tool
   */
  public static int getAmount(IModDataView persistentData, ModifierId modifier) {
    if (persistentData.contains(modifier, Tag.TAG_ANY_NUMERIC)) {
      return persistentData.getInt(modifier);
    }
    return ModifierRecipeLookup.getNeededPerLevel(modifier);
  }

  /**
   * Gets the amount of value applied to the tool thus far
   * @param tool      Tool instance
   * @param modifier  Modifier instance
   * @return  Amount applied to the tool
   */
  public static int getAmount(IToolContext tool, ModifierId modifier) {
    return getAmount(tool.getPersistentData(), modifier);
  }

  /**
   * Gets the amount of this modifier on the tool
   * @param persistentData  Tool persistent mod NBT
   * @return  Amount
   */
  public int getAmount(IModDataView persistentData) {
    return getAmount(persistentData, this.getId());
  }

  /**
   * Gets the amount of this modifier on the tool
   * @param tool  Tool amount
   * @return  Amount
   */
  public int getAmount(IToolContext tool) {
    return getAmount(tool, this.getId());
  }

  /**
   * Gets the level scaled based on the current amount into the level
   * @param persistentData  Tool persistent mod NBT
   * @param level  Modifier level
   * @return  Level, possibly reduced by an incomplete level
   */
  public float getScaledLevel(IModDataView persistentData, int level) {
    if (level <= 0) {
      return 0;
    }
    int neededPerLevel = ModifierRecipeLookup.getNeededPerLevel(this.getId());
    if (neededPerLevel > 0) {
      int amount = getAmount(persistentData);
      // if amount == needed per level, returns level
      if (amount < neededPerLevel) {
        // if amount == 0, returns level - 1, otherwise returns some fractional amount
        return level + (getAmount(persistentData) - neededPerLevel) / (float)neededPerLevel;
      }
    }
    return level;
  }

  @Override
  public float getEffectiveLevel(IToolContext tool, int level) {
    return getScaledLevel(tool.getPersistentData(), level);
  }

  /**
   * Gets the level scaled based on the current amount into the level
   * @param tool   Tool instance
   * @param level  Modifier level
   * @return  Level, possibly reduced by an incomplete level
   * @deprecated use {@link #getEffectiveLevel(IToolContext, int)}
   */
  @Deprecated
  public float getScaledLevel(IToolContext tool, int level) {
    return getScaledLevel(tool.getPersistentData(), level);
  }

  /**
   * Sets the amount on the tool
   * @param persistentData  Tool NBT
   * @param modifier        Modifier to set
   * @param amount          New amount
   */
  public static void setAmount(ModDataNBT persistentData, ModifierId modifier, int amount) {
    persistentData.putInt(modifier, amount);
  }

  /**
   * Adds a tooltip showing the bonus damage and the type of damage dded
   * @param tool         Tool instance
   * @param level        Current level
   * @param levelAmount  Bonus per level
   * @param tooltip      Tooltip
   */
  protected void addDamageTooltip(IToolStackView tool, int level, float levelAmount, List<Component> tooltip) {
    addDamageTooltip(tool, getScaledLevel(tool, level) * levelAmount, tooltip);
  }
}
