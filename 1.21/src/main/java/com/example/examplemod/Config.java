package com.example.examplemod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = GravityTorch.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    static {
    	BUILDER.push("subsection1");
    	BUILDER
        .comment("Whether to log the dirt block on common setup")
        .define("val1", true);
    	BUILDER
        .comment("Whether to log the dirt block on common setup")
        .define("val2", true);
    	BUILDER
        .comment("Whether to log the dirt block on common setup")
        .define("val3", true);
    	BUILDER.pop();
    	BUILDER
        .comment("Whether to log the dirt block on common setup")
        .define("outer2", true);
    	}
    
    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
        
        
        UnmodifiableConfig values = SPEC.getValues();
        Set<? extends Entry> entrySet = values.entrySet();
        for (Entry entry : entrySet) {
			String key = entry.getKey();
			Object rawValue = entry.getRawValue();
			System.out.println("found cfg value '"+key+"' as raw: "+rawValue);
			if (rawValue instanceof net.neoforged.neoforge.common.ModConfigSpec.ConfigValue cv) {
				System.out.println(" found cfg value '"+key+"' as get: "+cv.get());
			}
		}
        
        // found cfg value 'logDirtBlock' as: net.neoforged.neoforge.common.ModConfigSpec$BooleanValue@5c141c7b
        
        // found cfg value 'magicNumber' as: net.neoforged.neoforge.common.ModConfigSpec$IntValue@14f254f
        
        // found cfg value 'magicNumberIntroduction' as: net.neoforged.neoforge.common.ModConfigSpec$ConfigValue@363ee411
        
        // found cfg value 'items' as: net.neoforged.neoforge.common.ModConfigSpec$ConfigValue@4df54c1

        // net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<T>
        
    }
}
