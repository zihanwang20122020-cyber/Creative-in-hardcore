package dev.zihan.emotestudio.client.gui;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial quick-pick. Aim with the mouse, release the key (or click) to play.
 *
 * <p>The wheel is filled from your favourites first, then your quick slots, then the first few
 * built-ins, so a fresh install still has something on it.
 */
public final class EmoteWheelScreen extends Screen {
    private static final int SLOTS = 8;
    private static final int RING_INNER = 40;
    private static final int RING_OUTER = 96;

    private static final int SLOT_BACKGROUND = 0xC017171C;
    private static final int SLOT_HOVER = 0xE03A6EA5;
    private static final int SLOT_BORDER = 0xFF3C3C46;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int TEXT_DIM = 0xFF9A9AA8;

    private final List<Emote> entries = new ArrayList<>();
    private int page;
    private int hovered = -1;

    public EmoteWheelScreen() {
        super(Component.translatable("gui.emotestudio.wheel"));
    }

    @Override
    protected void init() {
        entries.clear();
        EmoteRepository repository = EmoteRepository.get();

        for (String id : EmoteStudioClient.config().favourites()) {
            Emote emote = repository.byId(id);
            if (emote != null && !entries.contains(emote)) {
                entries.add(emote);
            }
        }
        for (int slot = 0; slot < 8; slot++) {
            Emote emote = repository.byId(EmoteStudioClient.config().quickSlot(slot));
            if (emote != null && !entries.contains(emote)) {
                entries.add(emote);
            }
        }
        for (Emote emote : repository.all()) {
            if (entries.size() >= SLOTS * 6) {
                break;
            }
            if (!entries.contains(emote)) {
                entries.add(emote);
            }
        }
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + SLOTS - 1) / SLOTS);
    }

    private Emote slotEmote(int slot) {
        int index = page * SLOTS + slot;
        return index < entries.size() ? entries.get(index) : null;
    }

    /** Slot under the pointer, or -1 when the pointer sits in the dead zone at the centre. */
    private int slotAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < RING_INNER * 0.6 || distance > RING_OUTER * 1.4) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0.0) {
            angle += 360.0;
        }
        double slice = 360.0 / SLOTS;
        return (int) Math.floor((angle + slice / 2.0) % 360.0 / slice);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hovered = slotAt(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = slotAt(event.x(), event.y());
        if (slot >= 0) {
            play(slot);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        page = Math.floorMod(page - (int) Math.signum(scrollY), pageCount());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
            play(key - GLFW.GLFW_KEY_1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            page = (page + 1) % pageCount();
            return true;
        }
        if (key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_SLASH) {
            minecraft.setScreenAndShow(new EmoteBrowserScreen(null));
            return true;
        }
        return super.keyPressed(event);
    }

    private void play(int slot) {
        Emote emote = slotEmote(slot);
        if (emote != null) {
            EmoteStudioClient.playOwnEmote(emote);
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hovered = slotAt(mouseX, mouseY);

        int centreX = width / 2;
        int centreY = height / 2;

        for (int slot = 0; slot < SLOTS; slot++) {
            Emote emote = slotEmote(slot);
            double angle = Math.toRadians(slot * (360.0 / SLOTS));
            int radius = (RING_INNER + RING_OUTER) / 2;
            int x = centreX + (int) Math.round(Math.sin(angle) * radius);
            int y = centreY - (int) Math.round(Math.cos(angle) * radius);

            int boxWidth = 84;
            int boxHeight = 20;
            int left = x - boxWidth / 2;
            int top = y - boxHeight / 2;

            boolean active = slot == hovered && emote != null;
            graphics.fill(left - 1, top - 1, left + boxWidth + 1, top + boxHeight + 1, SLOT_BORDER);
            graphics.fill(left, top, left + boxWidth, top + boxHeight, active ? SLOT_HOVER : SLOT_BACKGROUND);

            String label = emote == null ? "—" : trim(emote.name(), boxWidth - 8);
            graphics.centeredText(font, label, x, top + 6, emote == null ? TEXT_DIM : TEXT);
            graphics.text(font, Integer.toString(slot + 1), left + 3, top + 6, TEXT_DIM, false);
        }

        Emote focus = hovered >= 0 ? slotEmote(hovered) : null;
        if (focus != null) {
            graphics.centeredText(font, Component.literal(focus.name()), centreX, centreY - 10, TEXT);
            graphics.centeredText(font, focus.category(), centreX, centreY + 2, TEXT_DIM);
        } else {
            graphics.centeredText(font, getTitle(), centreX, centreY - 4, TEXT_DIM);
        }

        if (pageCount() > 1) {
            String pageLabel = (page + 1) + " / " + pageCount();
            graphics.centeredText(font, pageLabel, centreX, centreY + RING_OUTER + 22, TEXT_DIM);
        }
        graphics.centeredText(font, Component.translatable("gui.emotestudio.wheel.hint"),
                centreX, height - 18, TEXT_DIM);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private String trim(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
