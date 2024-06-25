package info.loenwind.gravitytorch.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import info.loenwind.gravitytorch.ClientProxy;
import info.loenwind.gravitytorch.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.World;

@Mixin(RenderBlocks.class)
public abstract class IntoRenderBlocks {

  @Inject(method = "renderBlockSandFalling", at = @At("HEAD"), cancellable = true)
  public void onRenderBlockSandFalling(Block block, World world, int x, int y, int z, int meta, CallbackInfo ci) {
    if (block == CommonProxy.blockGravityTorch) {
      // x/y/z as lighting reference. Position (translation) is pre-set by caller to
      // the centre of the entity
      ClientProxy.gravityTorchRenderer.renderFallingBlock(world, x, y, z, (RenderBlocks) (Object) this);
      ci.cancel();
    }
  }

}
