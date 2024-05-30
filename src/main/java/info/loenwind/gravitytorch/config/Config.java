package info.loenwind.gravitytorch.config;

import java.io.File;

import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import info.loenwind.gravitytorch.GravityTorch;
import net.minecraftforge.common.config.Configuration;

public class Config {

  public static final class Handler {
    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
      if (event.modID.equals(GravityTorch.MODID)) {
        sync();
      }
    }
  }

  private static final Handler handler = new Handler();

  public static boolean directDrawingWorld = true;
  public static boolean directDrawingInventory = true;
  public static boolean directDrawingEntity = true;
  public static boolean directDrawingHand = true;

  static Configuration configuration;

  public static void synchronizeConfiguration(File configFile) {
    configuration = new Configuration(configFile);

    sync();

    FMLCommonHandler.instance().bus().register(handler);
  }

  private static void sync() {
    directDrawingWorld = configuration.getBoolean("directDrawingWorld", Configuration.CATEGORY_GENERAL,
        directDrawingWorld,
        "Enable direct OpenGL drawing for the block in the world? Disable if the block is invisible.");
    directDrawingInventory = configuration.getBoolean("directDrawingInventory", Configuration.CATEGORY_GENERAL,
        directDrawingInventory,
        "Enable direct OpenGL drawing for the item in the inventory? Disable if the item is invisible.");
    directDrawingEntity = configuration.getBoolean("directDrawingEntity", Configuration.CATEGORY_GENERAL,
        directDrawingEntity,
        "Enable direct OpenGL drawing for the falling block? Disable if the falling block entity is invisible.");
    directDrawingHand = configuration.getBoolean("directDrawingHand", Configuration.CATEGORY_GENERAL, directDrawingHand,
        "Enable direct OpenGL drawing for the item in the hand? Disable if the item is invisible.");

    if (Loader.isModLoaded("angelica")) {
      if (directDrawingWorld) {
        GravityTorch.LOG.info("Angelica is present, forcing directDrawingWorld to off");
        directDrawingWorld = false;
      }
    }

    if (configuration.hasChanged()) {
      configuration.save();
    }
  }

}
