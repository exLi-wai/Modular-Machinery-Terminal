package com.shiver.modularmachineryterminal.common;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class TerminalConfig {

    public static boolean teleportEnabled = true;
    public static String teleportRequiredGameStage = "";
    public static boolean teamAccessEnabled = true;

    public static boolean clientTeleportEnabled = true;
    public static String clientTeleportRequiredGameStage = "";
    public static boolean clientTeamAccessEnabled = true;

    private static Configuration config;

    public static void load(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        if (config == null) {
            return;
        }

        config.load();
        teleportEnabled = config.getBoolean(
                "teleportEnabled",
                "teleport",
                true,
                "Whether the terminal can teleport players to the controller."
        );
        teleportRequiredGameStage = config.getString(
                "teleportRequiredGameStage",
                "teleport",
                "",
                "GameStage required to use controller teleport. Empty means no stage is required."
        ).trim();
        teamAccessEnabled = config.getBoolean(
                "teamAccessEnabled",
                "team",
                true,
                "Whether players can access controllers owned by FTB Utilities teammates."
        );

        if (config.hasChanged()) {
            config.save();
        }

        updateClientConfig(teleportEnabled, teleportRequiredGameStage, teamAccessEnabled);
    }

    public static void updateClientTeleportConfig(boolean enabled, String requiredGameStage) {
        clientTeleportEnabled = enabled;
        clientTeleportRequiredGameStage = requiredGameStage == null ? "" : requiredGameStage.trim();
    }

    public static void updateClientConfig(boolean teleportEnabled, String requiredGameStage, boolean teamAccessEnabled) {
        updateClientTeleportConfig(teleportEnabled, requiredGameStage);
        clientTeamAccessEnabled = teamAccessEnabled;
    }
}
