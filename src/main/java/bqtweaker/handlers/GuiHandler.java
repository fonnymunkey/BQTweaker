package bqtweaker.handlers;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import bqtweaker.client.gui.GuiHomeOverride;
import bqtweaker.client.gui.GuiQuestOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;

public class GuiHandler
{
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void overrideGuiQuest(GuiScreenEvent.InitGuiEvent.Pre event)
	{
		if(event.getGui() instanceof GuiQuest) {
			GuiQuest preGui = (GuiQuest) event.getGui();
			//Haha reflection go brrr
			try {
				Field fQuestID = preGui.getClass().getDeclaredField("questID");
				fQuestID.setAccessible(true);
				int questID = fQuestID.getInt(preGui);
				GuiScreen parent = preGui.parent;
				Minecraft mc = Minecraft.getMinecraft();
				GuiQuestOverride guiQuestOverride = new GuiQuestOverride(parent,questID);
				mc.displayGuiScreen(guiQuestOverride);
			}
			catch(Exception ex) {}
		}
	}
	
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void overrideGuiHome(GuiScreenEvent.InitGuiEvent.Pre event)
    {
    	if(event.getGui() instanceof GuiHome) {
    		GuiHome preGui = (GuiHome) event.getGui();
    		GuiScreen parent = preGui.parent;
    		Minecraft mc = Minecraft.getMinecraft();
    		GuiHomeOverride guiHomeOverride = new GuiHomeOverride(parent);
    		mc.displayGuiScreen(guiHomeOverride);
    	}
    }
}