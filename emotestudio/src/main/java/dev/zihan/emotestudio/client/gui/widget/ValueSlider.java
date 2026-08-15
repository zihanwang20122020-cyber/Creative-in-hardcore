package dev.zihan.emotestudio.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;

/**
 * A labelled numeric slider with a readable value and keyboard nudging.
 *
 * <p>Vanilla's slider is built around a 0..1 fraction; the editor needs real units (degrees, pixels,
 * ticks) with a step size, so this keeps the value in its own units and only converts for drawing.
 */
public final class ValueSlider extends AbstractWidget {
    private static final int TRACK = 0xFF101014;
    private static final int TRACK_BORDER = 0xFF3C3C46;
    private static final int FILL = 0xFF3A6EA5;
    private static final int HANDLE = 0xFFE8E8F0;
    private static final int HANDLE_ACTIVE = 0xFFFFD780;

    private final String label;
    private final double min;
    private final double max;
    private final double step;
    private final String unit;
    private final DoubleConsumer onChange;

    private double value;
    private boolean dragging;

    public ValueSlider(int x, int y, int width, int height, String label,
                       double min, double max, double step, String unit,
                       double initial, DoubleConsumer onChange) {
        super(x, y, width, height, Component.literal(label));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit == null ? "" : unit;
        this.onChange = onChange;
        this.value = clamp(initial);
    }

    public double value() {
        return value;
    }

    /** Sets the value without firing {@code onChange}, for when the selection changes. */
    public void setValueSilently(double newValue) {
        this.value = clamp(newValue);
    }

    private double clamp(double raw) {
        double clamped = Math.clamp(raw, min, max);
        if (step > 0.0) {
            clamped = Math.round(clamped / step) * step;
        }
        return clamped;
    }

    private void setValue(double newValue) {
        double clamped = clamp(newValue);
        if (clamped != value) {
            value = clamped;
            onChange.accept(value);
        }
    }

    private double fraction() {
        return max - min <= 1.0E-6 ? 0.0 : (value - min) / (max - min);
    }

    private void applyFromMouse(double mouseX) {
        double fraction = Math.clamp((mouseX - (getX() + 2)) / (double) (width - 4), 0.0, 1.0);
        setValue(min + fraction * (max - min));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        dragging = true;
        applyFromMouse(event.x());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        applyFromMouse(event.x());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        setValue(value + Math.signum(scrollY) * Math.max(step, (max - min) / 100.0));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        double nudge = Math.max(step, (max - min) / 200.0);
        if (event.modifiers() != 0) {
            nudge *= 10.0;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            setValue(value - nudge);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            setValue(value + nudge);
            return true;
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, TRACK_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, TRACK);

        int fillWidth = (int) ((width - 4) * fraction());
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, FILL);
        }

        int handleX = x + 2 + fillWidth;
        graphics.fill(handleX - 1, y + 1, handleX + 2, y + height - 1,
                dragging || isHovered() ? HANDLE_ACTIVE : HANDLE);

        var font = Minecraft.getInstance().font;
        String text = label + ": " + format(value) + unit;
        graphics.text(font, text, x + 4, y + (height - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
    }

    private String format(double raw) {
        if (step >= 1.0) {
            return Integer.toString((int) Math.round(raw));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", raw);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(label + " " + format(value) + unit));
    }
}
