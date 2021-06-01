package bqtweaker.handlers;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import betterquesting.api.storage.BQ_Settings;

public class ConfigHandler {

	public static Configuration config;
	
	public static boolean bqNotifEnabled = true;
	public static boolean bqMobendPatch = false;
	
	public static boolean bqHomeOverride = true;
	public static boolean bqQuestOverride = true;
	public static boolean bqQuestlineOverride = true;
	
	public static void initConfig(File configFile)
	{
		config = new Configuration(configFile);
		
		bqNotifEnabled = config.getBoolean("bqNotifEnabled", "Notifications", bqNotifEnabled, "Enable BetterQuesting quest notifications?");
		bqMobendPatch = config.getBoolean("bqMobendPatch", "Patches", bqMobendPatch, "Enable Mobend-BQ animation bug patch?");
		
		bqHomeOverride = config.getBoolean("bqHomeOverride", "Overrides", bqHomeOverride, "Override BQ's home page? (Quest Notification button)");
		bqQuestOverride = config.getBoolean("bqQuestOverride", "Overrides", bqQuestOverride, "Override BQ's quest page? (Task/Desc/Reward wrapping and scrolling, image/link embedding, right-click add task)");
		bqQuestlineOverride = config.getBoolean("bqQuestlineOverride", "Overrides", bqQuestlineOverride, "Override BQ's questline page? (Questline page redesign, right-click to edit)");
		
		if(config.hasChanged()) {
			config.save();
		}
	}
	
	public static void loadBQNotif() {
		BQ_Settings.questNotices = bqNotifEnabled;
	}
	
	public static void saveBQNotif() {
		config.get("Notifications", "bqNotifEnabled", bqNotifEnabled, "Enable BetterQuesting quest notifications?").set(BQ_Settings.questNotices);
		config.save();
		config.load();
	}
}