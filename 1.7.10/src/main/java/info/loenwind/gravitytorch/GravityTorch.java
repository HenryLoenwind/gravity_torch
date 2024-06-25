package info.loenwind.gravitytorch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = GravityTorch.MODID, version = Tags.VERSION, name = "gravitytorch", acceptedMinecraftVersions = "[1.7.10]", guiFactory = "info.loenwind.gravitytorch.config.ConfigFactory")
public class GravityTorch {

  public static final String MODID = "gravitytorch";
  public static final Logger LOG = LogManager.getLogger(MODID);

  @SidedProxy(clientSide = "info.loenwind.gravitytorch.ClientProxy", serverSide = "info.loenwind.gravitytorch.CommonProxy")
  public static CommonProxy proxy;

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }

  @Mod.EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    proxy.postInit(event);
  }

  @Mod.EventHandler
  public void serverStarting(FMLServerStartingEvent event) {
    proxy.serverStarting(event);
  }
}
