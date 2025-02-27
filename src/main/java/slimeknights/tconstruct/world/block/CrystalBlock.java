package slimeknights.tconstruct.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrystalBlock extends Block {
  private final SoundEvent chimeSound;
  public CrystalBlock(SoundEvent chimeSound, Properties props) {
    super(props);
    this.chimeSound = chimeSound;
  }

  @Override
  public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
    if (!level.isClientSide) {
      BlockPos pos = hit.getBlockPos();
      level.playSound(null, pos, getSoundType(state).getHitSound(), SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
      level.playSound(null, pos, chimeSound, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
    }
  }
}
