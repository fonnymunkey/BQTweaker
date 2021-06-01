package bqtweaker.core.proxies;

import bqtweaker.client.BQTweaker_Keybindings;
import bqtweaker.handlers.ConfigHandler;
import bqtweaker.handlers.GuiHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy
{
	@Override
	public boolean isClient()
	{
		return true;
	}
	
	@Override
	public void registerHandlers()
	{
		MinecraftForge.EVENT_BUS.register(GuiHandler.class);
		BQTweaker_Keybindings.RegisterKeys();
	}
	
	@Override
	public void registerConfig(FMLPreInitializationEvent event)
	{
		ConfigHandler.initConfig(event.getSuggestedConfigurationFile());
	}
}
