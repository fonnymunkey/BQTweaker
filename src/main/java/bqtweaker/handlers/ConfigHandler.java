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
	
	public static class ClientConfig{
		
		@Config.Comment("Should the button for toggling notifications be enabled?")
		@Config.Name("BQ Notifications Button")
		public boolean bqNotifEnabled= true;
		
		@Config.Comment("Should BQTweaker attempt to patch an entity rendering bug when Mobends is active? (Disabling this disables Party Create and Party Manage overrides as well.)")
		@Config.Name("BQ Mobends Patch")
		public boolean bqMobendPatch= true;
		
		@Config.Comment("Should BQTweaker automatically trim leading and trailing whitespace from quest descriptions? (Can help make text cleaner, turn off if you'd rather handle whitespace yourself.)")
		@Config.Name("BQ Trim Descriptions")
		public boolean bqTrimDescriptions= true;
		
		@Config.Comment("Override BQ's Home page?")
		@Config.Name("BQ Home Override")
		public boolean bqHomeOverride= true;
		
		@Config.Comment("Override BQ's Party Create page?")
		@Config.Name("BQ Party Create Override")
		public boolean bqPartyCreateOverride= true;
		
		@Config.Comment("Override BQ's Party Manage page?")
		@Config.Name("BQ Party Manage Override")
		public boolean bqPartyManageOverride= true;
		
		@Config.Comment("Override BQ's Quest page?")
		@Config.Name("BQ Quest Override")
		public boolean bqQuestOverride= true;
		
		@Config.Comment("Override BQ's Questline page?")
		@Config.Name("BQ Questline Override")
		public boolean bqQuestlineOverride= true;
	}
	
	@Mod.EventBusSubscriber(modid = BQTweaker.MODID)
	private static class EventHandler{
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(BQTweaker.MODID)) ConfigManager.sync(BQTweaker.MODID, Config.Type.INSTANCE);
		}
	}
}