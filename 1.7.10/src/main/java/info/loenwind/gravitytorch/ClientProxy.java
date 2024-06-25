package info.loenwind.gravitytorch;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import info.loenwind.gravitytorch.block.BlockGravityTorch;
import info.loenwind.gravitytorch.renderer.BlockGravityTorchRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy {

  static public BlockGravityTorchRenderer gravityTorchRenderer;

  @Override
  public void init(FMLInitializationEvent event) {
    super.init(event);

    BlockGravityTorch.renderId = RenderingRegistry.getNextAvailableRenderId();
    gravityTorchRenderer = new BlockGravityTorchRenderer();
    RenderingRegistry.registerBlockHandler(BlockGravityTorch.renderId, gravityTorchRenderer);
    MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(CommonProxy.blockGravityTorch),
        gravityTorchRenderer);
  }

}
