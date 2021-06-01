package bqtweaker.client.util;

import java.lang.reflect.Field;

import betterquesting.api2.client.gui.panels.IGuiPanel;
import bq_standard.client.gui.tasks.PanelTaskHunt;
import bq_standard.tasks.TaskHunt;
import bqtweaker.client.gui.panels.PanelTaskHuntOverride;

public class MobendPatcher {

	public static IGuiPanel patchPanel(IGuiPanel panel) {
		try{
			PanelTaskHunt panelHunt = (PanelTaskHunt)panel;
			Class<?> clazz = panelHunt.getClass();
			Field field = clazz.getDeclaredField("task");
			field.setAccessible(true);
			TaskHunt taskHunt = (TaskHunt)field.get(panelHunt);
			
			PanelTaskHuntOverride replacePanel = new PanelTaskHuntOverride(panel.getTransform(), taskHunt);
			
			return replacePanel;
		}
		catch(Exception ex) {
			System.out.println("Patching BQ-Mobends failed");
			return panel;
		}
	}
}
