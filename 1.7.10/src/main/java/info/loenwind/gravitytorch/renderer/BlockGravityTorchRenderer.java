package info.loenwind.gravitytorch.renderer;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.client.render.RenderUtil;
import com.enderio.core.client.render.VertexTranslation;
import com.enderio.core.common.util.BlockCoord;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import info.loenwind.gravitytorch.CommonProxy;
import info.loenwind.gravitytorch.block.BlockGravityTorch;
import info.loenwind.gravitytorch.config.Config;
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
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

@ThreadSafeISBRH(perThread = false)
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
      final IIcon[] icons_torch = RenderUtil.getBlockTextures(Blocks.torch, 0);
      final IIcon[] icons_cobble = RenderUtil.getBlockTextures(Blocks.cobblestone, 0);

      final CacheRenderer r = new CacheRenderer().setLighting(true).setBrightnessPerSide(FaceRenderer.stdBrightness);

      r.setBB(bb_stem).setXform(xform_stem).addSkirt(icons_torch, false, bb_stem);
      final IIcon tex = icons_torch[ForgeDirection.UP.ordinal()];
      r.addSingleFace(ForgeDirection.UP, //
          tex.getInterpolatedU(16f * bb_stem.minX), tex.getInterpolatedU(16f * bb_stem.maxX),
          tex.getInterpolatedV(16f * bb_stem.minZ - 1), tex.getInterpolatedV(16f * bb_stem.maxZ - 1), false);
      r.setBB(bb_base).addAllSides(icons_cobble, false, bb_base);

      csr = r.finishDrawing();
    }
  }

  @SubscribeEvent
  public void onTextureReload(TextureStitchEvent.Post event) {
    csr = null;
  }

  public BlockGravityTorchRenderer() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @Override
  public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
      RenderBlocks renderer) {
    setup();

    if (renderer.overrideBlockTexture != null) {
      if (!Loader.isModLoaded("angelica")) {
        // this is not thread-safe, but it also isn't needed as this block breaks
        // instantly
        Tessellator.instance.addTranslation(x, y, z);
        FaceRenderer.setLightingReference(world, CommonProxy.blockGravityTorch, x, y, z);
        FaceRenderer.renderCube(bb_stem, renderer.overrideBlockTexture, xform_stem, null, false);
        FaceRenderer.renderCube(bb_base, renderer.overrideBlockTexture, null, null, false);
        FaceRenderer.clearLightingReference();
        Tessellator.instance.addTranslation(-x, -y, -z);
      }
    } else {
      Tessellator tess = Tessellator.instance;
      tess.addTranslation(x, y, z);
      final RenderingContext renderingContext = new RenderingContext(world, new BlockCoord(x, y, z), tess,
          Config.directDrawingWorld);
      renderingContext.execute(csr, Config.directDrawingWorld);
      tess.addTranslation(-x, -y, -z);
    }

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

    Tessellator tess = Tessellator.instance;
    tess.addTranslation(-.5f, -.5f, -.5f);
    final RenderingContext renderingContext = new RenderingContext(world, new BlockCoord(x, y, z), tess, true);
    // ^ true so the rc will not assume the tess is already drawing
    renderingContext.execute(csr, Config.directDrawingEntity);
    tess.addTranslation(.5f, .5f, .5f); // not actually needed, our caller does glPopMatrix() next. Just be on the safe
                                        // side in case some mod takes over rendering...

    return true;
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
    setup();

    GL11.glPushMatrix();
    float scale = 2.5f;
    float xTrans = -0.5f;
    float yTrans = 0;
    float zTrans = -0.5f;
    boolean directDrawing = Config.directDrawingEntity;
    if (type == ItemRenderType.INVENTORY) {
      yTrans = -0.35f;
      scale = 1.8f;
      directDrawing = Config.directDrawingInventory;
    } else if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
      scale = 1.8f;
      yTrans = 0f;
      zTrans = -0.25f;
      xTrans = -0.25f;
      directDrawing = Config.directDrawingHand;
    }
    GL11.glScalef(scale, scale, scale);
    GL11.glTranslatef(xTrans, yTrans, zTrans);
    RenderUtil.bindBlockTexture();
    new RenderingContext(null, null).execute(csr, directDrawing);

    GL11.glPopMatrix();

  }

}
