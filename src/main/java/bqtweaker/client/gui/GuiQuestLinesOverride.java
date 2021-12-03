package bqtweaker.client.gui;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.questing.IQuestLineEntry;
import betterquesting.api.utils.RenderUtils;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasHoverTray;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestLine;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.popups.PopContextMenu;
import betterquesting.api2.client.gui.resources.colors.GuiColorPulse;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.resources.textures.GuiTextureColored;
import betterquesting.api2.client.gui.resources.textures.OreDictTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.editors.GuiQuestEditor;
import betterquesting.client.gui2.editors.GuiQuestLinesEditor;
import betterquesting.client.gui2.editors.designer.GuiDesigner;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class GuiQuestLinesOverride extends GuiScreenCanvas implements IPEventListener, INeedsRefresh
{
    private IQuestLine selectedLine = null;
    private static int selectedLineId = -1;
    
    private final List<Tuple<DBEntry<IQuestLine>, Integer>> visChapters = new ArrayList<>();
    
    private CanvasQuestLine cvQuest;
    private CanvasEmpty cvQuestPopup;
    
    // Keep these separate for now
    private static CanvasEmpty cvChapterTray;
    private static CanvasHoverTray cvFrame;
    
    private CanvasScrolling cvLines;
    private PanelVScrollBar scLines;
    
    private PanelGeneric icoChapter;
    private CanvasScrolling txTitleScroll;
    private CanvasScrolling txDescScroll;
    private PanelTextBox txTitle;
    private PanelTextBox txDesc;
    
    private PanelButton claimAll;
    private PanelButton btnDesign;
    
    private final List<PanelButtonStorage<DBEntry<IQuestLine>>> btnListRef = new ArrayList<>();
    
    public GuiQuestLinesOverride(GuiScreen parent)
    {
        super(parent);
    }
    
    @Override
    public void refreshGui()
    {
        refreshChapterVisibility();
        refreshContent();
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }
        
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player);
        
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        CanvasTextured cvTopBar = new CanvasTextured(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 0, 0, -32), 0), PresetTexture.PANEL_MAIN.getTexture());
        cvBackground.addPanel(cvTopBar);
        
        PanelButton btnExit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -24, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_PG_PREV.getTexture());
        btnExit.setClickAction((b) -> mc.displayGuiScreen(parent));
        btnExit.setTooltip(Collections.singletonList(QuestTranslation.translate("gui.back")));
        cvBackground.addPanel(btnExit);
        
        if(canEdit)
        {
            PanelButton btnEdit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -40, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_GEAR.getTexture());
            btnEdit.setClickAction((b) -> mc.displayGuiScreen(new GuiQuestLinesEditor(this)));
            btnEdit.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.edit")));
            cvBackground.addPanel(btnEdit);
            
            btnDesign = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -56, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_SORT.getTexture());
            btnDesign.setClickAction((b) -> mc.displayGuiScreen(new GuiDesigner(this, selectedLine)));
            btnDesign.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.designer")));
            cvBackground.addPanel(btnDesign);
            
            btnDesign.setActive(selectedLine!=null);
        }
        
        icoChapter = new PanelGeneric(new GuiTransform(GuiAlign.TOP_LEFT, 16, 8, 16, 16, 0), null);
        cvTopBar.addPanel(icoChapter);
        
        txTitleScroll = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(44, 8, 2, 6), 0));
        cvTopBar.addPanel(txTitleScroll);
        
        txTitle = new PanelTextBox(new GuiRectangle(0, 0, txTitleScroll.getTransform().getWidth(), txTitleScroll.getTransform().getHeight()), "", true);
        txTitle.setColor(PresetColor.TEXT_HEADER.getColor());
        txTitleScroll.addCulledPanel(txTitle, false);
        
        txDescScroll = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(2, 8, 6, 6), 0));
        cvTopBar.addPanel(txDescScroll);
        
        txDesc = new PanelTextBox(new GuiRectangle(0, 0, txDescScroll.getTransform().getWidth(), txDescScroll.getTransform().getHeight()), "", true);
        txDesc.setColor(PresetColor.TEXT_HEADER.getColor());
        txDescScroll.addCulledPanel(txDesc, false);
        
        cvFrame = new CanvasHoverTray(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(174, 32, 8, 8), 0), new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(180, 32, 8, 8), 0), PresetTexture.AUX_FRAME_0.getTexture());
        cvFrame.setManualOpen(true);
        cvBackground.addPanel(cvFrame);
        cvFrame.setTrayState(true, 1);
        
        // === CHAPTER TRAY ===
        
        cvChapterTray = new CanvasEmpty(new GuiRectangle(24, 24, 164, cvFrame.getTransform().getHeight()+16));
        cvBackground.addPanel(cvChapterTray);
        
        cvLines = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 8, 16, 8), 0));
        cvChapterTray.addPanel(cvLines);
        
        scLines = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-16, 8, 8, 8), 0));
        cvLines.setScrollDriverY(scLines);
        cvChapterTray.addPanel(scLines);
        
        PanelButton fitView = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 48, 16, 16, -2), 5, "");
        fitView.setIcon(PresetIcon.ICON_BOX_FIT.getTexture());
        fitView.setClickAction((b) -> {
            if(cvQuest.getQuestLine() != null) cvQuest.fitToWindow();
        });
        fitView.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.zoom_fit")));
        cvBackground.addPanel(fitView);
        
        claimAll = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 32, 16, 16, -2), -1, "");
        claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture());
        claimAll.setClickAction((b) -> {
            if(cvQuest.getQuestButtons().size() <= 0) return;
            List<Integer> claimIdList = new ArrayList<>();
            for(PanelButtonQuest pbQuest : cvQuest.getQuestButtons())
            {
                IQuest q = pbQuest.getStoredValue().getValue();
                if(q.getRewards().size() > 0 && q.canClaim(mc.player)) claimIdList.add(pbQuest.getStoredValue().getID());
            }
            
            int[] cIDs = new int[claimIdList.size()];
            for(int i = 0; i < cIDs.length; i++)
            {
                cIDs[i] = claimIdList.get(i);
            }
    
            NetQuestAction.requestClaim(cIDs);
            claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        });
        claimAll.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.claim_all")));
        cvBackground.addPanel(claimAll);
        
        // === CHAPTER VIEWPORT ===
        
        CanvasQuestLine oldCvQuest = cvQuest;
        cvQuest = new CanvasQuestLine(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), 2);
        
        //BQTweaker Right-Click menu
        cvQuestPopup = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0)){
        	@Override
        	public boolean onMouseClick(int mx, int my, int click) {
        		if(cvQuest.getQuestLine() == null || !this.getTransform().contains(mx, my)) return false;
            	if(canEdit && click == 1){
            		PopContextMenu popup = new PopContextMenu(new GuiRectangle(mx, my, RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.btn.designer"), Minecraft.getMinecraft().fontRenderer)+12, cvQuest.getButtonAt(mx, my) != null ? 32 : 16), true);
            		
            		if(cvQuest.getButtonAt(mx, my) != null) {
	            		GuiQuestEditor editor = new GuiQuestEditor(new GuiQuestLinesOverride(parent), cvQuest.getButtonAt(mx, my).getStoredValue().getID());
	            		Runnable actionEditor = new Runnable() {
	            			@Override
	            			public void run(){
	            				mc.displayGuiScreen(editor);
	            			}
	            		};
	            		popup.addButton(QuestTranslation.translate("betterquesting.btn.edit"), null, actionEditor);
            		}
            		
            		GuiDesigner designer = new GuiDesigner(new GuiQuestLinesOverride(parent), cvQuest.getQuestLine());
            		Runnable actionDesigner = new Runnable() {
            			@Override
            			public void run(){
            				mc.displayGuiScreen(designer);
            			}
            		};
            		popup.addButton(QuestTranslation.translate("betterquesting.btn.designer"), null, actionDesigner);
            		openPopup(popup);
            		return true;
            	}
            	else return false;
            }
        };
        cvFrame.addPanel(cvQuest);
        cvFrame.addPanel(cvQuestPopup);
    
        if(selectedLine != null)
        {
            cvQuest.setQuestLine(selectedLine);
            
            if(oldCvQuest != null)
            {
                cvQuest.setZoom(oldCvQuest.getZoom());
                cvQuest.setScrollX(oldCvQuest.getScrollX());
                cvQuest.setScrollY(oldCvQuest.getScrollY());
                cvQuest.refreshScrollBounds();
                cvQuest.updatePanelScroll();
            }
            
            txTitle.setText(QuestTranslation.translate(selectedLine.getUnlocalisedName()));
            txDesc.setText(QuestTranslation.translate(selectedLine.getUnlocalisedDescription()));
            icoChapter.setTexture(new OreDictTexture(1F, selectedLine.getProperty(NativeProps.ICON), false, true), null);
        }
        
        // === MISC ===
        refreshChapterVisibility();
        refreshClaimAll();
        refreshDesigner();
    }
    
    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }
    
    // TODO: Change CanvasQuestLine to NOT need these panel events anymore
    private void onButtonPress(PEventButton event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) // Quest Instance Select
        {
            @SuppressWarnings("unchecked")
            DBEntry<IQuest> quest = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            GuiHome.bookmark = new GuiQuest(this, quest.getID());
            
            mc.displayGuiScreen(GuiHome.bookmark);
        }
    }
    
    private void refreshChapterVisibility()
    {
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(mc.player);
        List<DBEntry<IQuestLine>> lineList = QuestLineDatabase.INSTANCE.getSortedEntries();
        this.visChapters.clear();
        UUID playerID = QuestingAPI.getQuestingUUID(mc.player);
        
        for(DBEntry<IQuestLine> dbEntry : lineList)
        {
            IQuestLine ql = dbEntry.getValue();
            EnumQuestVisibility vis = ql.getProperty(NativeProps.VISIBILITY);
            if(!canEdit && vis == EnumQuestVisibility.HIDDEN) continue;
        
            boolean show = false;
            boolean unlocked = false;
            boolean complete = false;
            boolean allComplete = true;
            boolean pendingClaim = false;
        
            if(canEdit)
            {
                show = true;
                unlocked = true;
                complete = true;
            }
            
            for(DBEntry<IQuestLineEntry> qID : ql.getEntries())
            {
                IQuest q = QuestDatabase.INSTANCE.getValue(qID.getID());
                if(q == null) continue;
                
                if(allComplete && !q.isComplete(playerID)) allComplete = false;
                if(!pendingClaim && q.isComplete(playerID) && !q.hasClaimed(playerID)) pendingClaim = true;
                if(!unlocked && q.isUnlocked(playerID)) unlocked = true;
                if(!complete && q.isComplete(playerID)) complete = true;
                if(!show && QuestCache.isQuestShown(q, playerID, mc.player)) show = true;
                if(unlocked && complete && show && pendingClaim && !allComplete) break;
            }
        
            if(vis == EnumQuestVisibility.COMPLETED && !complete)
            {
                continue;
            } else if(vis == EnumQuestVisibility.UNLOCKED && !unlocked)
            {
                continue;
            }
            
            int val = pendingClaim ? 1 : 0;
            if(allComplete) val |= 2;
            if(!show) val |= 4;
            
            visChapters.add(new Tuple<>(dbEntry, val));
        }
        
        buildChapterList();
    }
    
    private void buildChapterList()
    {
        cvLines.resetCanvas();
        btnListRef.clear();
        
        int listW = cvLines.getTransform().getWidth();
        
        for(int n = 0; n < visChapters.size(); n++)
        {
            DBEntry<IQuestLine> entry = visChapters.get(n).getFirst();
            int vis = visChapters.get(n).getSecond();
            
            cvLines.addPanel(new PanelGeneric(new GuiRectangle(0, n * 16, 16, 16, 0), new OreDictTexture(1F, entry.getValue().getProperty(NativeProps.ICON), false, true)));
            
            if((vis & 1) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_NOTICE.getTexture(), new GuiColorStatic(0xFFFFFF00))));
            } else if((vis & 2) > 0)
            {
                cvLines.addPanel(new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_TICK.getTexture(), new GuiColorStatic(0xFF00FF00))));
            }
            PanelButtonStorage<DBEntry<IQuestLine>> btnLine = new PanelButtonStorage<>(new GuiRectangle(16, n * 16, listW - 16, 16, 0), 1, QuestTranslation.translate(entry.getValue().getUnlocalisedName()), entry);
            btnLine.setTextAlignment(0);
            btnLine.setActive((vis & 4) == 0 && entry.getID() != selectedLineId);
            btnLine.setCallback((q) -> {
                btnListRef.forEach((b) -> {if(b.getStoredValue().getID() == selectedLineId) b.setActive(true);});
                btnLine.setActive(false);
                selectedLine = q.getValue();
                selectedLineId = q.getID();
                cvQuest.setQuestLine(q.getValue());
                icoChapter.setTexture(new OreDictTexture(1F, q.getValue().getProperty(NativeProps.ICON), false, true), null);
                txTitle.setText(QuestTranslation.translate(q.getValue().getUnlocalisedName()));
                txDesc.setText(QuestTranslation.translate(q.getValue().getUnlocalisedDescription()));
                cvQuest.fitToWindow();
                refreshClaimAll();
                refreshDesigner();
            });
            cvLines.addPanel(btnLine);
            btnListRef.add(btnLine);
        }
        
        cvLines.refreshScrollBounds();
        scLines.setEnabled(cvLines.getScrollBounds().getHeight() > 0);
    }
    
    private void refreshContent()
    {
        if(selectedLineId >= 0)
        {
            selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(selectedLine == null) selectedLineId = -1;
        } else
        {
            selectedLine = null;
        }
        
        float zoom = cvQuest.getZoom();
        int sx = cvQuest.getScrollX();
        int sy = cvQuest.getScrollY();
        cvQuest.setQuestLine(selectedLine);
        cvQuest.setZoom(zoom);
        cvQuest.setScrollX(sx);
        cvQuest.setScrollY(sy);
        cvQuest.refreshScrollBounds();
        cvQuest.updatePanelScroll();
        
        if(selectedLine != null)
        {
            txTitle.setText(QuestTranslation.translate(selectedLine.getUnlocalisedName()));
            txDesc.setText(QuestTranslation.translate(selectedLine.getUnlocalisedDescription()));
            icoChapter.setTexture(new OreDictTexture(1F, selectedLine.getProperty(NativeProps.ICON), false, true), null);
        } else
        {
            txTitle.setText("");
            txDesc.setText("");
            icoChapter.setTexture(null, null);
        }
        
        refreshClaimAll();
        refreshDesigner();
    }
    
    private void refreshDesigner() {
    	//BQTweaker design button refresh
        if(btnDesign!=null) {
            btnDesign.setActive(selectedLine!=null);
        }
    }
    
    private void refreshClaimAll()
    {
        if(cvQuest.getQuestLine() == null || cvQuest.getQuestButtons().size() <= 0)
        {
            claimAll.setActive(false);
            claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
            return;
        }
        
        for(PanelButtonQuest btn : cvQuest.getQuestButtons())
        {
            if(btn.getStoredValue().getValue().canClaim(mc.player))
            {
                claimAll.setActive(true);
                claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 2F, 0F), 0);
                return;
            }
        }
        
        claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        claimAll.setActive(false);
    }
}
