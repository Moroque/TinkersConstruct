package slimeknights.tconstruct.library.data.tinkering;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Data generator for mappings from enchantments to modifiers */
public abstract class AbstractEnchantmentToModifierProvider extends GenericDataProvider {
  /** Compiled JSON to save, no need to do anything fancier, it already does merging for us */
  private final JsonObject enchantmentMap = new JsonObject();

  public AbstractEnchantmentToModifierProvider(FabricDataOutput output) {
    super(output, PackType.SERVER_DATA, "tinkering");
  }

  /** Add any mappings */
  protected abstract void addEnchantmentMappings();

  @Override
  public CompletableFuture<?> run(CachedOutput pCache) {
    enchantmentMap.entrySet().clear();
    addEnchantmentMappings();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    futures.add(saveThing(pCache, TConstruct.getResource("enchantments_to_modifiers"), enchantmentMap));
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
  }

  /* Helpers */

  /** Adds the given enchantment */
  protected void add(Enchantment enchantment, ModifierId modifierId) {
    String key = Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.getKey(enchantment)).toString();
    if (enchantmentMap.has(key)) {
      throw new IllegalArgumentException("Duplicate enchantment " + key);
    }
    enchantmentMap.addProperty(key, modifierId.toString());
  }

  /** Adds the given enchantment tag */
  protected void add(TagKey<Enchantment> tag, ModifierId modifierId) {
    String key = "#" + tag.location();
    if (enchantmentMap.has(key)) {
      throw new IllegalArgumentException("Duplicate enchantment tag " + tag.location());
    }
    enchantmentMap.addProperty(key, modifierId.toString());
  }

  /** Adds the given enchantment tag */
  protected void add(ResourceLocation tag, ModifierId modifierId) {
    add(TagKey.create(Registries.ENCHANTMENT, tag), modifierId);
  }
}
