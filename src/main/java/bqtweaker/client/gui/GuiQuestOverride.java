package bqtweaker.client.gui;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.RenderUtils;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.io.FloatSimpleIO;
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
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.editors.GuiQuestEditor;
import betterquesting.client.gui2.editors.GuiRewardEditor;
import betterquesting.client.gui2.editors.GuiTaskEditor;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

import org.lwjgl.util.vector.Vector4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class GuiQuestOverride extends GuiScreenCanvas implements IPEventListener, INeedsRefresh {
    private static final Map<Integer, GuiQuest.ScrollPosition> scrollsPositions = new HashMap<>();
    private GuiQuest.ScrollPosition scrollPosition;
    private final int questID;
    private IQuest quest;
    private PanelButton btnDetect;
    private PanelButton btnClaim;
    private CanvasEmpty cvInner;
    private IGuiRect rectDescReward;
    private IGuiRect rectTask;
    private CanvasEmpty pnDescReward;
    private CanvasEmpty pnTask;
    private CanvasScrolling csDescReward;
    private CanvasScrolling csTask;

    public GuiQuestOverride(GuiScreen parent, int questID) {
        super(parent);
        this.questID = questID;
        this.scrollPosition = scrollsPositions.get(questID);
        if(this.scrollPosition == null) {
            this.scrollPosition = new GuiQuest.ScrollPosition(0, 0, 0);
            scrollsPositions.put(questID, this.scrollPosition);
        }
    }
    
    @Override
    public void initPanel() {
        super.initPanel();
        this.quest = QuestDatabase.INSTANCE.getValue(this.questID);
        if(this.quest == null) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
    
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
    
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate(this.quest.getProperty(NativeProps.NAME))).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        
        if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(this.mc.player)) {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -24, 100, 16, 0), 0, QuestTranslation.translate("gui.back")));
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -24, 100, 16, 0), 1, QuestTranslation.translate("betterquesting.btn.edit")));
        }
        else {
            cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -24, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        }
        
        this.cvInner = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(16, 32, 16, 24), 0));
        cvBackground.addPanel(this.cvInner);
        
        //Reward and Desc scrolling
        if(this.quest.getRewards().size() > 0) {
            this.btnClaim = new PanelButton(new GuiTransform(new Vector4f(0F, 1F, 0.5F, 1F), new GuiPadding(16, -16, 24, 0), 0), 6, QuestTranslation.translate("betterquesting.btn.claim"));
            this.btnClaim.setActive(false);
            this.cvInner.addPanel(this.btnClaim);
        }
        this.rectDescReward = new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(0, 0, 8, 16), 0);
        this.rectDescReward.setParent(this.cvInner.getTransform());
        CanvasEmpty descRewardPopup = new CanvasEmpty(this.rectDescReward) {
            @Override
            public boolean onMouseClick(int mx, int my, int click) {
                if(click == 1) {
                    if(GuiQuestOverride.this.rectDescReward.contains(mx, my) &&
                            QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(GuiQuestOverride.this.mc.player)) {
                        int width = RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.context.add_reward"), Minecraft.getMinecraft().fontRenderer);
                        PopContextMenu popup = new PopContextMenu(new GuiRectangle(mx, my, width + 12, 16), true);
                        GuiRewardEditor editor = new GuiRewardEditor(new GuiQuestOverride(GuiQuestOverride.this.parent, GuiQuestOverride.this.questID), GuiQuestOverride.this.quest);
                        Runnable action = () -> GuiQuestOverride.this.mc.displayGuiScreen(editor);
                        popup.addButton(QuestTranslation.translate("betterquesting.context.add_reward"), null, action);
                        GuiQuestOverride.this.openPopup(popup);
                        return true;
                    }
                }
                return false;
            }
        };
        this.cvInner.addPanel(descRewardPopup);
        //Handle Reward/Desc logic
        this.refreshRewardDescPanel();
        
        
        //Task Scrolling
        this.btnDetect = new PanelButton(new GuiTransform(new Vector4f(0.5F, 1F, 1F, 1F), new GuiPadding(24, -16, 16, 0), 0), 7, QuestTranslation.translate("betterquesting.btn.detect_submit"));
        this.btnDetect.setActive(false);
        this.cvInner.addPanel(this.btnDetect);
        this.rectTask = new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 0, 0, 16), 0);
        this.rectTask.setParent(this.cvInner.getTransform());
        CanvasEmpty taskPopup = new CanvasEmpty(this.rectTask) {
        	@Override
        	public boolean onMouseClick(int mx, int my, int click) {
        		if(click == 1) {
                    if(!(GuiQuestOverride.this.rectTask.getX() < mx &&
                            GuiQuestOverride.this.rectTask.getX()+GuiQuestOverride.this.rectTask.getWidth() > mx &&
                            GuiQuestOverride.this.rectTask.getY() < my &&
                            GuiQuestOverride.this.rectTask.getY() + GuiQuestOverride.this.rectTask.getHeight() > my)) return false;
                    if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(GuiQuestOverride.this.mc.player)) {
                        int width = RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.context.add_task"), Minecraft.getMinecraft().fontRenderer);
                        PopContextMenu popup = new PopContextMenu(new GuiRectangle(mx, my, width + 12, 16), true);
                        GuiTaskEditor editor = new GuiTaskEditor(new GuiQuestOverride(GuiQuestOverride.this.parent, GuiQuestOverride.this.questID), GuiQuestOverride.this.quest);
                        Runnable action = () -> GuiQuestOverride.this.mc.displayGuiScreen(editor);
                        popup.addButton(QuestTranslation.translate("betterquesting.context.add_task"), null, action);
                        GuiQuestOverride.this.openPopup(popup);
                        return true;
                    }
                }
            	return false;
            }
        };
        this.cvInner.addPanel(taskPopup);
        //Handle task logic
        this.refreshTaskPanel();
        
        IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 0, 0, 0, 0);
        ls0.setParent(this.cvInner.getTransform());
        IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, 0, 0, 0, 0);
        le0.setParent(this.cvInner.getTransform());
        PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
        this.cvInner.addPanel(paLine0);
    }
    
    @Override
    public void refreshGui() {
        this.refreshTaskPanel();
        this.refreshRewardDescPanel();
        this.updateButtons();
    }
    
    @Override
    public boolean onMouseClick(int mx, int my, int click) {
        if(super.onMouseClick(mx, my, click)) {
            this.updateButtons();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseRelease(int mx, int my, int click) {
        boolean var4;
        try {
            var4 = super.onMouseRelease(mx, my, click);
        }
        finally {
            if(this.csDescReward != null) {
                this.scrollPosition.setDescScrollY(this.csDescReward.getScrollY());
            }

            if (this.csTask != null) {
                this.scrollPosition.setTaskScrollY(this.csTask.getScrollY());
            }
        }
        return var4;
    }
    
    @Override
    public boolean onMouseScroll(int mx, int my, int scroll) {
        boolean var4;
        try {
            if(!super.onMouseScroll(mx, my, scroll)) return false;
            this.updateButtons();
            var4 = true;
        }
        finally {
            if(this.csDescReward != null) {
                this.scrollPosition.setDescScrollY(this.csDescReward.getScrollY());
            }

            if(this.csTask != null) {
                this.scrollPosition.setTaskScrollY(this.csTask.getScrollY());
            }
        }
        return var4;
    }
    
    @Override
    public boolean onKeyTyped(char c, int keycode) {
        if(super.onKeyTyped(c, keycode)) {
            this.updateButtons();
            return true;
        }
        return false;
    }
    
    @Override
    public void onPanelEvent(PanelEvent event) {
        if(event instanceof PEventButton) this.onButtonPress((PEventButton)event);
    }
    
    private void onButtonPress(PEventButton event) {
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 0) this.mc.displayGuiScreen(this.parent);
        else if(btn.getButtonID() == 1) this.mc.displayGuiScreen(new GuiQuestEditor(this, this.questID));
        else if(btn.getButtonID() == 6) NetQuestAction.requestClaim(new int[]{this.questID});
        else if(btn.getButtonID() == 7) NetQuestAction.requestDetect(new int[]{this.questID});
    }
    
    private void refreshRewardDescPanel() {
        if(this.pnDescReward != null) this.cvInner.removePanel(this.pnDescReward);

        this.pnDescReward = new CanvasEmpty(this.rectDescReward);
        this.cvInner.addPanel(this.pnDescReward);
        this.csDescReward = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 8, 1), 0));
        this.pnDescReward.addPanel(this.csDescReward);
        PanelVScrollBar paDescScroll = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 0, 0, 0), 0));
        this.pnDescReward.addPanel(paDescScroll);
        this.csDescReward.setScrollDriverY(paDescScroll);

        String desc = QuestTranslation.translate(this.quest.getProperty(NativeProps.DESC));
        int yOffset = 0;
        
        if(desc.contains("{Embed}")) {
        	String[] descSplit = desc.split("\\{Embed\\}");
        	for(String split : descSplit) {
        		if(split.startsWith("TypeImage") || split.startsWith("TypeLink")) {
        			try {
            			String[] embedEntry = split.split(";");
            			int width = Math.min(csDescReward.getTransform().getWidth(), Integer.parseInt(embedEntry[2]));
            			int height = Integer.parseInt(embedEntry[3]);
            			
            			if(embedEntry[0].contentEquals("TypeImage")) {
            	        	IGuiTexture embedImage = new SimpleTexture(new ResourceLocation(embedEntry[1]), new GuiRectangle(0, 0, Integer.parseInt(embedEntry[4]), Integer.parseInt(embedEntry[5])));
                            this.csDescReward.addCulledPanel(new PanelGeneric(new GuiRectangle((this.csDescReward.getTransform().getWidth()-width)/2, yOffset, width, height), embedImage), false);
            			}
            			else if(embedEntry[0].contentEquals("TypeLink")) {
            	        	PanelButton btnLink = new PanelButton(new GuiRectangle((this.csDescReward.getTransform().getWidth()-width)/2,  yOffset, width, height), 69, embedEntry[4]);
            	        	btnLink.setClickAction((b) -> {
            	        		TextComponentString component = new TextComponentString(embedEntry[1]);
            	            	component.setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, embedEntry[1])));
            	            	this.handleComponentClick(component);
            	        	});
            	        	btnLink.setTextAlignment(1);
                            this.csDescReward.addCulledPanel(btnLink, false);
            			}

                        yOffset += height + 8;
        			}
        			catch(Exception ignored) {}
        		}
        		else {
        			PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, yOffset, this.csDescReward.getTransform().getWidth(), 0), split, true);
                	paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
                    this.csDescReward.addCulledPanel(paDesc, false);

                    yOffset += paDesc.getTransform().getHeight() + 8;
        		}
        	}
            yOffset += 16;
        }
        else {
        	PanelTextBox paDesc = new PanelTextBox(new GuiRectangle(0, yOffset, this.csDescReward.getTransform().getWidth(), 0), desc, true);
        	paDesc.setColor(PresetColor.TEXT_MAIN.getColor());
            this.csDescReward.addCulledPanel(paDesc, false);
            yOffset += 16 + paDesc.getTransform().getHeight() + 8;
        }
        
        for(DBEntry<IReward> entry : this.quest.getRewards().getEntries()) {
            IReward rew = entry.getValue();
        	PanelTextBox rwdTitle = new PanelTextBox(
                    new GuiTransform(new Vector4f(), 0, yOffset, this.csDescReward.getTransform().getWidth(), 12, 0),
                    QuestTranslation.translate(rew.getUnlocalisedName())
            );
            rwdTitle.setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            rwdTitle.setEnabled(true);
            this.csDescReward.addPanel(rwdTitle);
            yOffset += 14;

        	IGuiPanel pnReward = rew.getRewardGui(
                    new GuiTransform(GuiAlign.FULL_BOX, 0, 0, this.csDescReward.getTransform().getWidth(), this.csDescReward.getTransform().getHeight(), 111),
                    new DBEntry<>(this.questID, this.quest)
            );
            pnReward.initPanel();
            CanvasEmpty tmpCanvas = new CanvasEmpty(
                    new GuiTransform(GuiAlign.TOP_LEFT, 0, yOffset, this.csDescReward.getTransform().getWidth(), pnReward.getTransform().getHeight() - pnReward.getTransform().getY(), 1)
            );
            this.csDescReward.addPanel(tmpCanvas);
            tmpCanvas.addPanel(pnReward);
            yOffset += tmpCanvas.getTransform().getHeight() + 4;
        }
        paDescScroll.setEnabled(this.csDescReward.getScrollBounds().getHeight() > 0);
        this.csDescReward.setScrollY(this.scrollPosition.getDescScrollY());
        this.csDescReward.updatePanelScroll();
        updateButtons();
    }
    
    private void refreshTaskPanel() {
        if(this.pnTask != null) this.cvInner.removePanel(this.pnTask);

        this.pnTask = new CanvasEmpty(this.rectTask);
        this.cvInner.addPanel(this.pnTask);
        this.csTask = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 8, 1), 0));
        this.pnTask.addPanel(this.csTask);
        PanelVScrollBar scList = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 0, 0, 0), 0));
        this.pnTask.addPanel(scList);
        this.csTask.setScrollDriverY(scList);
        this.csTask.setScrollDriverX(new FloatSimpleIO(0.0F, 0.0F, 0.0F));
        int yOffset = 0;
        List<DBEntry<ITask>> entries = this.quest.getTasks().getEntries();

        for(int i = 0; i < entries.size(); ++i) {
            ITask tsk = entries.get(i).getValue();
            String taskName = i + 1 + ". " + QuestTranslation.translate(tsk.getUnlocalisedName(), new Object[0]);
            PanelTextBox titleReward = new PanelTextBox(new GuiTransform(new Vector4f(), 0, yOffset, this.rectTask.getWidth(), 12, 0), taskName);
            titleReward.setColor(PresetColor.TEXT_HEADER.getColor()).setAlignment(1);
            titleReward.setEnabled(true);
            this.csTask.addPanel(titleReward);
            yOffset += 10;
            IGuiPanel taskGui = tsk.getTaskGui(
                    new GuiTransform(
                            GuiAlign.FULL_BOX,
                            0,
                            i == 0 && entries.size() == 1 && tsk.displaysCenteredAlone() ? this.rectTask.getHeight() / 3 : 0,
                            this.rectTask.getWidth(),
                            this.rectTask.getHeight(),
                            0
                    ),
                    new DBEntry<>(this.questID, this.quest)
            );
            if(taskGui != null) {
                taskGui.initPanel();
                CanvasEmpty tempCanvas = new CanvasEmpty(
                        new GuiTransform(
                                GuiAlign.TOP_LEFT, 0, yOffset, this.rectTask.getWidth(), taskGui.getTransform().getHeight() - taskGui.getTransform().getY(), 1
                        )
                );
                this.csTask.addPanel(tempCanvas);
                tempCanvas.addPanel(taskGui);
                int guiHeight = tempCanvas.getTransform().getHeight();
                yOffset += guiHeight;
            }
            yOffset += 8;
        }
        scList.setEnabled(this.csTask.getScrollBounds().getHeight() > 0);
        this.csTask.setScrollY(this.scrollPosition.getTaskScrollY());
        this.csTask.updatePanelScroll();
        this.updateButtons();
    }
    
    private void updateButtons() {
        Minecraft mc = Minecraft.getMinecraft();
        if(this.btnClaim != null) {
            // Claim button state
            this.btnClaim.setActive(this.quest.getRewards().size() > 0 && this.quest.canClaim(mc.player));
        }
        
        if(this.btnDetect != null) {
            // Detect/submit button state
            this.btnDetect.setActive(this.quest.canSubmit(mc.player));
        }
    }
}
