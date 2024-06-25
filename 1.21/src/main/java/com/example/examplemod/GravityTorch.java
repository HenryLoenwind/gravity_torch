package com.example.examplemod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GravityTorch.MODID)
public class GravityTorch {
    public static final String MODID = "gravitytorch";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<Block> DA_BLOCK = BLOCKS.register("mounted_torch", GravityTorchBlock::create);
    public static final DeferredItem<BlockItem> DA_ITEM = ITEMS.registerSimpleBlockItem("mounted_torch", DA_BLOCK);

    public GravityTorch(IEventBus modEventBus, ModContainer modContainer) {
    	modContainer.registerExtensionPoint(IConfigScreenFactory.class, (a, b) -> new ConfigurationScreen(modContainer, a, b));

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(DA_ITEM);
        }
    }

}
