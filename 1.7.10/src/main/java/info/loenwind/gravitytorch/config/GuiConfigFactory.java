package info.loenwind.gravitytorch.config;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import info.loenwind.gravitytorch.GravityTorch;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class GuiConfigFactory extends GuiConfig {

  public GuiConfigFactory(GuiScreen parentScreen) {
    super(parentScreen, getConfigElements(parentScreen), GravityTorch.MODID, GravityTorch.MODID + "-config", false,
        false, "Gravity Torch");
  }

  @SuppressWarnings("rawtypes")
  private static List<IConfigElement> getConfigElements(GuiScreen parent) {
    List<IConfigElement> list = new ArrayList<IConfigElement>();

    list.add(new ConfigElement(Config.configuration.getCategory(Configuration.CATEGORY_GENERAL)));

    return list;
  }
}