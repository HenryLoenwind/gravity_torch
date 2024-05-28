package info.loenwind.gravitytorch.renderer;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.client.render.RenderUtil;
import com.enderio.core.client.render.VertexTranslation;
import com.enderio.core.common.util.BlockCoord;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import info.loenwind.gravitytorch.CommonProxy;
import info.loenwind.gravitytorch.block.BlockGravityTorch;
import info.loenwind.gravitytorch.render.CachableRenderStatement;
import info.loenwind.gravitytorch.render.CacheRenderer;
import info.loenwind.gravitytorch.render.FaceRenderer;
import info.loenwind.gravitytorch.render.RenderingContext;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockGravityTorchRenderer implements ISimpleBlockRenderingHandler, IItemRenderer {

  private static List<CachableRenderStatement> csr;

  private static final float px = 1f / 16f;

  private static final float stem_diameter = 2f * px;
  private static final float stem_height = 10f * px;
  private static final float stem_offset = 2f * px;
  private static final BoundingBox bb_stem = new BoundingBox( //
      0.5F - stem_diameter / 2, 0f, 0.5F - stem_diameter / 2, //
      0.5F + stem_diameter / 2, stem_height, 0.5F + stem_diameter / 2);

  private static final VertexTranslation xform_stem = new VertexTranslation(0, stem_offset, 0);

  private static final float base_diameter = 4f * px;
  private static final float base_height = 4f * px;
  private static final BoundingBox bb_base = new BoundingBox( //
      0.5F - base_diameter / 2, 0.0F, 0.5F - base_diameter / 2, //
      0.5F + base_diameter / 2, base_height, 0.5F + base_diameter / 2);

  private static void setup() {
    if (csr == null) {
      IIcon[] icons_torch = RenderUtil.getBlockTextures(Blocks.torch, 0);
      IIcon[] icons_cobble = RenderUtil.getBlockTextures(Blocks.cobblestone, 0);

      CacheRenderer r = new CacheRenderer().setLighting(true).setBrightnessPerSide(FaceRenderer.stdBrightness);

      r.setBB(bb_stem).setXform(xform_stem).addSkirt(icons_torch, false, bb_stem);
      final IIcon tex = icons_torch[ForgeDirection.UP.ordinal()];
      r.addSingleFace(ForgeDirection.UP, //
          tex.getInterpolatedU(16f * bb_stem.minX), tex.getInterpolatedU(16f * bb_stem.maxX),
          tex.getInterpolatedV(16f * bb_stem.minZ - 1), tex.getInterpolatedV(16f * bb_stem.maxZ - 1), false);
      r.setBB(bb_base).addAllSides(icons_cobble, false, bb_base);

      csr = r.finishDrawing();
    }
  }

  @Override
  public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
      RenderBlocks renderer) {
    setup();

    Tessellator.instance.addTranslation(x, y, z);
    if (renderer.overrideBlockTexture != null) {
      FaceRenderer.setLightingReference(world, CommonProxy.blockGravityTorch, x, y, z);
      FaceRenderer.renderCube(bb_stem, renderer.overrideBlockTexture, xform_stem, null, false);
      FaceRenderer.renderCube(bb_base, renderer.overrideBlockTexture, null, null, false);
      FaceRenderer.clearLightingReference();
    } else {
      new RenderingContext(world, new BlockCoord(x, y, z)).execute(csr);
    }
    Tessellator.instance.addTranslation(-x, -y, -z);

    return true;
  }

  /**
   * Render the block as a falling entity.
   * <p>
   * The position (translation) has been set by the caller, but to the centre of
   * the block, so we need to translate to the min corner first.
   * <p>
   * Coordinates are given as lighting reference only. (Entities can't give off
   * light, so we pretend to be dark while falling. Let's say the flame gets blown
   * out and only re-ignites when stationary...)
   * 
   */
  public boolean renderFallingBlock(IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
    setup();

    Tessellator.instance.addTranslation(-.5f, -.5f, -.5f);
    new RenderingContext(world, new BlockCoord(x, y, z)).execute(csr);
    Tessellator.instance.addTranslation(.5f, .5f, .5f); // not actually needed, our caller does glPopMatrix() next. Just
                                                        // be on the safe side in case some mod takes over rendering...

    return true;
  }

  public static void renderBlock(boolean active) {
    setup();
    new RenderingContext().execute(csr);
    return;
  }

  @Override
  public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
    renderWorldBlock(null, 0, 0, 0, block, modelId, renderer);
  }

  @Override
  public boolean shouldRender3DInInventory(int modelId) {
    return true;
  }

  @Override
  public int getRenderId() {
    return BlockGravityTorch.renderId;
  }

  @Override
  public boolean handleRenderType(ItemStack item, ItemRenderType type) {
    return true;
  }

  @Override
  public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
    return true;
  }

  @Override
  public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//    Tessellator.instance.startDrawingQuads();
    GL11.glPushMatrix();
    float scale = 2.5f;
    float xTrans = -0.5f;
    float yTrans = 0;
    float zTrans = -0.5f;
    if (type == ItemRenderType.INVENTORY) {
      yTrans = -0.35f;
      scale = 1.8f;
    } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
      scale = 1.8f;
      yTrans = 0f;
      zTrans = -0.25f;
      xTrans = -0.25f;
    }
    GL11.glScalef(scale, scale, scale);
    GL11.glTranslatef(xTrans, yTrans, zTrans);
    RenderUtil.bindBlockTexture();
    renderWorldBlock(null, 0, 0, 0, CommonProxy.blockGravityTorch, getRenderId(), (RenderBlocks) data[0]);

//    Tessellator.instance.draw();
    GL11.glPopMatrix();

  }

}
