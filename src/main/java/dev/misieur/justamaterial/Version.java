package dev.misieur.justamaterial;

import io.papermc.paper.ServerBuildInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum Version {
    V1_21,
    V1_21_4,
    V1_21_5,
    V1_21_6,
    V1_21_9,
    V1_21_11;

    private static Version version;

    public @NotNull String toString() {
        return name().replace("V", "").replace('_', '.');
    }

    @Contract(pure = true)
    public static Version fromString(@NotNull String version) {
        return switch (version) {
            case "1.21", "1.21.1", "1.21.2", "1.21.3" -> V1_21;
            case "1.21.4" -> V1_21_4;
            case "1.21.5" -> V1_21_5;
            case "1.21.6", "1.21.7", "1.21.8" -> V1_21_6;
            case "1.21.9", "1.21.10" -> V1_21_9;
            case "1.21.11" -> V1_21_11;
            default -> throw new IllegalStateException("Unsupported version: " + version);
        };
    }

    public static Version get() {
        return version != null ? version : (version = fromString(ServerBuildInfo.buildInfo().minecraftVersionId()));
    }

}
