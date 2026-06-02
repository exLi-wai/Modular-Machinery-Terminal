package com.shiver.modularmachineryterminal.client.gui;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.FTBUtilitiesCompat;
import com.shiver.modularmachineryterminal.common.GameStagesCompat;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.OutputInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import com.shiver.modularmachineryterminal.common.TerminalConfig;
import com.shiver.modularmachineryterminal.common.ThreadInfo;
import com.shiver.modularmachineryterminal.network.PacketRequestDynamic;
import com.shiver.modularmachineryterminal.network.PacketRequestFullList;
import com.shiver.modularmachineryterminal.network.PacketOpenMachineComponentGui;
import com.shiver.modularmachineryterminal.network.PacketTeleportToMachine;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GuiTerminal extends GuiScreen {

    private static final int GUI_LAYOUT_WIDTH = 292;
    private static final int GUI_EXTRA_RIGHT_WIDTH = 12;
    private static final int GUI_WIDTH = GUI_LAYOUT_WIDTH + GUI_EXTRA_RIGHT_WIDTH;
    private static final int GUI_HEIGHT = 212;
    private static final int LIST_WIDTH = 126;
    private static final int ROW_HEIGHT = 24;
    private static final int CONTENT_TOP = 50;
    private static final int DETAIL_WIDTH = 134;
    private static final int LIST_ROW_WIDTH = LIST_WIDTH - 8;
    private static final Method JEC_CONTAINS = findJecContains();

    private GuiTextField searchField;
    private static MachineKey selected;
    private static SortMode sortMode = SortMode.NAME;
    private static boolean descending;
    private static boolean showTeamControllers = true;
    private static int scroll;
    private static int threadScroll;
    private static boolean settingsLoaded;
    private final Set<MachineKey> pinnedMachines = new HashSet<>();
    private String currentTooltip;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int left = guiLeft();
        int top = guiTop();
        if (!settingsLoaded) {
            loadSettings();
            settingsLoaded = true;
        }
        searchField = new GuiTextField(0, fontRenderer, left + 8, top + 32, 76, 14);
        searchField.setMaxStringLength(40);
        loadPinnedMachines();
        buttonList.clear();
        TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList(showTeamControllers));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        if (mc.player.ticksExisted % 20 == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList(showTeamControllers));
        }
        if (mc.player.ticksExisted % 4 == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketRequestDynamic(dynamicKeys(), showTeamControllers));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        currentTooltip = null;
        drawDefaultBackground();
        int left = guiLeft();
        int top = guiTop();
        drawPanel(left, top);
        drawHeader(left, top);
        drawLegend(left, top);
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.search"), left + 12, top + 35, 0xFF6F7982);
        }
        drawControls(left, top, mouseX, mouseY);
        drawMachineList(left, top, mouseX, mouseY);
        drawDetails(left, top, mouseX, mouseY);
        drawActionButtons(left, top, mouseX, mouseY);
        drawActionTooltip(left, top, mouseX, mouseY);
        if (currentTooltip != null) {
            drawHoveringText(Arrays.asList(currentTooltip.split("\n")), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        int left = guiLeft();
        int top = guiTop();
        MachineInfo machine = selectedMachine();

        if (mouseButton == 0 && inside(mouseX, mouseY, teamControllersX(left), top + 32, 14, 14)) {
            if (canToggleTeamControllers()) {
                showTeamControllers = !showTeamControllers;
                scroll = 0;
                saveSettings();
                TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList(showTeamControllers));
            }
            return;
        }

        if (machine != null && mouseButton == 0) {
            if (inside(mouseX, mouseY, actionButtonX(left), actionButtonY(top, 0), actionButtonSize(), actionButtonSize())) {
                togglePinned(machine.key);
                scroll = 0;
                return;
            }
            if (inside(mouseX, mouseY, actionButtonX(left), actionButtonY(top, 1), actionButtonSize(), actionButtonSize())) {
                if (canTeleport()) {
                    TerminalNetwork.CHANNEL.sendToServer(new PacketTeleportToMachine(machine.key));
                }
                return;
            }
            for (int i = 2; i < 8; i++) {
                if (inside(mouseX, mouseY, actionButtonX(left), actionButtonY(top, i), actionButtonSize(), actionButtonSize())) {
                    if (machine.loaded) {
                        TerminalNetwork.CHANNEL.sendToServer(new PacketOpenMachineComponentGui(machine.key, componentGroup(i), 0));
                    }
                    return;
                }
            }
        }

        if (inside(mouseX, mouseY, sortX(left), top + 32, 64, 14)) {
            sortMode = sortMode.next();
            scroll = 0;
            saveSettings();
            return;
        }
        if (inside(mouseX, mouseY, directionX(left), top + 32, 30, 14)) {
            descending = !descending;
            scroll = 0;
            saveSettings();
            return;
        }

        int listX = left + 10;
        int listY = top + CONTENT_TOP + 6;
        List<MachineInfo> visible = visibleMachines();
        int rows = Math.min(visibleRowCount(), visible.size() - scroll);
        for (int i = 0; i < rows; i++) {
            int y = listY + i * ROW_HEIGHT;
            if (inside(mouseX, mouseY, listX, y, LIST_ROW_WIDTH, ROW_HEIGHT - 2)) {
                selected = visible.get(scroll + i).key;
                threadScroll = 0;
                TerminalNetwork.CHANNEL.sendToServer(new PacketRequestDynamic(dynamicKeys(), showTeamControllers));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            scroll = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && overListPanel()) {
            List<MachineInfo> visible = visibleMachines();
            int maxScroll = Math.max(0, visible.size() - visibleRowCount());
            scroll += wheel < 0 ? 1 : -1;
            if (scroll < 0) {
                scroll = 0;
            }
            if (scroll > maxScroll) {
                scroll = maxScroll;
            }
        } else if (wheel != 0 && overDetailPanel()) {
            MachineInfo machine = selectedMachine();
            if (machine != null) {
                int maxScroll = Math.max(0, machine.threads.size() - visibleThreadRows(threadStartY()));
                threadScroll += wheel < 0 ? 1 : -1;
                if (threadScroll < 0) {
                    threadScroll = 0;
                }
                if (threadScroll > maxScroll) {
                    threadScroll = maxScroll;
                }
            }
        }
    }

    private void drawPanel(int left, int top) {
        drawRect(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xEE1C2024);
        drawRect(left + 3, top + 3, left + GUI_WIDTH - 3, top + GUI_HEIGHT - 3, 0xFF2B3036);
        drawRect(left + 6, top + CONTENT_TOP, left + LIST_WIDTH + 12, top + GUI_HEIGHT - 8, 0xFF14181C);
        drawRect(left + LIST_WIDTH + 16, top + CONTENT_TOP, left + GUI_LAYOUT_WIDTH - 8, top + GUI_HEIGHT - 8, 0xFF14181C);
        drawRect(left + 6, top + 28, legendX(left) - 8, top + 30, 0xFF4A545F);
    }

    private void drawHeader(int left, int top) {
        fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.title"), left + 8, top + 7, 0xDDEEFF);
        SummaryInfo summary = ClientTerminalData.getSummary();
        int y = top + 18;
        drawStat(left + 8, y, "gui.modular_machinery_terminal.total", summary.total, 0xFFBFC7CF);
        drawStat(left + 64, y, "gui.modular_machinery_terminal.loaded", summary.loaded, 0xFF69A7FF);
        drawStat(left + 122, y, "gui.modular_machinery_terminal.formed", summary.formed, 0xFF68D06E);
        drawStat(left + 184, y, "gui.modular_machinery_terminal.running", summary.running, 0xFF5FE69A);
    }

    private void drawStat(int x, int y, String key, int value, int color) {
        fontRenderer.drawString(I18n.format(key) + " " + value, x, y, color);
    }

    private void drawLegend(int left, int top) {
        int x = legendX(left) + 2;
        int y = top + 9;
        drawLegendItem(x, y, 0xFF43E06D, I18n.format("gui.modular_machinery_terminal.legend.running"));
        drawLegendItem(x, y + 9, 0xFFE2C84A, I18n.format("gui.modular_machinery_terminal.legend.idle"));
        drawLegendItem(x, y + 18, 0xFFE05C43, I18n.format("gui.modular_machinery_terminal.legend.invalid"));
        drawLegendItem(x, y + 27, 0xFF777777, I18n.format("gui.modular_machinery_terminal.legend.unloaded"));
    }

    private void drawLegendItem(int x, int y, int color, String text) {
        drawRect(x, y + 1, x + 2, y + 7, color);
        fontRenderer.drawString(text, x + 5, y, 0xFFBFC7CF);
    }

    private void drawControls(int left, int top, int mouseX, int mouseY) {
        drawSmallButton(sortX(left), top + 32, 64, 14, sortLabel(), inside(mouseX, mouseY, sortX(left), top + 32, 64, 14));
        drawSmallButton(directionX(left), top + 32, 30, 14, directionLabel(), inside(mouseX, mouseY, directionX(left), top + 32, 30, 14));
        boolean teamHovered = inside(mouseX, mouseY, teamControllersX(left), top + 32, 14, 14);
        drawSmallButton(teamControllersX(left), top + 32, 14, 14, "F", teamHovered);
        if (teamHovered) {
            currentTooltip = teamControllersTooltip();
        }
    }

    private void drawSmallButton(int x, int y, int width, int height, String text, boolean hovered) {
        int border = hovered ? 0xFF8DA5B8 : 0xFF5E6973;
        int fill = hovered ? 0xFF394650 : 0xFF273039;
        drawRect(x, y, x + width, y + height, border);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        String label = trim(text, width - 6);
        int textWidth = fontRenderer.getStringWidth(label);
        fontRenderer.drawString(label, x + (width - textWidth) / 2 + 1, y + 3, 0xFFD8DEE5);
    }

    private void drawActionButtons(int left, int top, int mouseX, int mouseY) {
        MachineInfo machine = selectedMachine();
        int x = actionButtonX(left);
        for (int i = 0; i < 8; i++) {
            boolean hovered = inside(mouseX, mouseY, x, actionButtonY(top, i), actionButtonSize(), actionButtonSize());
            drawIconButton(x, actionButtonY(top, i), actionButtonSize(), actionButtonSize(), actionButtonText(i, machine), hovered, actionEnabled(i, machine));
        }
    }

    private void drawIconButton(int x, int y, int width, int height, String text, boolean hovered, boolean enabled) {
        int border = !enabled ? 0xFF434A50 : (hovered ? 0xFF8DA5B8 : 0xFF5E6973);
        int fill = !enabled ? 0xFF20262B : (hovered ? 0xFF394650 : 0xFF273039);
        int color = enabled ? 0xFFD8DEE5 : 0xFF6F7982;
        drawRect(x, y, x + width, y + height, border);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        int textWidth = fontRenderer.getStringWidth(text);
        if (textWidth > 0) textWidth -= 1;
        fontRenderer.drawString(text, x + (width - textWidth) / 2, y + 2, color);
    }

    private boolean actionEnabled(int index, MachineInfo machine) {
        return index < 2 || machine == null || machine.loaded;
    }

    private String actionButtonText(int index, MachineInfo machine) {
        if (index == 0) return "↑";
        if (index == 1) return "T";
        if (index == 2) return "C";
        if (index == 3) return "I";
        if (index == 4) return "O";
        if (index == 5) return "M";
        if (index == 6) return "U";
        if (index == 7) return "S";
        return "";
    }

    private void drawActionTooltip(int left, int top, int mouseX, int mouseY) {
        for (int i = 0; i < 8; i++) {
            if (inside(mouseX, mouseY, actionButtonX(left), actionButtonY(top, i), actionButtonSize(), actionButtonSize())) {
                currentTooltip = actionTooltip(i);
                return;
            }
        }
    }

    private String actionTooltip(int index) {
        MachineInfo machine = selectedMachine();
        if (index >= 2 && machine != null && !machine.loaded) {
            return I18n.format("gui.modular_machinery_terminal.machine_unloaded");
        }
        if (index == 1) {
            if (!TerminalConfig.clientTeleportEnabled) {
                return I18n.format("gui.modular_machinery_terminal.teleport_disabled");
            }
            String stage = TerminalConfig.clientTeleportRequiredGameStage;
            if (stage != null && !stage.isEmpty() && !GameStagesCompat.hasStage(mc.player, stage)) {
                return I18n.format("gui.modular_machinery_terminal.teleport_stage_locked", stage);
            }
        }
        return I18n.format(actionTooltipKey(index));
    }

    private String actionTooltipKey(int index) {
        if (index == 0) return "gui.modular_machinery_terminal.pin_current";
        if (index == 1) return "gui.modular_machinery_terminal.teleport_front";
        if (index == 2) return "gui.modular_machinery_terminal.action_c";
        if (index == 3) return "gui.modular_machinery_terminal.action_i";
        if (index == 4) return "gui.modular_machinery_terminal.action_o";
        if (index == 5) return "gui.modular_machinery_terminal.action_s";
        if (index == 6) return "gui.modular_machinery_terminal.action_u";
        if (index == 7) return "gui.modular_machinery_terminal.action_m";
        return "";
    }

    private ComponentGuiGroup componentGroup(int index) {
        if (index == 2) return ComponentGuiGroup.CONTROLLER;
        if (index == 4) return ComponentGuiGroup.OUTPUT;
        if (index == 5) return ComponentGuiGroup.PATTERN;
        if (index == 6) return ComponentGuiGroup.UPGRADE;
        if (index == 7) return ComponentGuiGroup.SMART_INTERFACE;
        return ComponentGuiGroup.INPUT;
    }

    private boolean canToggleTeamControllers() {
        return TerminalConfig.clientTeamAccessEnabled && FTBUtilitiesCompat.isLoaded();
    }

    private boolean canTeleport() {
        if (!TerminalConfig.clientTeleportEnabled) {
            return false;
        }
        String stage = TerminalConfig.clientTeleportRequiredGameStage;
        return stage == null || stage.isEmpty() || GameStagesCompat.hasStage(mc.player, stage);
    }

    private String teamControllersTooltip() {
        if (!TerminalConfig.clientTeamAccessEnabled) {
            return I18n.format("gui.modular_machinery_terminal.team_controllers_disabled");
        }
        if (!FTBUtilitiesCompat.isLoaded()) {
            return I18n.format("gui.modular_machinery_terminal.show_team_controllers")
                    + "\n"
                    + I18n.format("gui.modular_machinery_terminal.ftbu_required");
        }
        return I18n.format("gui.modular_machinery_terminal.show_team_controllers")
                + "\n"
                + I18n.format("gui.modular_machinery_terminal.current_state",
                I18n.format(showTeamControllers ? "gui.modular_machinery_terminal.state_on" : "gui.modular_machinery_terminal.state_off"));
    }

    private void drawMachineList(int left, int top, int mouseX, int mouseY) {
        List<MachineInfo> visible = visibleMachines();
        int maxScroll = Math.max(0, visible.size() - visibleRowCount());
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        int listX = left + 10;
        int listY = top + CONTENT_TOP + 6;
        int rows = Math.min(visibleRowCount(), visible.size() - scroll);
        for (int i = 0; i < rows; i++) {
            MachineInfo machine = visible.get(scroll + i);
            int y = listY + i * ROW_HEIGHT;
            boolean isSelected = selected != null && selected.equals(machine.key);
            boolean isPinned = pinnedMachines.contains(machine.key);
            int bg = isSelected ? 0xFF314B5A : (isPinned ? 0xFF3D3A28 : 0xFF22272D);
            drawRect(listX, y, listX + LIST_ROW_WIDTH, y + ROW_HEIGHT - 2, bg);
            drawStatusLamp(listX + 4, y + 4, machine);
            drawItem(machine.controllerIcon, listX + 12, y + 3, !machine.loaded);
            int textColor = machine.loaded ? 0xFFE2E7EA : 0xFF888888;
            fontRenderer.drawString(trim(machine.name, 82), listX + 34, y + 1, textColor);
            BlockPos pos = machine.key.pos;
            String coord = "Dim" + machine.key.dimension + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            fontRenderer.drawString(trim(coord, 82), listX + 34, y + 11, machine.loaded ? 0xFF9EA8B2 : 0xFF777777);
        }
        drawScrollbar(left + LIST_WIDTH + 8, listY, 3, visibleRowCount() * ROW_HEIGHT - 2, visible.size(), visibleRowCount(), scroll);
    }

    private void drawDetails(int left, int top, int mouseX, int mouseY) {
        MachineInfo machine = selectedMachine();
        int x = left + LIST_WIDTH + 20;
        int y = top + 55;
        if (machine == null) {
            fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.no_machine"), x, y, 0xFF9EA8B2);
            return;
        }

        drawItem(machine.controllerIcon, x + 3, y + 4, !machine.loaded);
        fontRenderer.drawString(trim(machine.name, 118), x + 24, y + 2, machine.loaded ? 0xFFE2E7EA : 0xFF888888);
        BlockPos pos = machine.key.pos;
        fontRenderer.drawString("Dim" + machine.key.dimension + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), x + 24, y + 13, 0xFF9EA8B2);
        y += 26;

        if (!machine.loaded) {
            fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.machine_unloaded"), x, y, 0xFFB0B0B0);
            return;
        }

        fontRenderer.drawString(stateText(machine), x, y, stateColor(machine));
        y += 11;
        fontRenderer.drawString(trim(I18n.format("gui.modular_machinery_terminal.reason") + ": " + localizeStatus(machine.status), DETAIL_WIDTH), x, y, 0xFFBFC7CF);
        y += 11;
        String threads = I18n.format("gui.modular_machinery_terminal.threads") + ": " + machine.activeThreads + "/" + machine.maxThreads;
        String parallelism = I18n.format("gui.modular_machinery_terminal.parallelism") + ": " + machine.parallelism + "/" + machine.maxParallelism;
        fontRenderer.drawString(trim(threads, 55), x, y, 0xFFD8DEE5);
        fontRenderer.drawString(trim(parallelism, DETAIL_WIDTH - 57), x + 57, y, 0xFFD8DEE5);
        y += 11;
        fontRenderer.drawString(trim(I18n.format("gui.modular_machinery_terminal.energy") + ": " + formatEnergy(machine.energyPerTick), DETAIL_WIDTH), x, y, 0xFFD8DEE5);
        y += 13;

        drawThreads(x, y + 1, machine, mouseX, mouseY);
    }

    private void drawThreads(int x, int y, MachineInfo machine, int mouseX, int mouseY) {
        if (machine.threads.isEmpty()) {
            fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.thread_outputs") + ": 0", x, y, 0xFF888888);
            return;
        }

        String tooltip = null;
        int rows = Math.min(machine.threads.size(), visibleThreadRows(y));
        int maxScroll = Math.max(0, machine.threads.size() - rows);
        if (threadScroll > maxScroll) {
            threadScroll = maxScroll;
        }
        for (int i = 0; i < rows; i++) {
            ThreadInfo thread = machine.threads.get(threadScroll + i);
            int rowY = y + i * ROW_HEIGHT;
            drawRect(x, rowY, x + DETAIL_WIDTH - 2, rowY + ROW_HEIGHT - 2, 0xFF22272D);
            drawThreadStatusLamp(x + 4, rowY + 4, thread);
            drawOutputIcon(thread.output, x + 12, rowY + 3);
            fontRenderer.drawString(trim(thread.name, 58), x + 34, rowY + 1, thread.working ? 0xFFE2E7EA : 0xFFBFC7CF);
            fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.parallelism") + ": " + thread.parallelism, x + 34, rowY + 11, 0xFF9EA8B2);
            String progress = thread.progress + "%";
            fontRenderer.drawString(progress, x + DETAIL_WIDTH - fontRenderer.getStringWidth(progress) - 6, rowY + 7, thread.working ? 0xFF5FE69A : 0xFF9EA8B2);
            boolean overIcon = inside(mouseX, mouseY, x + 12, rowY + 3, 16, 16);
            if (overIcon) {
                String name = thread.output == null ? "" : thread.output.displayName();
                if (name == null || name.isEmpty()) {
                    tooltip = I18n.format("gui.modular_machinery_terminal.no_output");
                } else {
                    tooltip = name;
                }
            } else if (inside(mouseX, mouseY, x, rowY, DETAIL_WIDTH - 2, ROW_HEIGHT - 2) && !thread.status.isEmpty()) {
                tooltip = localizeStatus(thread.status);
            }
        }
        drawScrollbar(x + DETAIL_WIDTH, y, 3, rows * ROW_HEIGHT - 2, machine.threads.size(), rows, threadScroll);
        if (tooltip != null) {
            currentTooltip = tooltip;
        }
    }

    private int visibleThreadRows(int y) {
        return Math.max(0, (guiTop() + GUI_HEIGHT - 8 - y) / ROW_HEIGHT);
    }

    private void drawScrollbar(int x, int y, int width, int height, int total, int visible, int offset) {
        if (total <= visible || visible <= 0 || height <= 0) {
            drawRect(x, y, x + width, y + height, 0xFF20262B);
            return;
        }
        drawRect(x, y, x + width, y + height, 0xFF20262B);
        int thumbHeight = Math.max(10, height * visible / total);
        int thumbTravel = height - thumbHeight;
        int maxOffset = total - visible;
        int thumbY = y + (maxOffset <= 0 ? 0 : thumbTravel * offset / maxOffset);
        drawRect(x, thumbY, x + width, thumbY + thumbHeight, 0xFF6F7982);
    }

    private void drawStatusLamp(int x, int y, MachineInfo machine) {
        int color = 0xFF777777;
        if (machine.loaded && machine.running) {
            color = 0xFF43E06D;
        } else if (machine.loaded && machine.formed) {
            color = 0xFFE2C84A;
        } else if (machine.loaded) {
            color = 0xFFE05C43;
        }
        drawRect(x, y, x + 2, y + 14, color);
        drawRect(x + 2, y, x + 3, y + 14, 0x66000000);
    }

    private void drawThreadStatusLamp(int x, int y, ThreadInfo thread) {
        int color = thread.working ? 0xFF43E06D : 0xFFE2C84A;
        drawRect(x, y, x + 2, y + 14, color);
        drawRect(x + 2, y, x + 3, y + 14, 0x66000000);
    }

    private void drawItem(ItemStack stack, int x, int y, boolean grey) {
        if (stack == null || stack.isEmpty()) {
            drawRect(x, y, x + 16, y + 16, grey ? 0xFF555555 : 0xFF39424B);
            return;
        }
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.color(grey ? 0.45F : 1F, grey ? 0.45F : 1F, grey ? 0.45F : 1F, 1F);
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        GlStateManager.color(1F, 1F, 1F, 1F);
        RenderHelper.disableStandardItemLighting();
    }

    private void drawOutputIcon(OutputInfo output, int x, int y) {
        if (output == null || output.type == OutputInfo.Type.NONE) {
            drawRect(x, y, x + 16, y + 16, 0xFF39424B);
            return;
        }
        if (output.type == OutputInfo.Type.ITEM) {
            drawItem(output.item, x, y, false);
            return;
        }
        if (output.type == OutputInfo.Type.FLUID && output.fluid != null) {
            drawFluid(output.fluid, x, y);
            return;
        }
        if (output.type == OutputInfo.Type.GAS) {
            drawRect(x, y, x + 16, y + 16, 0xFF7CC7D9);
            return;
        }
        drawRect(x, y, x + 16, y + 16, 0xFF39424B);
    }

    private void drawFluid(FluidStack fluid, int x, int y) {
        if (fluid.getFluid() == null || fluid.getFluid().getStill(fluid) == null) {
            drawRect(x, y, x + 16, y + 16, 0xFF4080C0);
            return;
        }
        ResourceLocation still = fluid.getFluid().getStill(fluid);
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(still.toString());
        mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawTexturedModalRect(x, y, sprite, 16, 16);
    }

    private String stateText(MachineInfo machine) {
        String formed = I18n.format("gui.modular_machinery_terminal.formed") + ": " + (machine.formed ? "Y" : "N");
        String running = I18n.format("gui.modular_machinery_terminal.running") + ": " + (machine.running ? "Y" : "N");
        return formed + "  " + running;
    }

    private int stateColor(MachineInfo machine) {
        if (machine.running) {
            return 0xFF5FE69A;
        }
        if (machine.formed) {
            return 0xFFE2C84A;
        }
        return 0xFFE05C43;
    }

    private List<MachineInfo> visibleMachines() {
        String query = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        List<MachineInfo> machines = new ArrayList<>();
        for (MachineInfo machine : ClientTerminalData.getMachines()) {
            if (matchesSearch(machine, query)) {
                machines.add(machine);
            }
        }
        Comparator<MachineInfo> comparator = sortMode.comparator();
        if (descending) {
            comparator = comparator.reversed();
        }
        machines.sort(Comparator
                .comparing((MachineInfo machine) -> !pinnedMachines.contains(machine.key))
                .thenComparing(comparator)
                .thenComparing(machine -> machine.name));
        return machines;
    }

    private boolean matchesSearch(MachineInfo machine, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String name = machine.name.toLowerCase(Locale.ROOT);
        for (String part : query.split("\\s+")) {
            if (!name.contains(part) && !jecContains(name, part)) {
                return false;
            }
        }
        return true;
    }

    private static Method findJecContains() {
        try {
            return Class.forName("me.towdium.jecharacters.util.Match")
                    .getMethod("contains", String.class, CharSequence.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean jecContains(String text, String query) {
        if (JEC_CONTAINS == null) {
            return false;
        }
        try {
            return (Boolean) JEC_CONTAINS.invoke(null, text, query);
        } catch (Exception ignored) {
            return false;
        }
    }

    private MachineInfo selectedMachine() {
        if (selected == null) {
            List<MachineInfo> machines = visibleMachines();
            if (!machines.isEmpty()) {
                selected = machines.get(0).key;
            }
        }
        return selected == null ? null : ClientTerminalData.getMachine(selected);
    }

    private List<MachineKey> dynamicKeys() {
        List<MachineKey> keys = new ArrayList<>();
        MachineInfo selectedMachine = selectedMachine();
        if (selectedMachine != null) {
            keys.add(selectedMachine.key);
        }
        List<MachineInfo> visible = visibleMachines();
        int rows = Math.min(visibleRowCount(), visible.size() - scroll);
        for (int i = 0; i < rows; i++) {
            MachineKey key = visible.get(scroll + i).key;
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String localizeStatus(String status) {
        if (status == null || status.isEmpty()) {
            return I18n.format("gui.modular_machinery_terminal.idle");
        }
        if (I18n.hasKey(status)) {
            return I18n.format(status);
        }
        return status;
    }

    private String trim(String value, int width) {
        if (value == null) {
            return "";
        }
        return fontRenderer.trimStringToWidth(value, width);
    }

    private String formatEnergy(long energyPerTick) {
        if (energyPerTick > 0) {
            return "+" + energyPerTick + " RF/t";
        }
        if (energyPerTick < 0) {
            return energyPerTick + " RF/t";
        }
        return "0 RF/t";
    }

    private String sortLabel() {
        return I18n.format("gui.modular_machinery_terminal.sort") + ":" + I18n.format(sortMode.key);
    }

    private String directionLabel() {
        return I18n.format(descending ? "gui.modular_machinery_terminal.desc" : "gui.modular_machinery_terminal.asc");
    }

    private void togglePinned(MachineKey key) {
        if (pinnedMachines.contains(key)) {
            pinnedMachines.remove(key);
        } else {
            pinnedMachines.add(key);
        }
        savePinnedMachines();
    }

    private void loadPinnedMachines() {
        pinnedMachines.clear();
        File file = pinsFile();
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                MachineKey key = parseKey(line.trim());
                if (key != null) {
                    pinnedMachines.add(key);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void savePinnedMachines() {
        File file = pinsFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (MachineKey key : pinnedMachines) {
                BlockPos pos = key.pos;
                writer.write(key.dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private MachineKey parseKey(String text) {
        String[] parts = text.split(":");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new MachineKey(Integer.parseInt(parts[0]), new BlockPos(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private File pinsFile() {
        return new File(Loader.instance().getConfigDir(), "modularmachinery_terminal_pins.txt");
    }

    private void loadSettings() {
        File file = settingsFile();
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 2) {
                    sortMode = SortMode.valueOf(parts[0]);
                    descending = Boolean.parseBoolean(parts[1]);
                    if (parts.length >= 3) {
                        showTeamControllers = Boolean.parseBoolean(parts[2]);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveSettings() {
        File file = settingsFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sortMode.name() + "," + descending + "," + showTeamControllers);
            writer.newLine();
        } catch (IOException ignored) {
        }
    }

    private File settingsFile() {
        return new File(Loader.instance().getConfigDir(), "modularmachinery_terminal_settings.txt");
    }

    private int guiLeft() {
        return (width - GUI_LAYOUT_WIDTH) / 2;
    }

    private int guiTop() {
        return (height - GUI_HEIGHT) / 2;
    }

    private boolean overListPanel() {
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int left = guiLeft();
        int top = guiTop();
        return inside(mouseX, mouseY, left + 6, top + CONTENT_TOP, LIST_WIDTH + 6, GUI_HEIGHT - CONTENT_TOP - 8);
    }

    private boolean overDetailPanel() {
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int left = guiLeft();
        int top = guiTop();
        return inside(mouseX, mouseY, left + LIST_WIDTH + 16, top + CONTENT_TOP, GUI_LAYOUT_WIDTH - LIST_WIDTH - 24, GUI_HEIGHT - CONTENT_TOP - 8);
    }

    private int threadStartY() {
        return guiTop() + CONTENT_TOP + 78;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private int visibleRowCount() {
        return 6;
    }

    private int sortX(int left) {
        return left + 88;
    }

    private int directionX(int left) {
        return left + 155;
    }

    private int teamControllersX(int left) {
        return left + 188;
    }

    private int legendX(int left) {
        return left + GUI_WIDTH - 40;
    }

    private int actionButtonX(int left) {
        return left + GUI_LAYOUT_WIDTH - 6;
    }

    private int actionButtonY(int top, int index) {
        return top + CONTENT_TOP + index * (actionButtonSize() + 4);
    }

    private int actionButtonSize() {
        return 13;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private enum SortMode {
        NAME("gui.modular_machinery_terminal.sort.name", Comparator.comparing(machine -> machine.name.toLowerCase(Locale.ROOT))),
        LOADED("gui.modular_machinery_terminal.sort.loaded", Comparator.comparing(machine -> machine.loaded)),
        FORMED("gui.modular_machinery_terminal.sort.formed", Comparator.comparing(machine -> machine.formed)),
        RUNNING("gui.modular_machinery_terminal.sort.running", Comparator.comparing(machine -> machine.running));

        private final String key;
        private final Comparator<MachineInfo> comparator;

        SortMode(String key, Comparator<MachineInfo> comparator) {
            this.key = key;
            this.comparator = comparator;
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        private Comparator<MachineInfo> comparator() {
            return comparator;
        }
    }
}
