package bqtweaker.handlers;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.ListIterator;

import org.lwjgl.input.Keyboard;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.client.gui2.party.GuiPartyCreate;
import betterquesting.client.gui2.party.GuiPartyManage;
import bqtweaker.client.BQTweaker_Keybindings;
import bqtweaker.client.gui.GuiHomeOverride;
import bqtweaker.client.gui.GuiPartyCreateOverride;
import bqtweaker.client.gui.GuiPartyManageOverride;
import bqtweaker.client.gui.GuiQuestLinesOverride;
import bqtweaker.client.gui.GuiQuestOverride;
import bqtweaker.handlers.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.GuiScreenEvent;

public class GuiHandler
{
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void overrideGuiQuest(GuiScreenEvent.InitGuiEvent.Pre event)
	{
		if(event.getGui() instanceof GuiQuest && ConfigHandler.bqQuestOverride) {
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
	
    	if(event.getGui() instanceof GuiHome && ConfigHandler.bqHomeOverride) {
    		GuiHome preGui = (GuiHome) event.getGui();
    		GuiScreen parent = preGui.parent;
    		Minecraft mc = Minecraft.getMinecraft();
    		GuiHomeOverride guiHomeOverride = new GuiHomeOverride(parent);
    		mc.displayGuiScreen(guiHomeOverride);
    	}
    
    	if(event.getGui() instanceof GuiQuestLines && ConfigHandler.bqQuestlineOverride) {
    		GuiQuestLines preGui = (GuiQuestLines) event.getGui();
    		GuiScreen parent = preGui.parent;
    		Minecraft mc = Minecraft.getMinecraft();
    		GuiQuestLinesOverride guiQuestLinesOverride = new GuiQuestLinesOverride(parent);
    		mc.displayGuiScreen(guiQuestLinesOverride);
    	}
    	
    	if(event.getGui() instanceof GuiPartyCreate && ConfigHandler.bqMobendPatch) {
    		GuiPartyCreate preGui = (GuiPartyCreate) event.getGui();
    		GuiScreen parent = preGui.parent;
    		Minecraft mc = Minecraft.getMinecraft();
    		GuiPartyCreateOverride guiPartyCreateOverride = new GuiPartyCreateOverride(parent);
    		mc.displayGuiScreen(guiPartyCreateOverride);
    	}
    	
    	if(event.getGui() instanceof GuiPartyManage && ConfigHandler.bqMobendPatch) {
    		GuiPartyManage preGui = (GuiPartyManage) event.getGui();
    		GuiScreen parent = preGui.parent;
    		Minecraft mc = Minecraft.getMinecraft();
    		GuiPartyManageOverride guiPartyManageOverride = new GuiPartyManageOverride(parent);
    		mc.displayGuiScreen(guiPartyManageOverride);
    	}
    }
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onKeyTyped(GuiScreenEvent.KeyboardInputEvent.Pre event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.currentScreen instanceof GuiScreenCanvas && GameSettings.isKeyDown(BQTweaker_Keybindings.pageBack))
		{
			boolean keyUsed = false;
			GuiScreenCanvas currScreenCanvas = (GuiScreenCanvas)mc.currentScreen;
			if(currScreenCanvas.parent == null) return;
			try {
				ListIterator<IGuiPanel> pnIter = currScreenCanvas.getChildren().listIterator(currScreenCanvas.getChildren().size());
				
				while(pnIter.hasPrevious())
				{
					IGuiPanel entry = pnIter.previous();
					if(entry.isEnabled() && entry.onKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey()))
					{
						keyUsed = true;
						break;
					}
				}
				if(keyUsed) event.setCanceled(true);
				else {
					mc.displayGuiScreen(currScreenCanvas.parent);
				}
			}
			catch(Exception ex) {
				System.out.println("error");
			}
		}
	}
}