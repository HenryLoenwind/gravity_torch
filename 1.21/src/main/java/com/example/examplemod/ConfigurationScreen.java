package com.example.examplemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class ConfigurationScreen extends OptionsSubScreen {

	private static final Component TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE = Component.translatable("configuration.notonline");
	
	private final ModContainer mod;
	private boolean needsRestart = false;

	public ConfigurationScreen(ModContainer mod, Minecraft mc, Screen parent) {
		super(parent, mc.options, Component.translatable(mod.getModId() + ".configuration.title"));
		this.mod = mod;
	}

	@Override
	protected void addOptions() {
		EnumMap<ModConfig.Type, ModConfig> configs = findConfigs();

		for (Type type : ModConfig.Type.values()) {
			if (configs.containsKey(type)) {
				Button btn = Button.builder(Component.translatable("configuration."+type.name().toLowerCase()), button -> minecraft.setScreen(new ConfigurationSectionScreen(mod, minecraft, this, type, configs.get(type)))).build();
				if ((type == Type.SERVER || type == Type.COMMON) && minecraft.getCurrentServer() != null && !minecraft.isSingleplayer()) {
					btn.setTooltip(Tooltip.create(TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE));
					btn.active = false;
				}
				list.addSmall(btn, null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private EnumMap<ModConfig.Type, ModConfig> findConfigs() {
		Field field = ObfuscationReflectionHelper.findField(net.neoforged.fml.ModContainer.class, "configs"); // Really? No getter?
		EnumMap<ModConfig.Type, ModConfig> configs = null;
		try {
			configs = (EnumMap<Type, ModConfig>) field.get(mod);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return configs != null ? configs : new EnumMap<>(ModConfig.Type.class);
	}

	@Override
	public void onClose() {
		if (needsRestart) {
			System.out.println("TODO");
		}
		super.onClose();
	}

	protected static class ConfigurationSectionScreen extends OptionsSubScreen {

		private final ModContainer mod;
		private boolean changed = false;
		private boolean needsRestart = false;
		private final Set<? extends Entry> entrySet;
		private final UnmodifiableConfig commentConfig;

		public ConfigurationSectionScreen(ModContainer mod, Minecraft mc, Screen parent, ModConfig.Type type, ModConfig modConfig) {
			this(mod, mc, parent, type, (UnmodifiableConfig) modConfig.getSpec(), ((ModConfigSpec) modConfig.getSpec()).getValues().entrySet(), Component.translatable(mod.getModId() + ".configuration." + type.name().toLowerCase() + ".title"));
		}

		public ConfigurationSectionScreen(ModContainer mod, Minecraft mc, Screen parent, UnmodifiableConfig commentConfig, String key, Set<? extends Entry> entrySet) {
			this(mod, mc, parent, null, commentConfig, entrySet, Component.translatable(mod.getModId() + ".configuration." + key + ".title"));
		}

		public ConfigurationSectionScreen(ModContainer mod, Minecraft mc, Screen parent, ModConfig.Type type, UnmodifiableConfig commentConfig, Set<? extends Entry> entrySet, Component title) {
			super(parent, mc.options, title);
			this.mod = mod;
			this.entrySet = entrySet;
			this.commentConfig = commentConfig;
		}

		private String getTranslationKey(String key) {
			String result = null;
			Map<String,Object> valueMap = commentConfig.valueMap();
			Object object = valueMap.get(key);
			if (object instanceof ValueSpec vs) {
				result = vs.getTranslationKey();
			}
			return result != null ? result : mod.getModId() + ".configuration." + key;
		}

		private <T> OptionInstance.TooltipSupplier<T> getTooltip(String key) {
			String result = null;
			Map<String,Object> valueMap = commentConfig.valueMap();
			Object object = valueMap.get(key);
			if (object instanceof ValueSpec vs) {
				result = vs.getComment();
			}
			return result != null ? OptionInstance.cachedConstantTooltip(Component.literal(result)) : OptionInstance.noTooltip();
		}

		private void onChanged(String key) {
			changed = true;
			Map<String,Object> valueMap = commentConfig.valueMap();
			Object object = valueMap.get(key);
			if (object instanceof ValueSpec vs) {
				needsRestart |= vs.needsWorldRestart();
			}
		}

		@Override
		protected void addOptions() {
			List<OptionInstance<?>> options = new ArrayList<>();
			List<AbstractWidget> buttons = new ArrayList<>();
			for(Entry entry : entrySet) {
				String key = entry.getKey();
				Object rawValue = entry.getRawValue();
				if (rawValue instanceof ModConfigSpec.ConfigValue cv) {
					if (cv instanceof ModConfigSpec.BooleanValue boolvalue) {
						options.add(OptionInstance.createBoolean(getTranslationKey(key), getTooltip(key), boolvalue.get(), newVal -> { boolvalue.set(newVal); onChanged(key); }));
					} else {
						System.out.println("Unknown ConfigValue: " + rawValue);
					}
				} else if (rawValue instanceof UnmodifiableConfig subsection) {
					Map<String,Object> valueMap = commentConfig.valueMap();
					Object object = valueMap.get(key);
					if (object instanceof UnmodifiableConfig) {
						buttons.add(Button.builder(Component.translatable(getTranslationKey(key)), button -> minecraft.setScreen(new ConfigurationSectionScreen(mod, minecraft, this, (UnmodifiableConfig) object, key, subsection.entrySet()))).build());
					}
				} else {
					System.out.println("Unknown config element: " + rawValue);
				}
			}
			if (!buttons.isEmpty()) {
				list.addSmall(buttons);
			}
			if (!options.isEmpty()) {
				list.addSmall(options.toArray(new OptionInstance<?>[options.size()]));
			}
		}

		@Override
		public void onClose() {
			if (changed) {
				if (lastScreen instanceof ConfigurationSectionScreen parent) {
					parent.changed = true;
				} else if (commentConfig instanceof ModConfig cfg) {
					cfg.save();
				}
			}
			if (needsRestart) {
				if (lastScreen instanceof ConfigurationSectionScreen parent) {
					parent.needsRestart = true;
				} else if (lastScreen instanceof ConfigurationScreen parent) {
					parent.needsRestart = true;
				}
			}
			super.onClose();
		}

	}

}
