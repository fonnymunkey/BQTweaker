package bqtweaker.handlers;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import bqtweaker.core.BQTweaker;

@Config(modid = BQTweaker.MODID)
public class ConfigHandler {
	
	@Config.Comment("Client Config")
	@Config.Name("Client")
	public static final ClientConfig client = new ClientConfig();
	
	public static class ClientConfig {
		
		@Config.Comment("Override BQ's Quest page?")
		@Config.Name("BQ Quest Override")
		public boolean bqQuestOverride= true;
		
		@Config.Comment("Override BQ's Questline page?")
		@Config.Name("BQ Questline Override")
		public boolean bqQuestlineOverride= true;
	}
	
	@Mod.EventBusSubscriber(modid = BQTweaker.MODID)
	private static class EventHandler {
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(BQTweaker.MODID)) ConfigManager.sync(BQTweaker.MODID, Config.Type.INSTANCE);
		}
	}
}