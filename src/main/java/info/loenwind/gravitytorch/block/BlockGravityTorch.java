package info.loenwind.gravitytorch.block;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class BlockGravityTorch extends BlockFalling {

  private static final float px = 1f / 16f;

  public static int renderId = -1;

  public BlockGravityTorch() {
    super(Material.circuits);
    this.setTickRandomly(true);
    this.setCreativeTab(CreativeTabs.tabDecorations);
    setHardness(0.0F);
    setLightLevel(0.9375F);
    setStepSound(soundTypeWood);
    setBlockName("gravity_torch");
    setBlockTextureName("torch_on");
    float diameter = 4f * px;
    float height = (10f + 2f) * px;
    setBlockBounds(0.5F - diameter / 2, 0.0F, 0.5F - diameter / 2, 0.5F + diameter / 2, height, 0.5F + diameter / 2);
  }

  @Override
  public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z) {
    return null;
  }

  @Override
  public boolean isOpaqueCube() {
    return false;
  }

  @Override
  public boolean renderAsNormalBlock() {
    return false;
  }

  @Override
  public int getRenderType() {
    return renderId;
  }

  private boolean func_150107_m(World p_150107_1_, int p_150107_2_, int p_150107_3_, int p_150107_4_) {
    return World.doesBlockHaveSolidTopSurface(p_150107_1_, p_150107_2_, p_150107_3_, p_150107_4_);
  }

  @Override
  public boolean canPlaceBlockAt(World worldIn, int x, int y, int z) {
    return func_150107_m(worldIn, x, y - 1, z);
  }

  @Override
  public int onBlockPlaced(World worldIn, int x, int y, int z, int side, float subX, float subY, float subZ, int meta) {
    return 0; // item meta -> block meta
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(World worldIn, int x, int y, int z, Random random) {
    double d0 = x + 0.5F;
    double d1 = y + 0.7F + 2f * px;
    double d2 = z + 0.5F;

    worldIn.spawnParticle("smoke", d0, d1, d2, 0.0D, 0.0D, 0.0D);
    worldIn.spawnParticle("flame", d0, d1, d2, 0.0D, 0.0D, 0.0D);
  }

}
