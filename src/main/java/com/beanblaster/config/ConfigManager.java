package com.beanblaster.config;

import com.beanblaster.BeanBlasterClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ConfigManager {
    public static final String FILE_NAME = "bean-blaster-calculator.json";

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData data = new ConfigData();

    private ConfigManager() {
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(FILE.getParent());

            if (!Files.exists(FILE)) {
                data = new ConfigData();
                write();
                return;
            }

            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                data = new ConfigData();
                write();
                return;
            }

            ConfigData loaded = GSON.fromJson(json, ConfigData.class);
            data = loaded == null ? new ConfigData() : loaded;

            if (data.setups == null) {
                data.setups = new LinkedHashMap<>();
            }

            data.setups.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        } catch (Exception exception) {
            BeanBlasterClient.LOGGER.warn(
                    "The Bean Blaster config is invalid. It will be backed up and recreated.",
                    exception
            );
            backupBrokenFile();
            data = new ConfigData();

            try {
                write();
            } catch (IOException writeException) {
                BeanBlasterClient.LOGGER.error("Could not recreate the Bean Blaster config.", writeException);
            }
        }
    }

    public static synchronized Optional<SavedPosition> get(MinecraftClient client) {
        Optional<Context> context = resolveContext(client);
        if (context.isEmpty()) {
            return Optional.empty();
        }

        Map<String, SavedPosition> dimensions = data.setups.get(context.get().worldId());
        if (dimensions == null) {
            return Optional.empty();
        }

        SavedPosition position = dimensions.get(context.get().dimension());
        if (!isValid(position)) {
            return Optional.empty();
        }

        return Optional.of(new SavedPosition(position.x, position.y, position.z));
    }

    public static synchronized boolean save(MinecraftClient client, BlockPos blockPos) {
        Optional<Context> context = resolveContext(client);
        if (context.isEmpty()) {
            return false;
        }

        Map<String, SavedPosition> dimensions = data.setups.computeIfAbsent(
                context.get().worldId(),
                ignored -> new LinkedHashMap<>()
        );

        String dimension = context.get().dimension();
        SavedPosition previous = dimensions.put(
                dimension,
                new SavedPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ())
        );

        try {
            write();
            return true;
        } catch (IOException exception) {
            if (previous == null) {
                dimensions.remove(dimension);
            } else {
                dimensions.put(dimension, previous);
            }

            BeanBlasterClient.LOGGER.error("Could not save the Bean Blaster setup.", exception);
            return false;
        }
    }

    public static Optional<Context> resolveContext(MinecraftClient client) {
        if (client == null || client.world == null) {
            return Optional.empty();
        }

        String dimension = client.world.getRegistryKey().getValue().toString();

        if (client.isInSingleplayer() && client.getServer() != null) {
            Path root = client.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
            Path fileName = root.getFileName();
            String saveName = fileName == null ? root.toString() : fileName.toString();

            return Optional.of(new Context(
                    "singleplayer:" + saveName,
                    dimension,
                    saveName
            ));
        }

        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.address != null && !server.address.isBlank()) {
            String address = server.address.trim().toLowerCase(Locale.ROOT);

            return Optional.of(new Context(
                    "multiplayer:" + address,
                    dimension,
                    address
            ));
        }

        return Optional.empty();
    }

    private static void write() throws IOException {
        Files.createDirectories(FILE.getParent());

        Path temporary = FILE.resolveSibling(FILE_NAME + ".tmp");
        Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);

        try {
            Files.move(
                    temporary,
                    FILE,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void backupBrokenFile() {
        if (!Files.exists(FILE)) {
            return;
        }

        Path backup = FILE.resolveSibling(FILE_NAME + ".broken-" + Instant.now().toEpochMilli());

        try {
            Files.move(FILE, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            BeanBlasterClient.LOGGER.warn("Could not back up the broken config file.", exception);
        }
    }

    private static boolean isValid(SavedPosition position) {
        return position != null
                && position.x >= -30_000_000
                && position.x <= 30_000_000
                && position.z >= -30_000_000
                && position.z <= 30_000_000
                && position.y >= -2_048
                && position.y <= 2_048;
    }

    private static final class ConfigData {
        int version = 1;
        Map<String, Map<String, SavedPosition>> setups = new LinkedHashMap<>();
    }

    public static final class SavedPosition {
        public int x;
        public int y;
        public int z;

        public SavedPosition() {
        }

        public SavedPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public record Context(String worldId, String dimension, String displayName) {
    }
}
