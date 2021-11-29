package bqtweaker.core.proxies;

import bqtweaker.client.BQTweaker_Keybindings;
import bqtweaker.handlers.GuiHandler;
import net.minecraftforge.common.MinecraftForge;

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
		MinecraftForge.EVENT_BUS.register(new GuiHandler());
		BQTweaker_Keybindings.RegisterKeys();
	}
}
