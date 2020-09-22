package bqtweaker.handlers;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.Collections;

import betterquesting.api.storage.BQ_Settings;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import bqtweaker.client.gui.GuiQuestOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;

public class TabHandler
{
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void overrideGuiQuest(GuiScreenEvent.InitGuiEvent.Pre event)
	{
		if(!(event.getGui() instanceof GuiQuest)) return;
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
	
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void inventoryInit(GuiScreenEvent.InitGuiEvent.Post event)
    {
    	//Quest Notification Toggle
    	if(event.getGui() instanceof GuiHome) {
    		GuiHome guiHome = (GuiHome)event.getGui();
    		CanvasTextured bgCanvas = (CanvasTextured)guiHome.getChildren().get(0);
        	CanvasEmpty inCanvas = (CanvasEmpty)bgCanvas.getChildren().get(0);
        	PanelButton btnNotif = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_RIGHT, -140, -52, 136, 16, 0), 420, "Null") 
        	{
        		@Override
        		public void onButtonClick()
        		{
        			BQ_Settings.questNotices = !BQ_Settings.questNotices;
        			
        			this.setText(BQ_Settings.questNotices ? "Quest Notifications: On" : "Quest Notifications: Off");
        			
        			ConfigHandler.saveBQNotif();
        		}
        	};
        	if(BQ_Settings.questNotices) btnNotif.setText("Quest Notifications: On");
			else btnNotif.setText("Quest Notifications: Off");
        	btnNotif.setTooltip(Collections.singletonList("Toggle Quest Notifications"));
        	inCanvas.addPanel(btnNotif);
        }
    }
}