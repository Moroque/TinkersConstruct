package slimeknights.tconstruct.world.block;

import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

public class StickySlimeBlock extends SlimeBlock {

  private final BiPredicate<BlockState, BlockState> stickyPredicate;
  public StickySlimeBlock(Properties properties, BiPredicate<BlockState, BlockState> stickyPredicate) {
    super(properties);
    this.stickyPredicate = stickyPredicate;
  }

//  @Override TODO: PORT
  public boolean isSlimeBlock(BlockState state) {
    return true;
  }

//  @Override
  public boolean isStickyBlock(BlockState state) {
    return true;
  }

//  @Override
  public boolean canStickTo(BlockState state, BlockState other) {
    return stickyPredicate.test(state, other);
  }
}
