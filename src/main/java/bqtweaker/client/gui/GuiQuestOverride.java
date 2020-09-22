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
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.resources.colors.GuiColorPulse;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.editors.GuiQuestEditor;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.util.vector.Vector4f;

public class GuiQuestOverride extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
	//Credit to Funwayguy for GuiQuest this is based on
    private final int questID;
    
    private IQuest quest;
    
    private PanelButton btnRewardLeft;
    private PanelButton btnRewardRight;
    
    private PanelButton btnDetect;
    private PanelButton btnClaim;
    
    private PanelTextBox titleReward;
    
    private CanvasEmpty cvInner;
    
    private IGuiRect rectReward;
    private IGuiRect rectTask;
    
    private IGuiPanel pnReward;
    
    private CanvasScrolling tskScroll;
    private PanelVScrollBar tskScrollBar;
    
    private int rewardIndex = 0;
    
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
        
        if(quest.getRewards().size() > 0)
        {
            CanvasScrolling cvDesc = new CanvasScrolling(new GuiTransform(new Vector4f(0F, 0F, 0.5F, 0.5F), new GuiPadding(0, 0, 16, 16), 0));
            cvInner.addPanel(cvDesc);
            PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), QuestTranslation.translate(quest.getProperty(NativeProps.DESC)), true);
            paDesc.setColor(PresetColor.TEXT_MAIN.getColor());//.setFontSize(4);
            cvDesc.addCulledPanel(paDesc, false);
            
            PanelVScrollBar paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.MID_CENTER), new GuiPadding(-16, 0, 8, 16), 0));
            cvInner.addPanel(paDescScroll);
            cvDesc.setScrollDriverY(paDescScroll);
            paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
    
            btnClaim = new PanelButton(new GuiTransform(new Vector4f(0F, 1F, 0.5F, 1F), new GuiPadding(16, -16, 24, 0), 0), 6, QuestTranslation.translate("betterquesting.btn.claim"));
            btnClaim.setActive(false);
            cvInner.addPanel(btnClaim);
    
            btnRewardLeft = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, new GuiPadding(0, -16, -16, 0), 0), 2, "<");
            //btnRewardLeft.setActive(rewardIndex > 0);
            btnRewardLeft.setEnabled(rewardIndex > 0);
            btnRewardLeft.setTextHighlight(new GuiColorStatic(0xFF000000), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 1F, 0F), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 1F, 0F));
            cvInner.addPanel(btnRewardLeft);
    
            btnRewardRight = new PanelButton(new GuiTransform(new Vector4f(0.5F, 1F, 0.5F, 1F), new GuiPadding(-24, -16, 8, 0), 0), 3, ">");
            //btnRewardRight.setActive(rewardIndex < quest.getRewards().size() - 1);
            btnRewardRight.setEnabled(rewardIndex < quest.getRewards().size() - 1);
            btnRewardRight.setTextHighlight(new GuiColorStatic(0xFF000000), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 1F, 0F), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 1F, 0F));
            cvInner.addPanel(btnRewardRight);
            
            rectReward = new GuiTransform(new Vector4f(0F, 0.5F, 0.5F, 1F), new GuiPadding(0, 0, 8, 16), 0);
            rectReward.setParent(cvInner.getTransform());
            
            titleReward = new PanelTextBox(new GuiTransform(new Vector4f(0F, 0.5F, 0.5F, 0.5F), new GuiPadding(0, -16, 8, 0), 0), "?");
            titleReward.setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            cvInner.addPanel(titleReward);
            
            refreshRewardPanel();
        } else
        {
            CanvasScrolling cvDesc = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(0, 0, 16, 0), 0));
            cvInner.addPanel(cvDesc);
            PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, 0, cvDesc.getTransform().getWidth(), 0), QuestTranslation.translate(quest.getProperty(NativeProps.DESC)), true);
            paDesc.setColor(PresetColor.TEXT_MAIN.getColor());//.setFontSize(4);
            cvDesc.addCulledPanel(paDesc, false);
            
            PanelVScrollBar paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.quickAnchor(GuiAlign.TOP_CENTER, GuiAlign.BOTTOM_CENTER), new GuiPadding(-16, 0, 8, 0), 0));
            cvInner.addPanel(paDescScroll);
            cvDesc.setScrollDriverY(paDescScroll);
            paDescScroll.setEnabled(cvDesc.getScrollBounds().getHeight() > 0);
        }
        
        //if(quest.getTasks().size() > 0)
        {
            btnDetect = new PanelButton(new GuiTransform(new Vector4f(0.5F, 1F, 1F, 1F), new GuiPadding(24, -16, 16, 0), 0), 7, QuestTranslation.translate("betterquesting.btn.detect_submit"));
            btnDetect.setActive(false);
            cvInner.addPanel(btnDetect);
            
            rectTask = new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 0, 0, 18), 0);
            rectTask.setParent(cvInner.getTransform());
            
            refreshTaskPanel();
        }
    
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
        this.refreshRewardPanel();
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
        } else if(btn.getButtonID() == 2) // Reward previous
        {
            rewardIndex = MathHelper.clamp(rewardIndex - 1, 0, quest.getRewards().size() - 1);
            refreshRewardPanel();
        } else if(btn.getButtonID() == 3) // Reward next
        {
            rewardIndex = MathHelper.clamp(rewardIndex + 1, 0, quest.getRewards().size() - 1);
            refreshRewardPanel();
        } else if(btn.getButtonID() == 6) // Reward claim
        {
            NetQuestAction.requestClaim(new int[]{questID});
        } else if(btn.getButtonID() == 7) // Task detect/submit
        {
            NetQuestAction.requestDetect(new int[]{questID});
        }
    }
    
    private void refreshRewardPanel()
    {
        if(pnReward != null)
        {
            cvInner.removePanel(pnReward);
        }
        
        if(rewardIndex < 0 || rewardIndex >= quest.getRewards().size())
        {
            if(titleReward != null)
            {
                titleReward.setText("?");
                titleReward.setEnabled(false);
            }
            updateButtons();
            
            return;
        } else if(rectReward == null)
        {
            this.initPanel();
            return;
        }
        
        IReward rew = quest.getRewards().getEntries().get(rewardIndex).getValue();
        
        pnReward = rew.getRewardGui(rectReward, new DBEntry<>(questID, quest));
        
        if(pnReward != null)
        {
            cvInner.addPanel(pnReward);
        }
        
        if(titleReward != null)
        {
            titleReward.setText(QuestTranslation.translate(rew.getUnlocalisedName()));
            titleReward.setEnabled(true);
        }
        
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
        tskScrollBar = new PanelVScrollBar(new GuiRectangle(cvInner.getTransform().getWidth(), 0, 8, rectTask.getHeight(), 0));
        cvInner.addPanel(tskScrollBar);
        tskScroll.setScrollDriverY(tskScrollBar);
        
        for(int index = 0; index<quest.getTasks().size(); index++) {
        	ITask tsk = quest.getTasks().getEntries().get(index).getValue();
        	PanelTextBox tskTitle = new PanelTextBox(new GuiRectangle(0, index * rectTask.getHeight(), rectTask.getWidth(), 16, 0), QuestTranslation.translate(tsk.getUnlocalisedName())).setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
        	IGuiPanel pnTask = tsk.getTaskGui(new GuiRectangle(0, (index * rectTask.getHeight()) + 16, rectTask.getWidth(), rectTask.getHeight()-16, 0), new DBEntry<>(questID, quest));
        	tskScroll.addCulledPanel(tskTitle, true);
        	tskScroll.addCulledPanel(pnTask, true);
        }
        
        tskScrollBar.setEnabled(tskScroll.getScrollBounds().getHeight() > 0);
        
        updateButtons();
    }
    
    private void updateButtons()
    {
        Minecraft mc = Minecraft.getMinecraft();
        
        if(btnRewardLeft != null && btnRewardRight != null && btnClaim != null)
        {
            //btnRewardLeft.setActive(rewardIndex > 0);
            //btnRewardRight.setActive(rewardIndex < quest.getRewards().size() - 1);
        	btnRewardLeft.setEnabled(rewardIndex > 0);
            btnRewardRight.setEnabled(rewardIndex < quest.getRewards().size() - 1);
            
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
