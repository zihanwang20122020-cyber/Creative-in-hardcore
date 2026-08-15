package dev.zihan.emotestudio.client.gui;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.client.gui.editor.EmoteEditorScreen;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Searchable list of every emote, with a live preview of the highlighted one on your own character.
 *
 * <p>Selecting an emote previews it immediately; the preview is the same animation path the world
 * uses, so what you see here is exactly what other players will see.
 */
public final class EmoteBrowserScreen extends Screen {
    private static final int PANEL_BORDER = 0xFF3C3C46;
    private static final int PANEL_BACKGROUND = 0xFF17171C;
    private static final int ROW_HOVER = 0xFF262633;
    private static final int ROW_SELECTED = 0xFF3A6EA5;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int TEXT_DIM = 0xFF9A9AA8;
    private static final int TEXT_ACCENT = 0xFFFFC24D;

    private static final int ROW_HEIGHT = 13;
    private static final String ALL_CATEGORIES = "all";

    private final Screen parent;

    private EditBox searchBox;
    private Button categoryButton;
    private Button playButton;
    private Button favouriteButton;
    private Button editButton;
    private Button deleteButton;

    private List<Emote> visible = new ArrayList<>();
    private String category = ALL_CATEGORIES;
    private int selectedIndex;
    private int scroll;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;

    private String status = "";

    public EmoteBrowserScreen(Screen parent) {
        super(Component.translatable("gui.emotestudio.browser"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(width - 20, 420);
        int left = (width - panelWidth) / 2;
        int top = 34;
        int panelHeight = Math.min(height - top - 34, 200);

        listX = left;
        listY = top + 22;
        listWidth = panelWidth * 3 / 5;
        listHeight = panelHeight - 22;

        previewX = left + listWidth + 6;
        previewY = listY;
        previewWidth = panelWidth - listWidth - 6;
        previewHeight = listHeight - 26;

        searchBox = new EditBox(font, left + 1, top, listWidth - 2, 16,
                Component.translatable("gui.emotestudio.search"));
        searchBox.setHint(Component.translatable("gui.emotestudio.search"));
        searchBox.setResponder(text -> {
            scroll = 0;
            refresh();
        });
        addWidget(searchBox);

        categoryButton = Button.builder(categoryLabel(), button -> cycleCategory())
                .bounds(previewX, top, previewWidth, 16)
                .build();
        addRenderableWidget(categoryButton);

        int buttonsY = listY + listHeight - 20;
        int buttonWidth = (listWidth - 6) / 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.new"),
                        button -> minecraft.setScreenAndShow(EmoteEditorScreen.forNewEmote()))
                .bounds(listX, buttonsY, buttonWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.duplicate"),
                        button -> duplicateSelected())
                .bounds(listX + buttonWidth + 2, buttonsY, buttonWidth, 18).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.edit"),
                        button -> editSelected())
                .bounds(listX + (buttonWidth + 2) * 2, buttonsY, buttonWidth, 18).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.delete"),
                        button -> deleteSelected())
                .bounds(listX + (buttonWidth + 2) * 3, buttonsY, buttonWidth, 18).build());

        int sideY = previewY + previewHeight + 2;
        int halfWidth = (previewWidth - 2) / 2;
        playButton = addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.play"),
                        button -> playSelected())
                .bounds(previewX, sideY, halfWidth, 18).build());
        favouriteButton = addRenderableWidget(Button.builder(Component.literal("★"),
                        button -> toggleFavourite())
                .bounds(previewX + halfWidth + 2, sideY, halfWidth, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.reload"), button -> {
                    int count = EmoteRepository.get().reload();
                    status = Component.translatable("command.emotestudio.reloaded", count).getString();
                    refresh();
                })
                .bounds(left, top + panelHeight + 4, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.stop"),
                        button -> EmoteStudioClient.stopOwnEmote())
                .bounds(left + 94, top + panelHeight + 4, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.close"),
                        button -> onClose())
                .bounds(left + panelWidth - 90, top + panelHeight + 4, 90, 18).build());

        setInitialFocus(searchBox);
        refresh();
    }

    private Component categoryLabel() {
        return Component.translatable("gui.emotestudio.category",
                category.equals(ALL_CATEGORIES)
                        ? Component.translatable("gui.emotestudio.category.all").getString()
                        : category);
    }

    private void cycleCategory() {
        List<String> categories = new ArrayList<>();
        categories.add(ALL_CATEGORIES);
        categories.addAll(EmoteRepository.get().categories());
        int index = Math.max(0, categories.indexOf(category));
        category = categories.get((index + 1) % categories.size());
        categoryButton.setMessage(categoryLabel());
        scroll = 0;
        refresh();
    }

    private void refresh() {
        String query = searchBox == null ? "" : searchBox.getValue();
        String filter = category.equals(ALL_CATEGORIES) ? null : category;
        visible = EmoteRepository.get().search(query, filter);
        if (selectedIndex >= visible.size()) {
            selectedIndex = Math.max(0, visible.size() - 1);
        }
        updateButtons();
        previewSelected();
    }

    private Emote selected() {
        return selectedIndex >= 0 && selectedIndex < visible.size() ? visible.get(selectedIndex) : null;
    }

    private void updateButtons() {
        Emote emote = selected();
        boolean has = emote != null;
        playButton.active = has;
        favouriteButton.active = has;
        editButton.active = has;
        deleteButton.active = has && emote.editable();
        if (has) {
            favouriteButton.setMessage(Component.literal(
                    EmoteStudioClient.config().isFavourite(emote.id()) ? "★" : "☆"));
        }
    }

    /** Shows the highlighted emote on the preview model without broadcasting it. */
    private void previewSelected() {
        Emote emote = selected();
        if (emote == null) {
            EmoteAnimator.get().clearPreview();
            return;
        }
        EmoteAnimator.get().clearPreview();
        if (minecraft != null && minecraft.player != null) {
            EmoteAnimator.get().start(minecraft.player.getUUID(), emote, 1.0F);
        }
    }

    private void playSelected() {
        Emote emote = selected();
        if (emote != null) {
            EmoteStudioClient.playOwnEmote(emote);
            onClose();
        }
    }

    private void toggleFavourite() {
        Emote emote = selected();
        if (emote != null) {
            EmoteStudioClient.config().toggleFavourite(emote.id());
            updateButtons();
        }
    }

    private void editSelected() {
        Emote emote = selected();
        if (emote != null) {
            minecraft.setScreenAndShow(EmoteEditorScreen.forExisting(emote));
        }
    }

    private void duplicateSelected() {
        Emote emote = selected();
        if (emote == null) {
            return;
        }
        Emote copy = emote.copy();
        copy.setId(EmoteRepository.get().freeId(emote.id() + "_copy"));
        copy.setName(emote.name() + " (copy)");
        copy.setSource(Emote.Source.USER);
        copy.setFile(null);
        minecraft.setScreenAndShow(EmoteEditorScreen.forExisting(copy));
    }

    private void deleteSelected() {
        Emote emote = selected();
        if (emote == null || !emote.editable()) {
            return;
        }
        try {
            EmoteRepository.get().delete(emote);
            status = Component.translatable("gui.emotestudio.deleted", emote.name()).getString();
            refresh();
        } catch (IOException e) {
            status = e.getMessage();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isOverList(event.x(), event.y())) {
            int index = scroll + (int) ((event.y() - listY - 2) / ROW_HEIGHT);
            if (index >= 0 && index < visible.size()) {
                if (index == selectedIndex && doubleClick) {
                    playSelected();
                } else {
                    selectedIndex = index;
                    updateButtons();
                    previewSelected();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverList(double x, double y) {
        return x >= listX && x < listX + listWidth && y >= listY && y < listY + listHeight - 22;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverList(mouseX, mouseY)) {
            int rows = visibleRows();
            scroll = (int) Math.clamp(scroll - scrollY, 0, Math.max(0, visible.size() - rows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int visibleRows() {
        return Math.max(1, (listHeight - 24) / ROW_HEIGHT);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            int delta = key == GLFW.GLFW_KEY_DOWN ? 1 : -1;
            selectedIndex = (int) Math.clamp(selectedIndex + delta, 0, Math.max(0, visible.size() - 1));
            int rows = visibleRows();
            if (selectedIndex < scroll) {
                scroll = selectedIndex;
            } else if (selectedIndex >= scroll + rows) {
                scroll = selectedIndex - rows + 1;
            }
            updateButtons();
            previewSelected();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            playSelected();
            return true;
        }
        // Number keys bind the highlighted emote to a quick slot.
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8 && !searchBox.isFocused()) {
            Emote emote = selected();
            if (emote != null) {
                int slot = key - GLFW.GLFW_KEY_1;
                EmoteStudioClient.config().setQuickSlot(slot, emote.id());
                status = Component.translatable("gui.emotestudio.bound", emote.name(), slot + 1).getString();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        panel(graphics, listX, listY, listWidth, listHeight);
        panel(graphics, previewX, previewY, previewWidth, previewHeight);

        graphics.text(font, getTitle(), listX, listY - 34, TEXT, false);

        drawList(graphics, mouseX, mouseY);
        drawPreview(graphics, mouseX, mouseY);

        if (!status.isEmpty()) {
            graphics.text(font, status, listX, listY + listHeight + 26, TEXT_ACCENT, false);
        }

        searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BACKGROUND);
    }

    private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rows = visibleRows();
        int listBottom = listY + listHeight - 22;
        graphics.enableScissor(listX + 1, listY + 1, listX + listWidth - 1, listBottom);

        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= visible.size()) {
                break;
            }
            Emote emote = visible.get(index);
            int y = listY + 2 + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

            if (index == selectedIndex) {
                graphics.fill(listX + 1, y, listX + listWidth - 1, y + ROW_HEIGHT, ROW_SELECTED);
            } else if (hovered) {
                graphics.fill(listX + 1, y, listX + listWidth - 1, y + ROW_HEIGHT, ROW_HOVER);
            }

            String marker = EmoteStudioClient.config().isFavourite(emote.id()) ? "★ " : "";
            graphics.text(font, marker + emote.name(), listX + 4, y + 3, TEXT, false);

            String right = emote.editable() ? "✎" : emote.category();
            graphics.text(font, right, listX + listWidth - 6 - font.width(right), y + 3, TEXT_DIM, false);
        }
        graphics.disableScissor();

        String counter = visible.size() + " / " + EmoteRepository.get().all().size();
        graphics.text(font, counter, listX + 4, listBottom + 3, TEXT_DIM, false);

        // Scrollbar, only when there is something to scroll.
        if (visible.size() > rows) {
            int trackHeight = listBottom - listY - 2;
            int thumbHeight = Math.max(8, trackHeight * rows / visible.size());
            int thumbY = listY + 2 + (trackHeight - thumbHeight) * scroll / Math.max(1, visible.size() - rows);
            graphics.fill(listX + listWidth - 4, listY + 2, listX + listWidth - 2, listBottom, 0xFF23232B);
            graphics.fill(listX + listWidth - 4, thumbY, listX + listWidth - 2, thumbY + thumbHeight, 0xFF5A5A6A);
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Emote emote = selected();
        if (emote == null || minecraft == null || minecraft.player == null) {
            return;
        }

        int centreX = previewX + previewWidth / 2;
        int modelScale = Math.max(20, previewHeight / 4);
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
                previewX + 2, previewY + 2, previewX + previewWidth - 2, previewY + previewHeight - 36,
                modelScale, 0.0625F, mouseX, mouseY, minecraft.player);

        int textY = previewY + previewHeight - 34;
        graphics.centeredText(font, Component.literal(emote.name()), centreX, textY, TEXT_ACCENT);
        String meta = String.format("%.1fs · %d keys · %s",
                emote.lengthTicks() / 20.0F, emote.keyframeCount(), emote.loop() ? "loop" : "once");
        graphics.centeredText(font, meta, centreX, textY + 10, TEXT_DIM);
        if (!emote.description().isBlank()) {
            graphics.textWithWordWrap(font, Component.literal(emote.description()),
                    previewX + 4, textY + 21, previewWidth - 8, TEXT_DIM);
        }
    }

    @Override
    public void onClose() {
        EmoteAnimator.get().clearPreview();
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
