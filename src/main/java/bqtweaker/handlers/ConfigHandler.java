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

		@Config.Comment("Allow View Mode?")
		@Config.Name("BQ View Mode")
		public boolean bqViewMode = false;

		@Config.Comment("Allow Quest Searching?")
		@Config.Name("BQ Quest Searching")
		public boolean bqQuestSearching = false;

		@Config.Comment("Stop rebinded Back button from going to quest home page while in the questlines page?")
		@Config.Name("Limit Rebinded Back Button")
		public boolean bqLimitBack = false;
	}
	
	@Mod.EventBusSubscriber(modid = BQTweaker.MODID)
	private static class EventHandler {
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(BQTweaker.MODID)) ConfigManager.sync(BQTweaker.MODID, Config.Type.INSTANCE);
		}
	}
}