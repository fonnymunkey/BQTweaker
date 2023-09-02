package bqtweaker.handlers;

import betterquesting.client.gui2.GuiHome;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.ListIterator;

import org.lwjgl.input.Keyboard;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import bqtweaker.client.BQTweaker_Keybindings;
import bqtweaker.client.gui.GuiQuestLinesOverride;
import bqtweaker.client.gui.GuiQuestOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.GuiScreenEvent;

public class GuiHandler {
	private static Field fQuestID;

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void overrideGuiQuest(GuiScreenEvent.InitGuiEvent.Pre event) {
		if(event.getGui() instanceof GuiQuest && ConfigHandler.client.bqQuestOverride) {
			GuiQuest preGui = (GuiQuest)event.getGui();
			try {
				if(fQuestID == null) {
					fQuestID = preGui.getClass().getDeclaredField("questID");
					fQuestID.setAccessible(true);
				}
				GuiQuestOverride guiQuestOverride = new GuiQuestOverride(preGui.parent, fQuestID.getInt(preGui));
				Minecraft.getMinecraft().displayGuiScreen(guiQuestOverride);
			}
			catch(Exception ignored) {}
		}
    
    	if(event.getGui() instanceof GuiQuestLines && ConfigHandler.client.bqQuestlineOverride) {
    		GuiQuestLines preGui = (GuiQuestLines)event.getGui();
    		GuiQuestLinesOverride guiQuestLinesOverride = new GuiQuestLinesOverride(preGui.parent);
			Minecraft.getMinecraft().displayGuiScreen(guiQuestLinesOverride);
    	}
    }
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.currentScreen instanceof GuiScreenCanvas && GameSettings.isKeyDown(BQTweaker_Keybindings.pageBack)) {
			boolean keyUsed = false;
			GuiScreenCanvas currScreenCanvas = (GuiScreenCanvas)mc.currentScreen;
			if(currScreenCanvas.parent == null) return;
			if(currScreenCanvas.parent instanceof GuiHome && (currScreenCanvas instanceof GuiQuestLinesOverride || currScreenCanvas instanceof GuiQuestLines) && ConfigHandler.client.bqLimitBack) return;
			try {
				ListIterator<IGuiPanel> pnIter = currScreenCanvas.getChildren().listIterator(currScreenCanvas.getChildren().size());
				while(pnIter.hasPrevious()) {
					IGuiPanel entry = pnIter.previous();
					if(entry.isEnabled() && entry.onKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey())) {
						keyUsed = true;
						break;
					}
				}
				event.setCanceled(true);
				if(!keyUsed) mc.displayGuiScreen(currScreenCanvas.parent);
			}
			catch(Exception ignored) {}
		}
	}
}