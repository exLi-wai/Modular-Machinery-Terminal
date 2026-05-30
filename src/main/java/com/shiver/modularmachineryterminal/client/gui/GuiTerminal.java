package com.shiver.modularmachineryterminal.client.gui;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.OutputInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import com.shiver.modularmachineryterminal.common.ThreadInfo;
import com.shiver.modularmachineryterminal.network.PacketRequestDynamic;
import com.shiver.modularmachineryterminal.network.PacketRequestFullList;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GuiTerminal extends GuiScreen {

    private static final int GUI_WIDTH = 292;
    private static final int GUI_HEIGHT = 212;
    private static final int LIST_WIDTH = 126;
    private static final int ROW_HEIGHT = 24;
    private static final int CONTENT_TOP = 50;
    private static final int DETAIL_WIDTH = 134;
    private static final int LIST_ROW_WIDTH = LIST_WIDTH - 8;
    private static final Method JEC_CONTAINS = findJecContains();

    private GuiTextField searchField;
    private MachineKey selected;
    private SortMode sortMode = SortMode.NAME;
    private boolean descending;
    private int scroll;
    private int threadScroll;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int left = guiLeft();
        int top = guiTop();
        searchField = new GuiTextField(0, fontRenderer, left + 8, top + 32, 76, 14);
        searchField.setMaxStringLength(40);
        buttonList.clear();
        TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList());
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        if (mc.player.ticksExisted % 20 == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList());
        }
        if (mc.player.ticksExisted % 4 == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketRequestDynamic(dynamicKeys()));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
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
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        int left = guiLeft();
        int top = guiTop();

        if (inside(mouseX, mouseY, sortX(left), top + 32, 64, 14)) {
            sortMode = sortMode.next();
            scroll = 0;
            return;
        }
        if (inside(mouseX, mouseY, directionX(left), top + 32, 30, 14)) {
            descending = !descending;
            scroll = 0;
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
                TerminalNetwork.CHANNEL.sendToServer(new PacketRequestDynamic(dynamicKeys()));
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
        drawRect(left + LIST_WIDTH + 16, top + CONTENT_TOP, left + GUI_WIDTH - 8, top + GUI_HEIGHT - 8, 0xFF14181C);
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
        int x = legendX(left);
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
    }

    private void drawSmallButton(int x, int y, int width, int height, String text, boolean hovered) {
        int border = hovered ? 0xFF8DA5B8 : 0xFF5E6973;
        int fill = hovered ? 0xFF394650 : 0xFF273039;
        drawRect(x, y, x + width, y + height, border);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        fontRenderer.drawString(trim(text, width - 6), x + 3, y + 3, 0xFFD8DEE5);
    }

    private void drawMachineList(int left, int top, int mouseX, int mouseY) {
        List<MachineInfo> visible = visibleMachines();
        int listX = left + 10;
        int listY = top + CONTENT_TOP + 6;
        int rows = Math.min(visibleRowCount(), visible.size() - scroll);
        for (int i = 0; i < rows; i++) {
            MachineInfo machine = visible.get(scroll + i);
            int y = listY + i * ROW_HEIGHT;
            boolean isSelected = selected != null && selected.equals(machine.key);
            int bg = isSelected ? 0xFF314B5A : 0xFF22272D;
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
        fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.threads") + ": " + machine.activeThreads + "/" + machine.maxThreads, x, y, 0xFFD8DEE5);
        y += 11;
        fontRenderer.drawString(I18n.format("gui.modular_machinery_terminal.parallelism") + ": " + machine.parallelism + "/" + machine.maxParallelism, x, y, 0xFFD8DEE5);
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
            drawHoveringText(tooltip, mouseX, mouseY);
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
        machines.sort(comparator.thenComparing(machine -> machine.name));
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

    private String sortLabel() {
        return I18n.format("gui.modular_machinery_terminal.sort") + ":" + I18n.format(sortMode.key);
    }

    private String directionLabel() {
        return I18n.format(descending ? "gui.modular_machinery_terminal.desc" : "gui.modular_machinery_terminal.asc");
    }

    private int guiLeft() {
        return (width - GUI_WIDTH) / 2;
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
        return inside(mouseX, mouseY, left + LIST_WIDTH + 16, top + CONTENT_TOP, GUI_WIDTH - LIST_WIDTH - 24, GUI_HEIGHT - CONTENT_TOP - 8);
    }

    private int threadStartY() {
        return guiTop() + CONTENT_TOP + 68;
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

    private int legendX(int left) {
        return left + GUI_WIDTH - 40;
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
