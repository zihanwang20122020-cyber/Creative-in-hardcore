package dev.zihan.emotestudio.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.zihan.emotestudio.client.gui.EmoteBrowserScreen;
import dev.zihan.emotestudio.client.gui.editor.EmoteEditorScreen;
import dev.zihan.emotestudio.emote.Emote;
import dev.zihan.emotestudio.emote.io.EmoteRepository;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * {@code /emote} and friends. These are client commands: they never reach the server, so they work
 * identically in singleplayer, on a LAN world and on a server that has never heard of this mod.
 */
public final class EmoteClientCommands {
    private EmoteClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("emote")
                    .executes(context -> {
                        Minecraft.getInstance().execute(() ->
                                Minecraft.getInstance().setScreenAndShow(new EmoteBrowserScreen(null)));
                        return 1;
                    })
                    .then(ClientCommands.literal("play")
                            .then(ClientCommands.argument("id", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
                                        for (String id : EmoteRepository.get().ids()) {
                                            if (id.startsWith(remaining)) {
                                                builder.suggest(id);
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String id = StringArgumentType.getString(context, "id");
                                        if (!EmoteStudioClient.playOwnEmote(id)) {
                                            context.getSource().sendError(Component
                                                    .translatable("command.emotestudio.unknown", id));
                                            return 0;
                                        }
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("stop").executes(context -> {
                        EmoteStudioClient.stopOwnEmote();
                        return 1;
                    }))
                    .then(ClientCommands.literal("list").executes(context -> {
                        List<Emote> all = EmoteRepository.get().all();
                        context.getSource().sendFeedback(Component
                                .translatable("command.emotestudio.count", all.size())
                                .withStyle(ChatFormatting.GOLD));
                        for (String category : EmoteRepository.get().categories()) {
                            long count = all.stream().filter(e -> e.category().equals(category)).count();
                            context.getSource().sendFeedback(Component.literal(" · " + category + " (" + count + ")")
                                    .withStyle(ChatFormatting.GRAY));
                        }
                        return 1;
                    }))
                    .then(ClientCommands.literal("reload").executes(context -> {
                        int count = EmoteRepository.get().reload();
                        context.getSource().sendFeedback(Component
                                .translatable("command.emotestudio.reloaded", count)
                                .withStyle(ChatFormatting.GREEN));
                        return 1;
                    }))
                    .then(ClientCommands.literal("editor")
                            .executes(context -> {
                                Minecraft.getInstance().execute(() ->
                                        Minecraft.getInstance().setScreenAndShow(EmoteEditorScreen.forNewEmote()));
                                return 1;
                            })
                            .then(ClientCommands.argument("id", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
                                        for (String id : EmoteRepository.get().ids()) {
                                            if (id.startsWith(remaining)) {
                                                builder.suggest(id);
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String id = StringArgumentType.getString(context, "id");
                                        Emote emote = EmoteRepository.get().byId(id);
                                        if (emote == null) {
                                            context.getSource().sendError(Component
                                                    .translatable("command.emotestudio.unknown", id));
                                            return 0;
                                        }
                                        Minecraft.getInstance().execute(() -> Minecraft.getInstance()
                                                .setScreenAndShow(EmoteEditorScreen.forExisting(emote)));
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("chatsync")
                            .executes(context -> {
                                context.getSource().sendFeedback(Component
                                        .translatable(EmoteStudioClient.config().chatSync()
                                                ? "command.emotestudio.chatsync.on"
                                                : "command.emotestudio.chatsync.off")
                                        .withStyle(ChatFormatting.AQUA));
                                return 1;
                            })
                            .then(ClientCommands.literal("on").executes(context -> {
                                EmoteStudioClient.config().setChatSync(true);
                                context.getSource().sendFeedback(Component
                                        .translatable("command.emotestudio.chatsync.on")
                                        .withStyle(ChatFormatting.GREEN));
                                return 1;
                            }))
                            .then(ClientCommands.literal("off").executes(context -> {
                                EmoteStudioClient.config().setChatSync(false);
                                context.getSource().sendFeedback(Component
                                        .translatable("command.emotestudio.chatsync.off")
                                        .withStyle(ChatFormatting.YELLOW));
                                return 1;
                            })))
                    .then(ClientCommands.literal("folder").executes(context -> {
                        context.getSource().sendFeedback(Component
                                .translatable("command.emotestudio.folder",
                                        EmoteRepository.get().userFolder().toAbsolutePath().toString())
                                .withStyle(ChatFormatting.AQUA));
                        return 1;
                    }));

            dispatcher.register(root);
            dispatcher.register(ClientCommands.literal("emotes").redirect(dispatcher.register(root)));
        });
    }
}
