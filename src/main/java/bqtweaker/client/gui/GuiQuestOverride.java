package bqtweaker.client.gui;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.popups.PopContextMenu;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;
import betterquesting.api2.client.gui.resources.textures.SimpleTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.editors.GuiQuestEditor;
import betterquesting.client.gui2.editors.GuiTaskEditor;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import bqtweaker.client.util.MobendPatcher;
import bqtweaker.handlers.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.common.Loader;

import org.lwjgl.util.vector.Vector4f;

@SuppressWarnings("deprecation")
public class GuiQuestOverride extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
	//Credit to Funwayguy for GuiQuest this is based on
    private final int questID;
    
    private IQuest quest;
    
    private PanelButton btnDetect;
    private PanelButton btnClaim;
    
    private CanvasScrolling cvDesc;
    private PanelVScrollBar paDescScroll;
    
    private CanvasEmpty cvInner;
    
    private IGuiRect rectReward;
    private IGuiRect rectTask;
    
    private CanvasScrolling tskScroll;
    private PanelVScrollBar tskScrollBar;
    
    public GuiQuestOverride(GuiScreen parent, int questID)
    {
        super(parent);
        this.questID = questID;
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        this.quest = QuestDatabase.INSTANCE.getValue(questID);
        
        if(quest == null)
        {
            mc.displayGuiScreen(this.parent);
            return;
        }
    
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
    
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate(quest.getProperty(NativeProps.NAME))).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        
        if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player))
        {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 100, 16, 0), 0, QuestTranslation.translate("gui.back")));
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -16, 100, 16, 0), 1, QuestTranslation.translate("betterquesting.btn.edit")));
        } else
        {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        }
        
        cvInner = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(16, 32, 16, 24), 0));
        cvBackground.addPanel(cvInner);
        
        //Reward and Desc scrolling
        if(quest.getRewards().size() > 0) {
        	btnClaim = new PanelButton(new GuiTransform(new Vector4f(0F, 1F, 0.5F, 1F), new GuiPadding(16, -16, 24, 0), 0), 6, QuestTranslation.translate("betterquesting.btn.claim"));
            btnClaim.setActive(false);
            cvInner.addPanel(btnClaim);
        }
        
        rectReward = new GuiTransform(new Vector4f(0F, 0.5F, 0.5F, 1F), new GuiPadding(0, 0, 16, 16), 0);
        rectReward.setParent(cvInner.getTransform());
        
        //Handle Reward/Desc logic
        refreshRewardDescPanel();
        
        
        //Task Scrolling
        btnDetect = new PanelButton(new GuiTransform(new Vector4f(0.5F, 1F, 1F, 1F), new GuiPadding(24, -16, 16, 0), 0), 7, QuestTranslation.translate("betterquesting.btn.detect_submit"));
        btnDetect.setActive(false);
        cvInner.addPanel(btnDetect);
        
        rectTask = new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 0, 0, 18), 0);
        rectTask.setParent(cvInner.getTransform());
        
        CanvasEmpty cvTaskPopup = new CanvasEmpty(rectTask){
        	@Override
        	public boolean onMouseClick(int mx, int my, int click) {
        		if(click != 1) return false;
        		if(!(rectTask.getX() < mx && rectTask.getX()+rectTask.getWidth() > mx && rectTask.getY() < my && rectTask.getY() + rectTask.getHeight() > my)) return false;
            	if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player)){
            		PopContextMenu popup = new PopContextMenu(new GuiRectangle(mx, my, 64, 16), true);
            		GuiTaskEditor editor = new GuiTaskEditor(new GuiQuestOverride(parent,questID), quest);
            		Runnable action = new Runnable() {
            			@Override
            			public void run(){
            				mc.displayGuiScreen(editor);
            			}
            		};
            		popup.addButton(QuestTranslation.translate("bqtweaker.context.task"), null, action);
            		openPopup(popup);
            		return true;
            	}
            	else return false;
            }
        };
        cvInner.addPanel(cvTaskPopup);
        
        //Handle task logic
        refreshTaskPanel();
        
        
        IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 0, 0, 0, 0);
        ls0.setParent(cvInner.getTransform());
        IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, 0, 0, 0, 0);
        le0.setParent(cvInner.getTransform());
        PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
        cvInner.addPanel(paLine0);
    }
    
    @Override
    public void refreshGui()
    {
        this.refreshTaskPanel();
        this.refreshRewardDescPanel();
        this.updateButtons();
    }
    
    @Override
    public boolean onMouseClick(int mx, int my, int click)
    {
        if(super.onMouseClick(mx, my, click))
        {
            this.updateButtons();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onMouseScroll(int mx, int my, int scroll)
    {
        if(super.onMouseScroll(mx, my, scroll))
        {
            this.updateButtons();
            return true;
        }
    
        return false;
    }
    
    @Override
    public boolean onKeyTyped(char c, int keycode)
    {
        if(super.onKeyTyped(c, keycode))
        {
            this.updateButtons();
            return true;
        }
        
        return false;
    }
    
    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }
    
    private void onButtonPress(PEventButton event)
    {
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 0) // Exit
        {
            mc.displayGuiScreen(this.parent);
        } else if(btn.getButtonID() == 1) // Edit
        {
            mc.displayGuiScreen(new GuiQuestEditor(this, questID));
        } else if(btn.getButtonID() == 6) // Reward claim
        {
            NetQuestAction.requestClaim(new int[]{questID});
        } else if(btn.getButtonID() == 7) // Task detect/submit
        {
            NetQuestAction.requestDetect(new int[]{questID});
        }
    }
    
    private void refreshRewardDescPanel()
    {
    	if(cvDesc != null) {
    		cvInner.removePanel(cvDesc);
    		cvInner.removePanel(paDescScroll);
    	}
    	
        cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(0, 0, 16, 18), 0));
        cvInner.addPanel(cvDesc);
        
        String desc = QuestTranslation.translate(quest.getProperty(NativeProps.DESC));
        int panelPrevY = 0;
        int panelPrevHeight = 0;
        
        if(desc.contains("{Embed}")) {
        	String[] descSplit = desc.split("\\{Embed\\}");
        	for(String split : descSplit) {
        		if(split.startsWith("TypeImage") || split.startsWith("TypeLink")) {
        			try {
            			String[] embedEntry = split.split(";");
            			int width = Math.min(cvDesc.getTransform().getWidth(), Integer.parseInt(embedEntry[2]));
            			int height = Integer.parseInt(embedEntry[3]);
            			
            			if(embedEntry[0].contentEquals("TypeImage")) {
            	        	IGuiTexture embedImage = new SimpleTexture(new ResourceLocation(embedEntry[1]), new GuiRectangle(0, 0, Integer.parseInt(embedEntry[4]), Integer.parseInt(embedEntry[5])));
            	        	cvDesc.addCulledPanel(new PanelGeneric(new GuiRectangle((cvDesc.getTransform().getWidth()-width)/2, panelPrevY+panelPrevHeight+8, width, height), embedImage), true);
            			}
            			else if(embedEntry[0].contentEquals("TypeLink")) {
            	        	PanelButton btnLink = new PanelButton(new GuiRectangle((cvDesc.getTransform().getWidth()-width)/2, panelPrevY+panelPrevHeight+8, width, height), 69, embedEntry[4]);
            	        	btnLink.setClickAction((b) -> {
            	        		TextComponentString component = new TextComponentString(embedEntry[1]);
            	            	component.setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, embedEntry[1])));
            	            	this.handleComponentClick(component);
            	        	});
            	        	btnLink.setTextAlignment(1);
            	        	cvDesc.addCulledPanel(btnLink, true);
            			}
            			
        	        	panelPrevY = panelPrevY+panelPrevHeight+8;
                        panelPrevHeight = height;
        			}
        			catch(Exception ex) {}
        		}
        		else {
        			PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, panelPrevY+panelPrevHeight+12, cvDesc.getTransform().getWidth(), 0), ConfigHandler.client.bqTrimDescriptions ? split.trim() : split, true);
                	paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
                    cvDesc.addCulledPanel(paDesc, true);
                    
                    panelPrevY = panelPrevY+panelPrevHeight+12;
                    panelPrevHeight = paDesc.getTransform().getHeight();
        		}
        	}
        	panelPrevY += 32;
        }
        else {
        	PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), desc, true);
        	paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
            cvDesc.addCulledPanel(paDesc, true);
            panelPrevY = 32;
            panelPrevHeight = paDesc.getTransform().getHeight()+8;
        }
        
        paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.BOTTOM_CENTER), new GuiPadding(-16, 0, 8, 18), 0));
        cvInner.addPanel(paDescScroll);
        cvDesc.setScrollDriverY(paDescScroll);
        paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);

        if(quest.getRewards().size()<=0)
        {
            updateButtons();
            return;
        }
        
        for(int index = 0; index<quest.getRewards().size(); index++) {
        	IReward rew = quest.getRewards().getEntries().get(index).getValue();
        	PanelTextBox rwdTitle = new PanelTextBox(new GuiRectangle(0, panelPrevY + panelPrevHeight, cvDesc.getTransform().getWidth(), 16, 0), QuestTranslation.translate(rew.getUnlocalisedName())).setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
        	IGuiPanel pnReward = rew.getRewardGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, cvDesc.getTransform().getWidth(), 32, 0), new DBEntry<>(questID, quest));
        	
            cvDesc.addCulledPanel(rwdTitle, true);
            cvDesc.addCulledPanel(pnReward, true);
            
            int height = rectReward.getHeight()-32;
            
            CanvasEmpty pnRewardCanvas = (CanvasEmpty)pnReward;
            for(IGuiPanel pnl : pnRewardCanvas.getChildren()) {
            	if(pnl.getClass().isAssignableFrom(CanvasScrolling.class)) {
            		CanvasScrolling pnlScrolling = (CanvasScrolling)pnl;
                	height = pnlScrolling.getScrollBounds().getHeight() + 32;
            	}
            }
        	
            IGuiPanel pnRewardReplacement = rew.getRewardGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, rectReward.getWidth(), height, 0), new DBEntry<>(questID, quest));
            cvDesc.removePanel(pnReward);
            cvDesc.addCulledPanel(pnRewardReplacement, true);
            
            CanvasEmpty pnRewardReplacementCanvas = (CanvasEmpty)pnRewardReplacement;
        	for(IGuiPanel pnlRepl : pnRewardReplacementCanvas.getChildren()) {
        		if(pnlRepl.getClass().isAssignableFrom(PanelVScrollBar.class)) {
        			pnRewardReplacementCanvas.removePanel(pnlRepl);
        		}
        	}
        	
        	panelPrevY = panelPrevY + panelPrevHeight + 16;
        	panelPrevHeight = height+8;
        }
        
        paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
        
        updateButtons();
    }
    
    private void refreshTaskPanel()
    {
        if(tskScroll != null)
        {
            cvInner.removePanel(tskScroll);
            cvInner.removePanel(tskScrollBar);
        }
        
        if(quest.getTasks().size()<=0) {
        	updateButtons();
        	return;
        }
        
        tskScroll = new CanvasScrolling(rectTask);
        cvInner.addPanel(tskScroll);
        tskScrollBar = new PanelVScrollBar(new GuiRectangle(cvInner.getTransform().getWidth()-8, 0, 8, rectTask.getHeight(), 0));
        cvInner.addPanel(tskScrollBar);
        tskScroll.setScrollDriverY(tskScrollBar);
        
        int panelPrevY = 0;
        int panelPrevHeight = 0;
        
        for(int index = 0; index<quest.getTasks().size(); index++) {
        	ITask tsk = quest.getTasks().getEntries().get(index).getValue();
        	PanelTextBox tskTitle = new PanelTextBox(new GuiRectangle(0, panelPrevY + panelPrevHeight, rectTask.getWidth(), 16, 0), QuestTranslation.translate(tsk.getUnlocalisedName())).setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
        	IGuiPanel pnTask = tsk.getTaskGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, rectTask.getWidth(), 32, 0), new DBEntry<>(questID, quest));
        	
        	tskScroll.addCulledPanel(tskTitle, true);
        	tskScroll.addCulledPanel(pnTask, true);
        	
        	int height = rectTask.getHeight()-16;//Fallback height if panel does not have scrolling canvas
        	
        	//Need to get panel *after* adding it, to get info on size required
        	if(pnTask instanceof CanvasEmpty) {
            	CanvasEmpty pnTaskCanvas = (CanvasEmpty)pnTask;
            	for(IGuiPanel pnl : pnTaskCanvas.getChildren()) {
            		if(pnl.getClass().isAssignableFrom(CanvasScrolling.class)) {
            			CanvasScrolling pnlScrolling = (CanvasScrolling)pnl;
            			height = pnlScrolling.getScrollBounds().getHeight() + 32;//Set required height of panel
            		}
            	}
        	}
        	
        	//Create new panel using modified height
        	IGuiPanel pnTaskReplacement = tsk.getTaskGui(new GuiRectangle(0, panelPrevY + panelPrevHeight + 16, rectTask.getWidth(), height, 0), new DBEntry<>(questID, quest));
        	tskScroll.removePanel(pnTask);
        	
        	if(pnTaskReplacement.getClass().getName().contentEquals("bq_standard.client.gui.tasks.PanelTaskHunt") && Loader.isModLoaded("mobends") && ConfigHandler.client.bqMobendPatch) {
        		pnTaskReplacement = MobendPatcher.patchPanel(pnTaskReplacement);
        	}
        	
        	tskScroll.addCulledPanel(pnTaskReplacement, true);
        	
        	//Remove scrollbar from modified panel
        	if(pnTaskReplacement instanceof CanvasEmpty) {
            	CanvasEmpty pnTaskReplacementCanvas = (CanvasEmpty)pnTaskReplacement;
            	for(IGuiPanel pnlRepl : pnTaskReplacementCanvas.getChildren()) {
            		if(pnlRepl.getClass().isAssignableFrom(PanelVScrollBar.class)) {
            			pnTaskReplacementCanvas.removePanel(pnlRepl);
            		}
            	}
        	}
        	
        	//Set size information for if there is >1 task
        	panelPrevY = panelPrevY + panelPrevHeight + 16;
        	panelPrevHeight = height+8;//+8 for padding
        }
        
        tskScrollBar.setEnabled(tskScroll.getScrollBounds().getHeight() > 0);
        
        updateButtons();
    }
    
    private void updateButtons()
    {
        Minecraft mc = Minecraft.getMinecraft();
        
        if(btnClaim != null)
        {
            // Claim button state
            btnClaim.setActive(quest.getRewards().size() > 0 && quest.canClaim(mc.player));
        }
        
        if(btnDetect != null)
        {
            // Detect/submit button state
            btnDetect.setActive(quest.canSubmit(mc.player));
        }
    }
}
