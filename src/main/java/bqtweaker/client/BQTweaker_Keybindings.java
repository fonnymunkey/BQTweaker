package bqtweaker.client;

import org.lwjgl.input.Keyboard;

import bqtweaker.core.BQTweaker;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BQTweaker_Keybindings {
	
	public static KeyBinding pageBack;
	
	public static void RegisterKeys()
	{
		pageBack = new KeyBinding("bqtweaker.key.pageback", Keyboard.KEY_BACK, BQTweaker.NAME);
		
		ClientRegistry.registerKeyBinding(pageBack);
	}
}
