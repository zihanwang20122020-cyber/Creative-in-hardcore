package dev.zihan.emotestudio.client.gui.editor;

import dev.zihan.emotestudio.client.EmoteStudioClient;
import dev.zihan.emotestudio.client.anim.EmoteAnimator;
import dev.zihan.emotestudio.client.gui.EmoteBrowserScreen;
import dev.zihan.emotestudio.client.gui.widget.TimelineWidget;
import dev.zihan.emotestudio.client.gui.widget.ValueSlider;
import dev.zihan.emotestudio.emote.Easing;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.EmotePart;
import dev.zihan.emotestudio.emote.EmotePose;
import dev.zihan.emotestudio.emote.EmoteTrack;
import dev.zihan.emotestudio.emote.Keyframe;
import dev.zihan.emotestudio.emote.PartTransform;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The animation editor.
 *
 * <p>Layout: part list on the left, live character in the middle, the selected keyframe's nine
 * channels on the right, and the timeline plus transport along the bottom. Everything edits the
 * emote in memory; nothing touches disk until Save.
 *
 * <p>The preview is not a separate renderer — it drives the same {@link EmoteAnimator} preview hook
 * the world uses, so the figure in the editor and the figure other players see cannot drift apart.
 */
public final class EmoteEditorScreen extends Screen {
    private static final int PANEL_BORDER = 0xFF3C3C46;
    private static final int PANEL_BACKGROUND = 0xFF17171C;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int TEXT_DIM = 0xFF9A9AA8;
    private static final int TEXT_ACCENT = 0xFFFFC24D;
    private static final int TEXT_WARN = 0xFFE07A5F;

    private static final String[] CHANNEL_LABELS = {
            "Rot X", "Rot Y", "Rot Z", "Pos X", "Pos Y", "Pos Z", "Scale X", "Scale Y", "Scale Z"};

    private final Emote emote;
    private final boolean overridesBuiltin;

    private EmotePart selectedPart = EmotePart.RIGHT_ARM;
    private int selectedKeyframe = -1;
    private float playhead;
    private boolean playing = true;

    private final EmotePose scratch = new EmotePose();
    private final ValueSlider[] channelSliders = new ValueSlider[9];
    private final Button[] partButtons = new Button[EmotePart.values().length];

    private EditBox nameBox;
    private EditBox idBox;
    private Button playButton;
    private Button loopButton;
    private Button easingButton;
    private Button deleteKeyButton;
    private TimelineWidget timeline;

    private String status = "";
    private int statusColour = TEXT_DIM;

    private EmoteEditorScreen(Emote emote, boolean overridesBuiltin) {
        super(Component.translatable("gui.emotestudio.editor"));
        this.emote = emote;
        this.overridesBuiltin = overridesBuiltin;
    }

    /** A blank emote with a single neutral keyframe, ready to be shaped. */
    public static EmoteEditorScreen forNewEmote() {
        Emote emote = new Emote(EmoteRepository.get().freeId("my_emote"), "My Emote");
        emote.setAuthor(net.minecraft.client.Minecraft.getInstance().getUser().getName());
        emote.setCategory("custom");
        emote.setLengthTicks(40.0F);
        emote.setLoop(true);
        emote.setSource(Emote.Source.USER);
        emote.trackOrCreate(EmotePart.RIGHT_ARM).put(Keyframe.at(0.0F));
        return new EmoteEditorScreen(emote, false);
    }

    /**
     * Opens an existing emote for editing. Built-ins are copied first: saving then writes a user
     * file with the same id, which takes priority over the one in the jar.
     */
    public static EmoteEditorScreen forExisting(Emote source) {
        Emote working = source.copy();
        boolean overrides = source.source() == Emote.Source.BUILTIN;
        if (source.source() != Emote.Source.USER) {
            working.setSource(Emote.Source.USER);
            working.setFile(null);
        }
        return new EmoteEditorScreen(working, overrides);
    }

    @Override
    protected void init() {
        int margin = 6;
        int topBar = 6;
        int partsWidth = 66;
        int slidersWidth = 118;

        int timelineHeight = 74;
        int contentTop = topBar + 22;
        int contentBottom = height - timelineHeight - margin;
        int contentHeight = Math.max(60, contentBottom - contentTop);

        // ---- top bar: identity and file actions
        nameBox = new EditBox(font, margin, topBar, 118, 16, Component.translatable("gui.emotestudio.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(emote.name());
        nameBox.setResponder(emote::setName);
        addWidget(nameBox);

        idBox = new EditBox(font, margin + 122, topBar, 108, 16, Component.translatable("gui.emotestudio.id"));
        idBox.setMaxLength(48);
        idBox.setValue(emote.id());
        idBox.setResponder(emote::setId);
        addWidget(idBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.save"), button -> save())
                .bounds(width - margin - 190, topBar, 60, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.browse"),
                        button -> minecraft.setScreenAndShow(new EmoteBrowserScreen(null)))
                .bounds(width - margin - 126, topBar, 60, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.close"), button -> onClose())
                .bounds(width - margin - 62, topBar, 56, 16).build());

        // ---- left column: which part am I animating
        int partButtonHeight = Math.min(18, contentHeight / EmotePart.values().length - 2);
        for (EmotePart part : EmotePart.values()) {
            int index = part.ordinal();
            Button button = Button.builder(Component.literal(part.displayName()), b -> selectPart(part))
                    .bounds(margin, contentTop + index * (partButtonHeight + 2), partsWidth, partButtonHeight)
                    .build();
            partButtons[index] = addRenderableWidget(button);
        }

        int partsBottom = contentTop + EmotePart.values().length * (partButtonHeight + 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.mirror"),
                        button -> mirrorSides())
                .bounds(margin, partsBottom + 2, partsWidth, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.clear_track"),
                        button -> clearTrack())
                .bounds(margin, partsBottom + 20, partsWidth, 16).build());

        // ---- right column: the nine channels of the selected keyframe
        int sliderX = width - margin - slidersWidth;
        int sliderHeight = 14;
        for (int channel = 0; channel < 9; channel++) {
            int index = channel;
            boolean isScale = channel >= 6;
            boolean isRotation = channel < 3;
            double min = isRotation ? -180.0 : (isScale ? 0.1 : -24.0);
            double max = isRotation ? 180.0 : (isScale ? 3.0 : 24.0);
            double step = isScale ? 0.05 : 0.5;
            String unit = isRotation ? "°" : (isScale ? "x" : "px");

            ValueSlider slider = new ValueSlider(sliderX, contentTop + channel * (sliderHeight + 2),
                    slidersWidth, sliderHeight, CHANNEL_LABELS[channel], min, max, step, unit,
                    isScale ? 1.0 : 0.0, value -> setChannel(index, (float) value));
            channelSliders[channel] = addRenderableWidget(slider);
        }

        int slidersBottom = contentTop + 9 * (sliderHeight + 2) + 4;
        addRenderableWidget(new ValueSlider(sliderX, slidersBottom, slidersWidth, sliderHeight,
                "Length", 5.0, 400.0, 1.0, "t", emote.lengthTicks(), value -> {
            emote.setLengthTicks((float) value);
            playhead = Math.min(playhead, emote.lengthTicks());
        }));
        addRenderableWidget(new ValueSlider(sliderX, slidersBottom + 16, slidersWidth, sliderHeight,
                "Blend in", 0.0, 40.0, 0.5, "t", emote.blendInTicks(),
                value -> emote.setBlendInTicks((float) value)));
        addRenderableWidget(new ValueSlider(sliderX, slidersBottom + 32, slidersWidth, sliderHeight,
                "Blend out", 0.0, 40.0, 0.5, "t", emote.blendOutTicks(),
                value -> emote.setBlendOutTicks((float) value)));
        addRenderableWidget(new ValueSlider(sliderX, slidersBottom + 48, slidersWidth, sliderHeight,
                "Speed", 0.1, 4.0, 0.05, "x", emote.speed(),
                value -> emote.setSpeed((float) value)));

        // ---- bottom: timeline and transport
        int timelineY = contentBottom + 2;
        timeline = new TimelineWidget(margin, timelineY, width - margin * 2, 46, emote,
                () -> selectedPart,
                () -> playhead,
                index -> selectedKeyframe = index,
                time -> {
                    playing = false;
                    playhead = (float) time;
                    selectedKeyframe = keyframeIndexAt(playhead);
                    syncSliders();
                    updateButtons();
                });
        addRenderableWidget(timeline);

        int transportY = timelineY + 48;
        int buttonWidth = 62;
        int gap = 2;
        int cursor = margin;

        playButton = addRenderableWidget(Button.builder(Component.literal("⏸"), button -> togglePlay())
                .bounds(cursor, transportY, 24, 18).build());
        cursor += 26;
        addRenderableWidget(Button.builder(Component.literal("⏮"), button -> {
                    playhead = 0.0F;
                    selectedKeyframe = keyframeIndexAt(playhead);
                    syncSliders();
                })
                .bounds(cursor, transportY, 24, 18).build());
        cursor += 28;

        addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.add_key"),
                        button -> addKeyframe())
                .bounds(cursor, transportY, buttonWidth, 18).build());
        cursor += buttonWidth + gap;
        deleteKeyButton = addRenderableWidget(Button.builder(Component.translatable("gui.emotestudio.del_key"),
                        button -> deleteKeyframe())
                .bounds(cursor, transportY, buttonWidth, 18).build());
        cursor += buttonWidth + gap;
        easingButton = addRenderableWidget(Button.builder(Component.literal("Ease"),
                        button -> cycleEasing(hasShiftDown() ? -1 : 1))
                .bounds(cursor, transportY, 104, 18).build());
        cursor += 106;
        loopButton = addRenderableWidget(Button.builder(Component.literal("Loop"),
                        button -> {
                            emote.setLoop(!emote.loop());
                            updateButtons();
                        })
                .bounds(cursor, transportY, buttonWidth, 18).build());

        selectPart(selectedPart);
        updateButtons();
    }

    private boolean hasShiftDown() {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void selectPart(EmotePart part) {
        selectedPart = part;
        selectedKeyframe = keyframeIndexAt(playhead);
        syncSliders();
        updateButtons();
    }

    private int keyframeIndexAt(float time) {
        EmoteTrack track = emote.track(selectedPart);
        if (track == null) {
            return -1;
        }
        // Snap onto any keyframe within half a tick of the playhead.
        for (int i = 0; i < track.size(); i++) {
            if (Math.abs(track.get(i).time() - time) <= 0.5F) {
                return i;
            }
        }
        return -1;
    }

    private Keyframe currentKeyframe() {
        EmoteTrack track = emote.track(selectedPart);
        if (track == null || selectedKeyframe < 0 || selectedKeyframe >= track.size()) {
            return null;
        }
        return track.get(selectedKeyframe);
    }

    /** Pushes the selected keyframe's values into the sliders, or the sampled pose when none is. */
    private void syncSliders() {
        Keyframe frame = currentKeyframe();
        if (frame == null) {
            emote.sample(playhead, scratch);
            PartTransform transform = scratch.get(selectedPart);
            float[] values = {transform.rotX, transform.rotY, transform.rotZ,
                    transform.posX, transform.posY, transform.posZ,
                    transform.scaleX, transform.scaleY, transform.scaleZ};
            for (int i = 0; i < 9; i++) {
                channelSliders[i].setValueSilently(values[i]);
            }
            return;
        }
        for (int i = 0; i < 9; i++) {
            channelSliders[i].setValueSilently(frame.channel(i));
        }
    }

    /**
     * Writes one channel. Moving a slider with no keyframe under the playhead creates one first,
     * seeded from the current pose, so an edit is never silently dropped.
     */
    private void setChannel(int channel, float value) {
        EmoteTrack track = emote.trackOrCreate(selectedPart);
        Keyframe frame = currentKeyframe();
        if (frame == null) {
            emote.sample(playhead, scratch);
            frame = scratch.get(selectedPart).toKeyframe(playhead, Easing.SMOOTH);
            selectedKeyframe = track.put(frame);
        }
        Keyframe updated = frame.withChannel(channel, value);
        track.remove(selectedKeyframe);
        selectedKeyframe = track.put(updated);
        playing = false;
        updateButtons();
    }

    private void addKeyframe() {
        EmoteTrack track = emote.trackOrCreate(selectedPart);
        emote.sample(playhead, scratch);
        Keyframe frame = scratch.get(selectedPart).toKeyframe(playhead, Easing.SMOOTH);
        selectedKeyframe = track.put(frame);
        playing = false;
        syncSliders();
        updateButtons();
        setStatus(Component.translatable("gui.emotestudio.key_added",
                String.format(java.util.Locale.ROOT, "%.1f", playhead)).getString(), TEXT_DIM);
    }

    private void deleteKeyframe() {
        EmoteTrack track = emote.track(selectedPart);
        if (track == null || selectedKeyframe < 0) {
            return;
        }
        track.remove(selectedKeyframe);
        if (track.isEmpty()) {
            emote.setTrack(selectedPart, null);
        }
        selectedKeyframe = keyframeIndexAt(playhead);
        syncSliders();
        updateButtons();
    }

    private void cycleEasing(int direction) {
        Keyframe frame = currentKeyframe();
        if (frame == null) {
            return;
        }
        Easing[] values = Easing.values();
        int index = Math.floorMod(frame.easing().ordinal() + direction, values.length);
        EmoteTrack track = emote.trackOrCreate(selectedPart);
        track.remove(selectedKeyframe);
        selectedKeyframe = track.put(frame.withEasing(values[index]));
        updateButtons();
    }

    /** Copies the right-side limbs onto the left ones, flipping the axes that need flipping. */
    private void mirrorSides() {
        mirrorTrack(EmotePart.RIGHT_ARM, EmotePart.LEFT_ARM);
        mirrorTrack(EmotePart.RIGHT_LEG, EmotePart.LEFT_LEG);
        syncSliders();
        setStatus(Component.translatable("gui.emotestudio.mirrored").getString(), TEXT_DIM);
    }

    private void mirrorTrack(EmotePart from, EmotePart to) {
        EmoteTrack source = emote.track(from);
        if (source == null || source.isEmpty()) {
            return;
        }
        EmoteTrack mirrored = new EmoteTrack();
        for (Keyframe frame : source.keyframes()) {
            // Sideways rotation and sideways offset swap hands; forward motion stays as it is.
            mirrored.put(new Keyframe(frame.time(),
                    frame.rotX(), -frame.rotY(), -frame.rotZ(),
                    -frame.posX(), frame.posY(), frame.posZ(),
                    frame.scaleX(), frame.scaleY(), frame.scaleZ(),
                    frame.easing()));
        }
        emote.setTrack(to, mirrored);
    }

    private void clearTrack() {
        emote.setTrack(selectedPart, null);
        selectedKeyframe = -1;
        syncSliders();
        updateButtons();
    }

    private void togglePlay() {
        playing = !playing;
        updateButtons();
    }

    private void updateButtons() {
        playButton.setMessage(Component.literal(playing ? "⏸" : "⏵"));
        loopButton.setMessage(Component.literal(emote.loop() ? "Loop: on" : "Loop: off"));

        Keyframe frame = currentKeyframe();
        easingButton.active = frame != null;
        easingButton.setMessage(Component.literal(
                frame == null ? "Ease: —" : "Ease: " + frame.easing().serialisedName()));
        deleteKeyButton.active = frame != null;

        for (EmotePart part : EmotePart.values()) {
            Button button = partButtons[part.ordinal()];
            boolean animated = emote.isAnimated(part);
            String prefix = part == selectedPart ? "▶ " : (animated ? "• " : "  ");
            button.setMessage(Component.literal(prefix + part.displayName()));
        }
    }

    private void save() {
        if (emote.id().isBlank()) {
            setStatus(Component.translatable("gui.emotestudio.need_id").getString(), TEXT_WARN);
            return;
        }
        if (emote.tracks().isEmpty()) {
            setStatus(Component.translatable("gui.emotestudio.need_keys").getString(), TEXT_WARN);
            return;
        }
        try {
            Path path = EmoteRepository.get().save(emote);
            setStatus(Component.translatable("gui.emotestudio.saved",
                    path.getFileName().toString()).getString(), TEXT_ACCENT);
        } catch (IOException e) {
            setStatus(Component.translatable("gui.emotestudio.save_failed", e.getMessage()).getString(), TEXT_WARN);
        }
    }

    private void setStatus(String message, int colour) {
        this.status = message;
        this.statusColour = colour;
    }

    @Override
    public void tick() {
        if (playing) {
            playhead += emote.speed();
            if (playhead >= emote.lengthTicks()) {
                playhead = emote.loop() ? playhead % emote.lengthTicks() : emote.lengthTicks();
                if (!emote.loop()) {
                    playing = false;
                    updateButtons();
                }
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (nameBox.isFocused() || idBox.isFocused()) {
            return super.keyPressed(event);
        }
        switch (key) {
            case GLFW.GLFW_KEY_SPACE -> {
                togglePlay();
                return true;
            }
            case GLFW.GLFW_KEY_K -> {
                addKeyframe();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                deleteKeyframe();
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                if (event.modifiers() != 0) {
                    save();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
                playing = false;
                playhead = Math.clamp(playhead + (key == GLFW.GLFW_KEY_RIGHT ? 1.0F : -1.0F),
                        0.0F, emote.lengthTicks());
                selectedKeyframe = keyframeIndexAt(playhead);
                syncSliders();
                updateButtons();
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                EmotePart[] parts = EmotePart.values();
                selectPart(parts[(selectedPart.ordinal() + 1) % parts.length]);
                return true;
            }
            default -> {
                // Fall through to the default handling below.
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Drive the live character from the edited emote at the current playhead.
        EmoteAnimator.get().setPreview(emote, playhead);

        int margin = 6;
        int previewLeft = margin + 72;
        int previewRight = width - margin - 124;
        int previewTop = 28;
        int previewBottom = height - 130;

        panel(graphics, previewLeft, previewTop, previewRight - previewLeft, previewBottom - previewTop);

        if (minecraft != null && minecraft.player != null && previewBottom - previewTop > 40) {
            int scale = Math.max(24, (previewBottom - previewTop) / 3);
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
                    previewLeft + 2, previewTop + 2, previewRight - 2, previewBottom - 2,
                    scale, 0.0625F, mouseX, mouseY, minecraft.player);
        }

        String header = String.format(java.util.Locale.ROOT, "%s  ·  t=%.1f / %.0f  ·  %d keys",
                selectedPart.displayName(), playhead, emote.lengthTicks(), emote.keyframeCount());
        graphics.text(font, header, previewLeft + 4, previewTop + 3, TEXT, false);

        if (overridesBuiltin) {
            graphics.text(font, Component.translatable("gui.emotestudio.overrides_builtin"),
                    previewLeft + 4, previewBottom - 20, TEXT_ACCENT, false);
        }
        graphics.text(font, Component.translatable("gui.emotestudio.shortcuts"),
                previewLeft + 4, previewBottom - 11, TEXT_DIM, false);

        if (!status.isEmpty()) {
            graphics.text(font, status, margin, height - 14, statusColour, false);
        }

        nameBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        idBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BACKGROUND);
    }

    @Override
    public void onClose() {
        EmoteAnimator.get().clearPreview();
        if (minecraft != null) {
            minecraft.setScreenAndShow(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
