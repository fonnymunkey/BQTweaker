package bqtweaker.client.gui.panels;

import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import net.minecraft.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public class PanelTextBoxTooltip extends PanelTextBox {
    private String tooltip;

    public PanelTextBoxTooltip(IGuiRect rect, String text, boolean autofit) {
        super(rect, text, autofit);
    }

    public PanelTextBox setText(String text) {
        super.setText(text);
        this.tooltip = text;
        return this;
    }

    public List<String> getTooltip(int mx, int my) {
        if(!this.getTransform().contains(mx, my)) return null;
        return StringUtils.isNullOrEmpty(this.tooltip) ? null : Arrays.asList(this.tooltip.split("\n"));
    }
}