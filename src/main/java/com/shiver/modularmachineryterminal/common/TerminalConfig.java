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

    /**
     * 加载终端配置文件。
     * @param file 目标文件
     */
    public static void load(File file) {
        config = new Configuration(file);
        sync();
    }

    /**
     * 同步终端配置到 Forge 配置系统。
     */
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

    /**
     * 更新客户端传送相关配置。
     * @param enabled 按钮是否可用
     * @param requiredGameStage 传送所需阶段
     */
    public static void updateClientTeleportConfig(boolean enabled, String requiredGameStage) {
        clientTeleportEnabled = enabled;
        clientTeleportRequiredGameStage = requiredGameStage == null ? "" : requiredGameStage.trim();
    }

    /**
     * 更新客户端终端功能配置。
     * @param teleportEnabled 客户端传送是否启用
     * @param requiredGameStage 传送所需阶段
     * @param teamAccessEnabled 团队访问是否启用
     */
    public static void updateClientConfig(boolean teleportEnabled, String requiredGameStage, boolean teamAccessEnabled) {
        updateClientTeleportConfig(teleportEnabled, requiredGameStage);
        clientTeamAccessEnabled = teamAccessEnabled;
    }
}
