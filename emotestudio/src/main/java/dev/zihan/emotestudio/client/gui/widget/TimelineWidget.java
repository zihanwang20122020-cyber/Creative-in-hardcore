package dev.zihan.emotestudio.client.gui.widget;

import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePart;
import dev.zihan.emotestudio.emote.EmoteTrack;
import dev.zihan.emotestudio.emote.Keyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * The editor's timeline: a ruler, every part's keyframes as a row of marks, and a draggable
 * playhead.
 *
 * <p>Clicking near a keyframe of the selected part selects it; clicking anywhere else scrubs. That
 * one rule covers both jobs without a mode switch.
 */
public final class TimelineWidget extends AbstractWidget {
    private static final int BACKGROUND = 0xFF17171C;
    private static final int BORDER = 0xFF3C3C46;
    private static final int RULER = 0xFF4A4A56;
    private static final int ROW_LABEL = 0xFF9A9AA8;
    private static final int KEY_OTHER = 0xFF5C7CA8;
    private static final int KEY_SELECTED_TRACK = 0xFFFFC24D;
    private static final int KEY_ACTIVE = 0xFFFFFFFF;
    private static final int PLAYHEAD = 0xFFE05252;

    /** Click tolerance, in pixels, for grabbing a keyframe instead of scrubbing. */
    private static final int GRAB_RADIUS = 4;

    private static final int LABEL_WIDTH = 52;
    private static final int RULER_HEIGHT = 11;

    private final Emote emote;
    private final IntConsumer onSelectKeyframe;
    private final java.util.function.DoubleConsumer onScrub;
    private final java.util.function.Supplier<EmotePart> selectedPart;
    private final java.util.function.DoubleSupplier playhead;

    public TimelineWidget(int x, int y, int width, int height, Emote emote,
                          java.util.function.Supplier<EmotePart> selectedPart,
                          java.util.function.DoubleSupplier playhead,
                          IntConsumer onSelectKeyframe,
                          java.util.function.DoubleConsumer onScrub) {
        super(x, y, width, height, Component.literal("Timeline"));
        this.emote = emote;
        this.selectedPart = selectedPart;
        this.playhead = playhead;
        this.onSelectKeyframe = onSelectKeyframe;
        this.onScrub = onScrub;
    }

    private int trackLeft() {
        return getX() + LABEL_WIDTH;
    }

    private int trackWidth() {
        return width - LABEL_WIDTH - 4;
    }

    private int timeToX(float time) {
        float fraction = emote.lengthTicks() <= 0 ? 0.0F : time / emote.lengthTicks();
        return trackLeft() + Math.round(fraction * trackWidth());
    }

    private float xToTime(double x) {
        double fraction = Math.clamp((x - trackLeft()) / (double) trackWidth(), 0.0, 1.0);
        return (float) (fraction * emote.lengthTicks());
    }

    private int rowHeight() {
        return Math.max(6, (height - RULER_HEIGHT - 4) / EmotePart.values().length);
    }

    private int rowY(EmotePart part) {
        return getY() + RULER_HEIGHT + 2 + part.ordinal() * rowHeight();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        handlePointer(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        onScrub.accept(xToTime(event.x()));
    }

    private void handlePointer(double mouseX, double mouseY) {
        EmotePart part = selectedPart.get();
        EmoteTrack track = emote.track(part);

        // A click inside the selected part's row, close to a mark, means "select that keyframe".
        if (track != null && !track.isEmpty()) {
            int row = rowY(part);
            if (mouseY >= row - 1 && mouseY <= row + rowHeight()) {
                int best = -1;
                int bestDistance = GRAB_RADIUS + 1;
                for (int i = 0; i < track.size(); i++) {
                    int distance = (int) Math.abs(timeToX(track.get(i).time()) - mouseX);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = i;
                    }
                }
                if (best >= 0) {
                    onSelectKeyframe.accept(best);
                    onScrub.accept(track.get(best).time());
                    return;
                }
            }
        }
        onScrub.accept(xToTime(mouseX));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND);

        var font = Minecraft.getInstance().font;

        // Ruler: a mark every 10 ticks, labelled every second.
        for (int tick = 0; tick <= (int) emote.lengthTicks(); tick += 5) {
            int tickX = timeToX(tick);
            boolean major = tick % 20 == 0;
            graphics.fill(tickX, y + 2, tickX + 1, y + (major ? 9 : 6), RULER);
            if (major) {
                String label = (tick / 20) + "s";
                graphics.text(font, label, tickX + 2, y + 1, ROW_LABEL, false);
            }
        }

        EmotePart selected = selectedPart.get();
        int rowHeight = rowHeight();

        for (EmotePart part : EmotePart.values()) {
            int rowY = rowY(part);
            boolean isSelected = part == selected;
            graphics.text(font, part.key(), x + 3, rowY + (rowHeight - font.lineHeight) / 2,
                    isSelected ? KEY_SELECTED_TRACK : ROW_LABEL, false);

            EmoteTrack track = emote.track(part);
            if (track == null || track.isEmpty()) {
                continue;
            }
            graphics.fill(trackLeft(), rowY + rowHeight / 2, trackLeft() + trackWidth(),
                    rowY + rowHeight / 2 + 1, 0xFF2A2A32);
            for (Keyframe frame : track.keyframes()) {
                int keyX = timeToX(frame.time());
                int colour = isSelected ? KEY_SELECTED_TRACK : KEY_OTHER;
                graphics.fill(keyX - 2, rowY + 1, keyX + 3, rowY + rowHeight - 1, colour);
                if (isSelected) {
                    graphics.fill(keyX - 1, rowY + 2, keyX + 2, rowY + rowHeight - 2, KEY_ACTIVE);
                }
            }
        }

        int playheadX = timeToX((float) playhead.getAsDouble());
        graphics.fill(playheadX, y + 1, playheadX + 1, y + height - 1, PLAYHEAD);
        graphics.fill(playheadX - 2, y + 1, playheadX + 3, y + 4, PLAYHEAD);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.emotestudio.timeline"));
    }
}
