package bqtweaker.client.gui;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.enums.EnumLogic;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.questing.IQuestLineEntry;
import betterquesting.api.storage.BQ_Settings;
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
import betterquesting.api2.client.gui.popups.PopChoice;
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
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.client.gui2.GuiQuestSearch;
import betterquesting.client.gui2.editors.GuiQuestEditor;
import betterquesting.client.gui2.editors.GuiQuestLinesEditor;
import betterquesting.client.gui2.editors.designer.GuiDesigner;
import betterquesting.handlers.ConfigHandler;
import betterquesting.network.handlers.NetQuestAction;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Tuple;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("deprecation")
public class GuiQuestLinesOverride extends GuiScreenCanvas implements IPEventListener, INeedsRefresh {
    private static int selectedLineId = -1;
    private static GuiQuestLines.ScrollPosition scrollPosition;

    private IQuestLine selectedLine = null;
    private final List<Tuple<DBEntry<IQuestLine>, Integer>> visChapters = new ArrayList<>();
    private CanvasQuestLine cvQuest;
    private CanvasScrolling cvLines;
    private PanelVScrollBar scLines;
    private PanelGeneric icoChapter;
    private PanelTextBox txTitle;
    private PanelTextBox txDesc;
    private PanelTextBox txCompletion;
    private PanelButton claimAll;
    private PanelButton btnDesign;
    private final List<PanelButtonStorage<DBEntry<IQuestLine>>> btnListRef = new ArrayList<>();
    
    public GuiQuestLinesOverride(GuiScreen parent) {
        super(parent);
        if(scrollPosition == null) {
            scrollPosition = new GuiQuestLines.ScrollPosition(0);
        }
    }
    
    @Override
    public void refreshGui() {
        refreshChapterVisibility();
        refreshContent();
    }
    
    @Override
    public void initPanel() {
        super.initPanel();
        GuiHome.bookmark = this;
        if(!BQ_Settings.skipHome) {
            ConfigHandler.config.get("general", "Skip Home", false).set(true);
            ConfigHandler.config.save();
            BQ_Settings.skipHome = true;
        }

        if(selectedLineId >= 0) {
            this.selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(this.selectedLine == null) selectedLineId = -1;
        }
        else this.selectedLine = null;
        
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(this.mc.player);
        
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        CanvasTextured cvTopBar = new CanvasTextured(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 0, 0, -32), 0), PresetTexture.PANEL_MAIN.getTexture());
        cvBackground.addPanel(cvTopBar);
        
        PanelButton btnExit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -24, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_PG_PREV.getTexture());
        btnExit.setClickAction((b) -> this.mc.displayGuiScreen(this.parent));
        btnExit.setTooltip(Collections.singletonList(QuestTranslation.translate("gui.back")));
        cvBackground.addPanel(btnExit);
        PanelButton btnSearch = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -40, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_ZOOM.getTexture());
        btnSearch.setClickAction(this::openSearch);
        btnSearch.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.gui.search")));
        cvBackground.addPanel(btnSearch);
        
        if(canEdit) {
            PanelButton btnEdit = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -56, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_GEAR.getTexture());
            btnEdit.setClickAction((b) -> this.mc.displayGuiScreen(new GuiQuestLinesEditor(this)));
            btnEdit.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.edit")));
            cvBackground.addPanel(btnEdit);

            this.btnDesign = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_LEFT, 8, -72, 16, 16, 0), -1, "").setIcon(PresetIcon.ICON_SORT.getTexture());
            this.btnDesign.setClickAction((b) -> this.mc.displayGuiScreen(new GuiDesigner(this, this.selectedLine)));
            this.btnDesign.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.designer")));
            cvBackground.addPanel(this.btnDesign);

            this.btnDesign.setActive(selectedLine!=null);
        }

        this.icoChapter = new PanelGeneric(new GuiTransform(GuiAlign.TOP_LEFT, 8, 8, 16, 16, 0), null);
        cvTopBar.addPanel(this.icoChapter);

        CanvasScrolling txTitleScroll = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(26, 8, 70, 6), 0));
        cvTopBar.addPanel(txTitleScroll);
        this.txTitle = new PanelTextBox(new GuiRectangle(0, 0, txTitleScroll.getTransform().getWidth(), txTitleScroll.getTransform().getHeight()), "", true);
        this.txTitle.setColor(PresetColor.TEXT_HEADER.getColor());
        txTitleScroll.addCulledPanel(this.txTitle, false);

        CanvasScrolling txCompletionScroll = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(30 + txTitleScroll.getTransform().getWidth(), 8, 2, 6), 0));
        cvTopBar.addPanel(txCompletionScroll);
        this.txCompletion = new PanelTextBox(new GuiRectangle(0, 0, txCompletionScroll.getTransform().getWidth(), txCompletionScroll.getTransform().getHeight()), "", true);
        this.txCompletion.setColor(PresetColor.TEXT_HEADER.getColor());
        txCompletionScroll.addCulledPanel(this.txCompletion, false);

        CanvasScrolling txDescScroll = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(2, 8, 6, 6), 0));
        cvTopBar.addPanel(txDescScroll);
        this.txDesc = new PanelTextBox(new GuiRectangle(0, 0, txDescScroll.getTransform().getWidth(), txDescScroll.getTransform().getHeight()), "", true);
        this.txDesc.setColor(PresetColor.TEXT_HEADER.getColor());
        txDescScroll.addCulledPanel(this.txDesc, false);

        CanvasHoverTray cvFrame = new CanvasHoverTray(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(174, 32, 8, 8), 0), new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(180, 32, 8, 8), 0), PresetTexture.AUX_FRAME_0.getTexture());
        cvFrame.setManualOpen(true);
        cvBackground.addPanel(cvFrame);
        cvFrame.setTrayState(true, 1);
        
        // === CHAPTER TRAY ===

        CanvasEmpty cvChapterTray = new CanvasEmpty(new GuiRectangle(24, 24, 164, cvFrame.getTransform().getHeight() + 16));
        cvBackground.addPanel(cvChapterTray);

        this.cvLines = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 8, 16, 8), 0));
        cvChapterTray.addPanel(this.cvLines);

        this.scLines = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-16, 8, 8, 8), 0));
        this.cvLines.setScrollDriverY(this.scLines);
        cvChapterTray.addPanel(this.scLines);
        
        PanelButton fitView = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 48, 16, 16, -2), 5, "");
        fitView.setIcon(PresetIcon.ICON_BOX_FIT.getTexture());
        fitView.setClickAction((b) -> {
            if(this.cvQuest.getQuestLine() != null) this.cvQuest.fitToWindow();
        });
        fitView.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.zoom_fit")));
        cvBackground.addPanel(fitView);
        
        this.claimAll = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, 8, 32, 16, 16, -2), -1, "");
        this.claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture());
        this.claimAll.setClickAction(
                b -> {
                    if(BQ_Settings.claimAllConfirmation) {
                        this.openPopup(
                                new PopChoice(
                                        QuestTranslation.translate("betterquesting.gui.claim_all_warning")
                                                + "\n\n"
                                                + QuestTranslation.translate("betterquesting.gui.claim_all_confirm"),
                                        PresetIcon.ICON_CHEST_ALL.getTexture(),
                                        integer -> {
                                            if(integer == 1) {
                                                ConfigHandler.config.get("general", "Claim all requires confirmation", true).set(false);
                                                ConfigHandler.config.save();
                                                ConfigHandler.initConfigs();
                                            }

                                            if(integer <= 1) this.claimAll();
                                        },
                                        QuestTranslation.translate("gui.yes"),
                                        QuestTranslation.translate("betterquesting.gui.yes_always"),
                                        QuestTranslation.translate("gui.no"))
                        );
                    }
                    else this.claimAll();
                }
        );
        this.claimAll.setTooltip(Collections.singletonList(QuestTranslation.translate("betterquesting.btn.claim_all")));
        cvBackground.addPanel(this.claimAll);
        
        // === CHAPTER VIEWPORT ===
        
        CanvasQuestLine oldCvQuest = this.cvQuest;
        this.cvQuest = new CanvasQuestLine(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), 2);
        
        //BQTweaker Right-Click menu
        CanvasEmpty cvQuestPopup = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0)) {
            @Override
            public boolean onMouseClick(int mx, int my, int click) {
                if(GuiQuestLinesOverride.this.cvQuest.getQuestLine() == null || !this.getTransform().contains(mx, my)) return false;
                if(click == 1) {
                    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
                    boolean questExistsUnderMouse = GuiQuestLinesOverride.this.cvQuest.getButtonAt(mx, my) != null;
                    if(!canEdit && !questExistsUnderMouse) return false;
                    int maxWidth = questExistsUnderMouse
                            ? RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.btn.share_quest"), fr)
                            : 0;
                    if(canEdit) {
                        maxWidth = Math.max(
                                maxWidth,
                                Math.max(
                                        RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.btn.edit"), fr),
                                        RenderUtils.getStringWidth(QuestTranslation.translate("betterquesting.btn.designer"), fr)
                                )
                        );
                    }

                    PopContextMenu popup = new PopContextMenu(new GuiRectangle(mx, my, maxWidth + 12, questExistsUnderMouse ? 48 : 16), true);
                    if(canEdit) {
                        if(questExistsUnderMouse) {
                            GuiQuestEditor editor = new GuiQuestEditor(
                                    new GuiQuestLinesOverride(GuiQuestLinesOverride.this.parent),
                                    GuiQuestLinesOverride.this.cvQuest.getButtonAt(mx, my).getStoredValue().getID()
                            );
                            Runnable actionEditor = () -> Minecraft.getMinecraft().displayGuiScreen(editor);
                            popup.addButton(QuestTranslation.translate("betterquesting.btn.edit"), null, actionEditor);
                        }
                        GuiDesigner designer = new GuiDesigner(new GuiQuestLinesOverride(GuiQuestLinesOverride.this.parent), GuiQuestLinesOverride.this.cvQuest.getQuestLine());
                        Runnable actionDesigner = () -> Minecraft.getMinecraft().displayGuiScreen(designer);
                        popup.addButton(QuestTranslation.translate("betterquesting.btn.designer"), null, actionDesigner);
                    }

                    if(questExistsUnderMouse) {
                        Runnable questSharer = () -> {
                            GuiQuestLinesOverride.this.mc.player
                                    .sendChatMessage(
                                            "betterquesting.msg.share_quest:" + GuiQuestLinesOverride.this.cvQuest.getButtonAt(mx, my).getStoredValue().getID()
                                    );
                            GuiQuestLinesOverride.this.mc.displayGuiScreen(null);
                        };
                        popup.addButton(QuestTranslation.translate("betterquesting.btn.share_quest"), null, questSharer);
                    }

                    GuiQuestLinesOverride.this.openPopup(popup);
                    return true;
                }
                return false;
            }
        };
        cvFrame.addPanel(this.cvQuest);
        cvFrame.addPanel(cvQuestPopup);
    
        if(this.selectedLine != null) {
            this.cvQuest.setQuestLine(this.selectedLine);
            if(oldCvQuest != null) {
                this.cvQuest.setZoom(oldCvQuest.getZoom());
                this.cvQuest.setScrollX(oldCvQuest.getScrollX());
                this.cvQuest.setScrollY(oldCvQuest.getScrollY());
                this.cvQuest.refreshScrollBounds();
                this.cvQuest.updatePanelScroll();
            }

            this.refreshQuestCompletion();
            this.txTitle.setText(QuestTranslation.translate(this.selectedLine.getUnlocalisedName()));
            this.txDesc.setText(QuestTranslation.translate(this.selectedLine.getUnlocalisedDescription()));
            this.icoChapter.setTexture(new OreDictTexture(1F, this.selectedLine.getProperty(NativeProps.ICON), false, true), null);
        }
        
        // === MISC ===
        this.refreshChapterVisibility();
        this.refreshClaimAll();
        this.refreshDesigner();
        this.cvLines.setScrollY(scrollPosition.getChapterScrollY());
        this.cvLines.updatePanelScroll();
    }

    @Override
    public boolean onMouseRelease(int mx, int my, int click) {
        boolean var4;
        try {
            var4 = super.onMouseRelease(mx, my, click);
        }
        finally {
            if(this.cvLines != null) scrollPosition.setChapterScrollY(this.cvLines.getScrollY());
        }
        return var4;
    }

    @Override
    public boolean onMouseScroll(int mx, int my, int scroll) {
        boolean var4;
        try {
            var4 = super.onMouseScroll(mx, my, scroll);
        }
        finally {
            if(this.cvLines != null) scrollPosition.setChapterScrollY(this.cvLines.getScrollY());
        }
        return var4;
    }

    private void claimAll() {
        if(!this.cvQuest.getQuestButtons().isEmpty()) {
            List<Integer> claimIdList = new ArrayList<>();
            for(PanelButtonQuest pbQuest : this.cvQuest.getQuestButtons()) {
                IQuest q = pbQuest.getStoredValue().getValue();
                if(q.getRewards().size() > 0 && q.canClaim(this.mc.player)) claimIdList.add(pbQuest.getStoredValue().getID());
            }

            int[] cIDs = new int[claimIdList.size()];
            for(int i = 0; i < cIDs.length; ++i) {
                cIDs[i] = claimIdList.get(i);
            }

            NetQuestAction.requestClaim(cIDs);
            this.claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        }
    }
    
    @Override
    public void onPanelEvent(PanelEvent event) {
        if(event instanceof PEventButton) this.onButtonPress((PEventButton)event);
    }

    @SuppressWarnings("unchecked")
    private void onButtonPress(PEventButton event) {
        Minecraft mc = Minecraft.getMinecraft();
        IPanelButton btn = event.getButton();
        if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) {
            DBEntry<IQuest> quest = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            GuiHome.bookmark = new GuiQuest(this, quest.getID());
            mc.displayGuiScreen(GuiHome.bookmark);
        }
    }
    
    private void refreshChapterVisibility() {
        boolean canEdit = QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(this.mc.player);
        List<DBEntry<IQuestLine>> lineList = QuestLineDatabase.INSTANCE.getSortedEntries();
        this.visChapters.clear();
        UUID playerID = QuestingAPI.getQuestingUUID(this.mc.player);
        
        for(DBEntry<IQuestLine> dbEntry : lineList) {
            IQuestLine ql = dbEntry.getValue();
            EnumQuestVisibility vis = ql.getProperty(NativeProps.VISIBILITY);
            if(!canEdit && vis == EnumQuestVisibility.HIDDEN) continue;
            boolean show = false;
            boolean unlocked = false;
            boolean complete = false;
            boolean allComplete = true;
            boolean pendingClaim = false;
            if(canEdit) {
                show = true;
                unlocked = true;
                complete = true;
            }
            
            for(DBEntry<IQuestLineEntry> qID : ql.getEntries()) {
                IQuest q = QuestDatabase.INSTANCE.getValue(qID.getID());
                if(q == null) continue;
                if(allComplete && !this.isQuestCompletedForQuestline(playerID, q)) allComplete = false;
                if(!pendingClaim && q.canClaimBasically(this.mc.player)) pendingClaim = true;
                if(!unlocked && q.isUnlocked(playerID)) unlocked = true;
                if(!complete && q.isComplete(playerID)) complete = true;
                if(!show && QuestCache.isQuestShown(q, playerID, this.mc.player)) show = true;
                if(unlocked && complete && show && pendingClaim && !allComplete) break;
            }
        
            if(vis == EnumQuestVisibility.COMPLETED && !complete) continue;
            if(vis == EnumQuestVisibility.UNLOCKED && !unlocked) continue;
            
            int val = pendingClaim ? 1 : 0;
            if(allComplete) val |= 2;
            if(!show) val |= 4;
            
            this.visChapters.add(new Tuple<>(dbEntry, val));
        }
        this.buildChapterList();
    }

    private boolean isQuestCompletedForQuestline(UUID playerID, @Nonnull IQuest q) {
        if(q.isComplete(playerID)) return true;
        else if(q.getProperty(NativeProps.VISIBILITY) == EnumQuestVisibility.HIDDEN) return true;
        else {
            if(q.getProperty(NativeProps.LOGIC_QUEST) == EnumLogic.XOR) {
                int reqCount = 0;
                for(int qRequirementId : q.getRequirements()) {
                    IQuest quest = QuestDatabase.INSTANCE.getValue(qRequirementId);
                    if(quest.isComplete(playerID)) ++reqCount;
                    if(reqCount == 2) return true;
                }
            }
            return false;
        }
    }
    
    private void buildChapterList() {
        this.cvLines.resetCanvas();
        this.btnListRef.clear();
        
        int listW = this.cvLines.getTransform().getWidth();
        
        for(int n = 0; n < this.visChapters.size(); n++) {
            DBEntry<IQuestLine> entry = this.visChapters.get(n).getFirst();
            int vis = this.visChapters.get(n).getSecond();
            this.cvLines.addPanel(
                    new PanelGeneric(new GuiRectangle(0, n * 16, 16, 16, 0), new OreDictTexture(1F, entry.getValue().getProperty(NativeProps.ICON), false, true)));
            if((vis & 1) > 0) {
                this.cvLines.addPanel(
                        new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_NOTICE.getTexture(), new GuiColorStatic(0xFFFFFF00))));
            }
            else if((vis & 2) > 0) {
                this.cvLines.addPanel(
                        new PanelGeneric(new GuiRectangle(8, n * 16 + 8, 8, 8, -1), new GuiTextureColored(PresetIcon.ICON_TICK.getTexture(), new GuiColorStatic(0xFF00FF00))));
            }
            PanelButtonStorage<DBEntry<IQuestLine>> btnLine = new PanelButtonStorage<>(new GuiRectangle(16, n * 16, listW - 16, 16, 0), 1, QuestTranslation.translate(entry.getValue().getUnlocalisedName()), entry);
            btnLine.setTextAlignment(0);
            btnLine.setActive((vis & 4) == 0 && entry.getID() != selectedLineId);
            btnLine.setCallback((q) -> {
                this.btnListRef.forEach((b) -> { if(b.getStoredValue().getID() == selectedLineId) b.setActive(true); });
                btnLine.setActive(false);
                this.selectedLine = q.getValue();
                selectedLineId = q.getID();
                this.cvQuest.setQuestLine(q.getValue());
                this.icoChapter.setTexture(new OreDictTexture(1F, q.getValue().getProperty(NativeProps.ICON), false, true), null);
                this.refreshQuestCompletion();
                this.txTitle.setText(QuestTranslation.translate(q.getValue().getUnlocalisedName()));
                this.txDesc.setText(QuestTranslation.translate(q.getValue().getUnlocalisedDescription()));
                this.cvQuest.fitToWindow();
                this.refreshClaimAll();
                this.refreshDesigner();
            });
            this.cvLines.addPanel(btnLine);
            this.btnListRef.add(btnLine);
        }
        this.cvLines.refreshScrollBounds();
        this.scLines.setEnabled(this.cvLines.getScrollBounds().getHeight() > 0);
    }

    private void refreshQuestCompletion() {
        EntityPlayer player = this.mc.player;
        UUID playerUUId = QuestingAPI.getQuestingUUID(player);
        if(this.selectedLine != null) {
            int questsCompleted = 0;
            int totalQuests = 0;

            for(DBEntry<IQuestLineEntry> entry : this.selectedLine.getEntries()) {
                IQuest quest = QuestingAPI.getAPI(ApiReference.QUEST_DB).getValue(entry.getID());
                if(quest.getProperty(NativeProps.LOGIC_QUEST) == EnumLogic.XOR) {
                    totalQuests -= Math.max(0, quest.getRequirements().length - 1);
                }

                //Don't count hidden quests
                if(quest.getProperty(NativeProps.VISIBILITY) != EnumQuestVisibility.HIDDEN) {
                    ++totalQuests;
                    if(quest.isComplete(playerUUId)) ++questsCompleted;
                }
            }

            this.txCompletion.setText(QuestTranslation.translate("betterquesting.title.completion", questsCompleted, totalQuests));
        }
    }

    private void openQuestLine(DBEntry<IQuestLine> q) {
        this.selectedLine = q.getValue();
        selectedLineId = q.getID();

        for(int i = 0; i < this.btnListRef.size(); ++i) {
            this.btnListRef.get(i).setActive((this.visChapters.get(i).getSecond() & 4) == 0 && q.getID() != selectedLineId);
        }

        this.cvQuest.setQuestLine(q.getValue());
        this.icoChapter.setTexture(new OreDictTexture(1.0F, q.getValue().getProperty(NativeProps.ICON), false, true), null);
        this.txTitle.setText(QuestTranslation.translate(q.getValue().getUnlocalisedName()));
        this.refreshQuestCompletion();
        this.cvQuest.fitToWindow();

        this.refreshClaimAll();
        this.refreshDesigner();
    }
    
    private void refreshContent() {
        if(selectedLineId >= 0) {
            this.selectedLine = QuestLineDatabase.INSTANCE.getValue(selectedLineId);
            if(this.selectedLine == null) selectedLineId = -1;
        }
        else this.selectedLine = null;
        
        float zoom = this.cvQuest.getZoom();
        int sx = this.cvQuest.getScrollX();
        int sy = this.cvQuest.getScrollY();
        this.cvQuest.setQuestLine(this.selectedLine);
        this.cvQuest.setZoom(zoom);
        this.cvQuest.setScrollX(sx);
        this.cvQuest.setScrollY(sy);
        this.cvQuest.refreshScrollBounds();
        this.cvQuest.updatePanelScroll();
        
        if(selectedLine != null) {
            this.refreshQuestCompletion();
            this.txTitle.setText(QuestTranslation.translate(this.selectedLine.getUnlocalisedName()));
            this.txDesc.setText(QuestTranslation.translate(this.selectedLine.getUnlocalisedDescription()));
            this.icoChapter.setTexture(new OreDictTexture(1F, this.selectedLine.getProperty(NativeProps.ICON), false, true), null);
        }
        else {
            this.txTitle.setText("");
            this.txDesc.setText("");
            this.icoChapter.setTexture(null, null);
        }

        this.refreshClaimAll();
        this.refreshDesigner();
    }
    
    private void refreshClaimAll() {
        if(this.cvQuest.getQuestLine() == null || this.cvQuest.getQuestButtons().size() <= 0) {
            this.claimAll.setActive(false);
            this.claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
            return;
        }
        
        for(PanelButtonQuest btn : this.cvQuest.getQuestButtons()) {
            if(btn.getStoredValue().getValue().canClaim(this.mc.player)) {
                this.claimAll.setActive(true);
                this.claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorPulse(0xFFFFFFFF, 0xFF444444, 2F, 0F), 0);
                return;
            }
        }

        this.claimAll.setIcon(PresetIcon.ICON_CHEST_ALL.getTexture(), new GuiColorStatic(0xFF444444), 0);
        this.claimAll.setActive(false);
    }

    private void refreshDesigner() {
        //BQTweaker design button refresh
        if(this.btnDesign != null) this.btnDesign.setActive(this.selectedLine != null);
    }

    private void openSearch(PanelButton panelButton) {
        GuiQuestSearch guiQuestSearch = new GuiQuestSearch(this);
        guiQuestSearch.setCallback(
                entry -> {
                    this.openQuestLine(entry.getQuestLineEntry());
                    int selectedQuestId = entry.getQuest().getID();
                    Optional<PanelButtonQuest> targetQuestButton = this.cvQuest
                            .getQuestButtons()
                            .stream()
                            .filter(panelButtonQuest -> panelButtonQuest.getStoredValue().getID() == selectedQuestId)
                            .findFirst();
                    targetQuestButton.ifPresent(
                            panelButtonQuest -> {
                                GuiTextureColored newTexture = new GuiTextureColored(
                                        panelButtonQuest.txFrame,
                                        new GuiColorPulse(new GuiColorStatic(255, 220, 115, 255), new GuiColorStatic(255, 191, 0, 255), 1.0, 0.0F)
                                );
                                panelButtonQuest.setTextures(newTexture, newTexture, newTexture);
                            }
                    );
                }
        );
        this.mc.displayGuiScreen(guiQuestSearch);
    }
}