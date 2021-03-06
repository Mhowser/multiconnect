package net.earthcomputer.multiconnect.protocols.v1_12_2;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import net.earthcomputer.multiconnect.protocols.v1_12_2.command.Commands_1_12_2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.packet.CommandSuggestionsS2CPacket;
import net.minecraft.client.network.packet.CommandTreeS2CPacket;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.network.packet.RequestCommandCompletionsC2SPacket;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;

public class TabCompletionManager {

    private static final Queue<Entry> entries = new ArrayDeque<>();

    public static void addTabCompletionRequest(int id, String message) {
        synchronized (entries) {
            entries.add(new Entry(id, message));
        }
    }

    public static Entry nextEntry() {
        synchronized (entries) {
            return entries.poll();
        }
    }

    public static void requestCommandList() {
        assert MinecraftClient.getInstance().getNetworkHandler() != null;
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(-1, "/"));
    }

    public static boolean handleCommandList(CommandSuggestionsS2CPacket packet) {
        if (packet.getCompletionId() != -1)
            return false;
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        Commands_1_12_2.register(dispatcher, packet.getSuggestions().getList().stream()
                .map(Suggestion::getText)
                .filter(str -> !str.isEmpty())
                .map(str -> str.substring(1))
                .collect(Collectors.toSet()));
        assert MinecraftClient.getInstance().getNetworkHandler() != null;
        MinecraftClient.getInstance().getNetworkHandler().onCommandTree(new CommandTreeS2CPacket(dispatcher.getRoot()));
        return true;
    }

    public static final class Entry {
        private final int id;
        private final String message;

        public Entry(int id, String message) {
            this.id = id;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

}
