package bqtweaker.handlers;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import betterquesting.api.storage.BQ_Settings;

public class ConfigHandler {

	public static Configuration config;
	
	public static boolean bqNotifEnabled = true;
	
	public static void initConfig(File configFile)
	{
		config = new Configuration(configFile);
		
		bqNotifEnabled = config.getBoolean("bqNotifEnabled", "Notifications", bqNotifEnabled, "Enable BetterQuesting quest notifications?");

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
	}
}
