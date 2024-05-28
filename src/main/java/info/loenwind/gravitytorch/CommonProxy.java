package info.loenwind.gravitytorch;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import info.loenwind.gravitytorch.block.BlockGravityTorch;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class CommonProxy {

  public static Block blockGravityTorch;

  public void preInit(FMLPreInitializationEvent event) {
    Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

    blockGravityTorch = new BlockGravityTorch();
    GameRegistry.registerBlock(blockGravityTorch, ItemBlock.class, "gravity_torch");
  }

  public void init(FMLInitializationEvent event) {
    GameRegistry.addRecipe(new ShapedOreRecipe(blockGravityTorch, "t", "c", 't', Blocks.torch, 'c', "cobblestone"));
  }

  public void postInit(FMLPostInitializationEvent event) {
  }

  public void serverStarting(FMLServerStartingEvent event) {
  }

}
