package com.tgskiv.skydiving.menu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class VarioConfigHandledScreen extends HandledScreen<VarioScreenHandler> {
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT_ITEM = 166;
    private static final int BG_HEIGHT_HELMET = 236;

    private final boolean helmetMode;

    public VarioConfigHandledScreen(VarioScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.helmetMode = Text.translatable("screen.paraglidingsimulator.vario_helmet").getString().equals(title.getString());
        this.backgroundWidth = BG_WIDTH;
        this.backgroundHeight = helmetMode ? BG_HEIGHT_HELMET : BG_HEIGHT_ITEM;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.x;
        int y = this.y;

        if (helmetMode) {
            addHelmetControls(x, y);
        } else {
            addItemControls(x, y);
        }
    }

    private void addItemControls(int x, int y) {
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.varioDescentThreshold = clamp(SkydivingClientConfig.varioDescentThreshold - 0.1, -10.0, 0.0);
        }).dimensions(x + 120, y + 32, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.varioDescentThreshold = clamp(SkydivingClientConfig.varioDescentThreshold + 0.1, -10.0, 0.0);
        }).dimensions(x + 144, y + 32, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.varioStrongDescentThreshold = clamp(SkydivingClientConfig.varioStrongDescentThreshold - 0.5, -20.0, 0.0);
        }).dimensions(x + 120, y + 56, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.varioStrongDescentThreshold = clamp(SkydivingClientConfig.varioStrongDescentThreshold + 0.5, -20.0, 0.0);
        }).dimensions(x + 144, y + 56, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.varioVolume = (float)clamp(SkydivingClientConfig.varioVolume - 0.05, 0.0, 1.0);
        }).dimensions(x + 120, y + 80, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.varioVolume = (float)clamp(SkydivingClientConfig.varioVolume + 0.05, 0.0, 1.0);
        }).dimensions(x + 144, y + 80, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.paraglidingsimulator.done"), button -> close())
                .dimensions(x + 48, y + 124, 80, 20).build());
    }

    private void addHelmetControls(int x, int y) {
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.helmetDescentThreshold = clamp(SkydivingClientConfig.helmetDescentThreshold - 0.1, -10.0, 0.0);
        }).dimensions(x + 120, y + 24, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.helmetDescentThreshold = clamp(SkydivingClientConfig.helmetDescentThreshold + 0.1, -10.0, 0.0);
        }).dimensions(x + 144, y + 24, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.helmetStrongDescentThreshold = clamp(SkydivingClientConfig.helmetStrongDescentThreshold - 0.5, -20.0, 0.0);
        }).dimensions(x + 120, y + 46, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.helmetStrongDescentThreshold = clamp(SkydivingClientConfig.helmetStrongDescentThreshold + 0.5, -20.0, 0.0);
        }).dimensions(x + 144, y + 46, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.helmetVolume = (float)clamp(SkydivingClientConfig.helmetVolume - 0.05, 0.0, 1.0);
        }).dimensions(x + 120, y + 68, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.helmetVolume = (float)clamp(SkydivingClientConfig.helmetVolume + 0.05, 0.0, 1.0);
        }).dimensions(x + 144, y + 68, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            SkydivingClientConfig.hudScale = (float)clamp(SkydivingClientConfig.hudScale - 0.25, 0.5, 1.5);
        }).dimensions(x + 120, y + 90, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            SkydivingClientConfig.hudScale = (float)clamp(SkydivingClientConfig.hudScale + 0.25, 0.5, 1.5);
        }).dimensions(x + 144, y + 90, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.wind", SkydivingClientConfig.hudShowWind), button -> {
            SkydivingClientConfig.hudShowWind = !SkydivingClientConfig.hudShowWind;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.wind", SkydivingClientConfig.hudShowWind));
        }).dimensions(x + 10, y + 120, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.altitude", SkydivingClientConfig.hudShowAltitude), button -> {
            SkydivingClientConfig.hudShowAltitude = !SkydivingClientConfig.hudShowAltitude;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.altitude", SkydivingClientConfig.hudShowAltitude));
        }).dimensions(x + 92, y + 120, 74, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.speed", SkydivingClientConfig.hudShowSpeed), button -> {
            SkydivingClientConfig.hudShowSpeed = !SkydivingClientConfig.hudShowSpeed;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.speed", SkydivingClientConfig.hudShowSpeed));
        }).dimensions(x + 10, y + 142, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.vario", SkydivingClientConfig.hudShowVario), button -> {
            SkydivingClientConfig.hudShowVario = !SkydivingClientConfig.hudShowVario;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.vario", SkydivingClientConfig.hudShowVario));
        }).dimensions(x + 92, y + 142, 74, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.heading", SkydivingClientConfig.hudShowHeading), button -> {
            SkydivingClientConfig.hudShowHeading = !SkydivingClientConfig.hudShowHeading;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.heading", SkydivingClientConfig.hudShowHeading));
        }).dimensions(x + 10, y + 164, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(toggleLabel("gui.paraglidingsimulator.hud.time", SkydivingClientConfig.hudShowThermal), button -> {
            SkydivingClientConfig.hudShowThermal = !SkydivingClientConfig.hudShowThermal;
            button.setMessage(toggleLabel("gui.paraglidingsimulator.hud.time", SkydivingClientConfig.hudShowThermal));
        }).dimensions(x + 92, y + 164, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.paraglidingsimulator.mod_settings"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!isClothConfigAvailable()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("message.paraglidingsimulator.config_requires_cloth"), true);
                }
                return;
            }
            client.setScreen(ParaglidingSimulatorConfigScreen.create(this));
        }).dimensions(x + 10, y + 210, 74, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.paraglidingsimulator.done"), button -> close())
                .dimensions(x + 92, y + 210, 74, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        renderInGameBackground(context);
        drawPanel(context, x, y, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(textRenderer, title, backgroundWidth / 2, 6, 0xFFFFFF);
        drawLabels(context, 0, 0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLabels(DrawContext context, int baseX, int baseY) {
        String descentLabel = Text.translatable("gui.paraglidingsimulator.label.descent_alarm").getString();
        String strongLabel = Text.translatable("gui.paraglidingsimulator.label.strong_descent").getString();
        String volumeLabel = Text.translatable("gui.paraglidingsimulator.label.volume").getString();

        int labelColor = 0xFFFFFF;
        int valueRight = baseX + 112;

        if (helmetMode) {
            context.drawTextWithShadow(textRenderer, descentLabel, baseX + 10, baseY + 30, labelColor);
            context.drawTextWithShadow(textRenderer, strongLabel, baseX + 10, baseY + 52, labelColor);
            context.drawTextWithShadow(textRenderer, volumeLabel, baseX + 10, baseY + 74, labelColor);
            String descentValue = String.format("%.2f", SkydivingClientConfig.helmetDescentThreshold);
            String strongValue = String.format("%.2f", SkydivingClientConfig.helmetStrongDescentThreshold);
            String volumeValue = String.format("%.2f", SkydivingClientConfig.helmetVolume);
            context.drawTextWithShadow(textRenderer, descentValue, valueRight - textRenderer.getWidth(descentValue), baseY + 30, labelColor);
            context.drawTextWithShadow(textRenderer, strongValue, valueRight - textRenderer.getWidth(strongValue), baseY + 52, labelColor);
            context.drawTextWithShadow(textRenderer, volumeValue, valueRight - textRenderer.getWidth(volumeValue), baseY + 74, labelColor);
            context.drawTextWithShadow(textRenderer, Text.translatable("gui.paraglidingsimulator.label.hud"), baseX + 10, baseY + 96, labelColor);
            String hudScaleText = String.format("x%.2f", SkydivingClientConfig.hudScale);
            context.drawTextWithShadow(textRenderer, hudScaleText, valueRight - textRenderer.getWidth(hudScaleText), baseY + 96, labelColor);
        } else {
            context.drawTextWithShadow(textRenderer, descentLabel, baseX + 10, baseY + 38, labelColor);
            context.drawTextWithShadow(textRenderer, strongLabel, baseX + 10, baseY + 62, labelColor);
            context.drawTextWithShadow(textRenderer, volumeLabel, baseX + 10, baseY + 86, labelColor);

            String descentValue = String.format("%.2f", SkydivingClientConfig.varioDescentThreshold);
            String strongValue = String.format("%.2f", SkydivingClientConfig.varioStrongDescentThreshold);
            String volumeValue = String.format("%.2f", SkydivingClientConfig.varioVolume);
            context.drawTextWithShadow(textRenderer, descentValue, valueRight - textRenderer.getWidth(descentValue), baseY + 38, labelColor);
            context.drawTextWithShadow(textRenderer, strongValue, valueRight - textRenderer.getWidth(strongValue), baseY + 62, labelColor);
            context.drawTextWithShadow(textRenderer, volumeValue, valueRight - textRenderer.getWidth(volumeValue), baseY + 86, labelColor);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Text toggleLabel(String labelKey, boolean enabled) {
        return Text.translatable("gui.paraglidingsimulator.toggle",
                Text.translatable(labelKey),
                Text.translatable(enabled ? "gui.paraglidingsimulator.on" : "gui.paraglidingsimulator.off"));
    }

    private static boolean isClothConfigAvailable() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("cloth-config") || loader.isModLoaded("cloth-config2");
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        int bg = 0xFFB6B6B6;
        int borderDark = 0xFF404040;
        int borderLight = 0xFFFFFFFF;
        context.fill(x, y, x + width, y + height, bg);
        context.fill(x, y, x + width, y + 1, borderLight);
        context.fill(x, y, x + 1, y + height, borderLight);
        context.fill(x + width - 1, y, x + width, y + height, borderDark);
        context.fill(x, y + height - 1, x + width, y + height, borderDark);
    }
}
