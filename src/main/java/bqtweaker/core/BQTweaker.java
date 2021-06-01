package bqtweaker.core;

import org.apache.logging.log4j.Logger;

import bqtweaker.core.proxies.CommonProxy;
import bqtweaker.handlers.ConfigHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = BQTweaker.MODID, version = BQTweaker.VERSION, name = BQTweaker.NAME, dependencies = "after:betterquesting")
public class BQTweaker
{
    public static final String MODID = "bqtweaker";
    public static final String VERSION = "1.2.0";
    public static final String NAME = "BQTweaker";
    public static final String PROXY = "bqtweaker.core.proxies";
    public static final String CHANNEL = "BQTWEAKER";
	
	@Instance(MODID)
	public static BQTweaker instance;
	
	@SidedProxy(clientSide = PROXY + ".ClientProxy", serverSide = PROXY + ".CommonProxy")
	public static CommonProxy proxy;
	public SimpleNetworkWrapper network;
	public static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	logger = event.getModLog();
    	network = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);  
    	proxy.registerConfig(event);
    	if(ConfigHandler.bqHomeOverride) ConfigHandler.loadBQNotif();
    }
    
    @SuppressWarnings("unused")
	@EventHandler
    public void init(FMLInitializationEvent event)
    {
        ModContainer modContainer = Loader.instance().getIndexedModList().get("bqtweaker");
        if(modContainer != null && modContainer.getMod() instanceof BQTweaker)
        {
            BQTweaker modInstance = (BQTweaker)modContainer.getMod();
            // DO THINGS...
        }
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
    	if(Loader.isModLoaded("betterquesting")) { proxy.registerHandlers(); }
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
    }
}
