package com.khiar.modweb;

import net.ccbluex.liquidbounce.mcef.MCEF;
import net.ccbluex.liquidbounce.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class BrowserScreen extends Screen {
    private final ModWebConfig config;
    private MCEFBrowser browser;
    private TextFieldWidget address;
    private boolean maximized;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private int oldX, oldY, oldW, oldH;

    private static final int TITLE_BAR = 58;

    public BrowserScreen(Text title, ModWebConfig config) {
        super(title);
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        clampWindow();

        if (browser == null) {
            if (!MCEF.INSTANCE.isInitialized()) {
                close();
                return;
            }
            browser = MCEF.INSTANCE.createBrowser("https://www.google.com", false, 60);
            browser.setFocus(true);
        }

        resizeBrowser();

        int addressWidth = Math.max(180, config.width - 190);
        address = new TextFieldWidget(textRenderer, config.x + 105, config.y + 10,
                addressWidth, 30, Text.literal("Telusuri Google atau ketik URL"));
        address.setMaxLength(2048);
        address.setPlaceholder(Text.literal("Telusuri Google atau ketik URL"));
        addDrawableChild(address);
    }

    private void clampWindow() {
        config.width = Math.max(300, Math.min(config.width, width));
        config.height = Math.max(220, Math.min(config.height, height));
        config.x = Math.max(0, Math.min(config.x, Math.max(0, width - config.width)));
        config.y = Math.max(0, Math.min(config.y, Math.max(0, height - config.height)));
    }

    private int browserX() { return config.x; }
    private int browserY() { return config.y + TITLE_BAR; }
    private int browserW() { return Math.max(100, config.width); }
    private int browserH() { return Math.max(100, config.height - TITLE_BAR); }

    private void resizeBrowser() {
        if (browser != null) {
            double scale = client.getWindow().getScaleFactor();
            browser.resize((int) (browserW() * scale), (int) (browserH() * scale));
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        clampWindow();
        if (address != null) {
            address.setX(config.x + 105);
            address.setY(config.y + 10);
            address.setWidth(Math.max(180, config.width - 190));
        }
        resizeBrowser();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = config.x;
        int y = config.y;
        int w = config.width;
        int h = config.height;

        // Minecraft remains visible behind the browser.
        drawCrosshair(ctx);

        ctx.fill(x, y, x + w, y + TITLE_BAR, 0xFF202124);
        ctx.fill(x, y + TITLE_BAR, x + w, y + h, 0xFF101114);

        ctx.drawText(textRenderer, "◉  Google Chrome", x + 12, y + 21, 0xFFE8EAED, false);
        ctx.drawText(textRenderer, "—", x + w - 76, y + 20, 0xFFBDC1C6, false);
        ctx.drawText(textRenderer, maximized ? "❐" : "□", x + w - 51, y + 20, 0xFFBDC1C6, false);
        ctx.drawText(textRenderer, "×", x + w - 26, y + 20, 0xFFE8EAED, false);

        // Chrome-like address bar background. The actual text input is rendered by TextFieldWidget.
        ctx.fill(x + 100, y + 9, x + w - 82, y + 41, 0xFF303134);
        ctx.drawText(textRenderer, "🎤", x + w - 70, y + 20, 0xFFBDC1C6, false);
        ctx.drawText(textRenderer, "◉", x + w - 45, y + 20, 0xFFBDC1C6, false);

        drawBrowserTexture(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBrowserTexture(DrawContext ctx) {
        if (browser == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(browserX(), browserY() + browserH(), 0).texture(0, 1).color(255, 255, 255, 255).next();
        buffer.vertex(browserX() + browserW(), browserY() + browserH(), 0).texture(1, 1).color(255, 255, 255, 255).next();
        buffer.vertex(browserX() + browserW(), browserY(), 0).texture(1, 0).color(255, 255, 255, 255).next();
        buffer.vertex(browserX(), browserY(), 0).texture(0, 0).color(255, 255, 255, 255).next();
        tessellator.draw();

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }

    private void drawCrosshair(DrawContext ctx) {
        int cx = width / 2;
        int cy = height / 2;
        ctx.fill(cx - 5, cy, cx + 6, cy + 1, 0xFFFFFFFF);
        ctx.fill(cx, cy - 5, cx + 1, cy + 6, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = config.x;
        int y = config.y;
        int w = config.width;

        if (mouseY >= y && mouseY < y + TITLE_BAR) {
            if (mouseX >= x + w - 32) {
                close();
                return true;
            }
            if (mouseX >= x + w - 60) {
                toggleMaximize();
                return true;
            }
            if (mouseX >= x + w - 88) {
                // Minimize = temporarily hide the screen; F9 can reopen it.
                close();
                return true;
            }
            if (mouseX < x + 100) {
                dragging = true;
                dragOffsetX = (int) mouseX - x;
                dragOffsetY = (int) mouseY - y;
                return true;
            }
        }

        if (browser != null && mouseY >= browserY() && mouseY <= browserY() + browserH()
                && mouseX >= browserX() && mouseX <= browserX() + browserW()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMousePress((int) ((mouseX - browserX()) * scale),
                    (int) ((mouseY - browserY()) * scale), button);
            browser.setFocus(true);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        if (browser != null) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMouseRelease((int) ((mouseX - browserX()) * scale),
                    (int) ((mouseY - browserY()) * scale), button);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging) {
            config.x = Math.max(0, Math.min(width - config.width, (int) mouseX - dragOffsetX));
            config.y = Math.max(0, Math.min(height - config.height, (int) mouseY - dragOffsetY));
            if (address != null) {
                address.setX(config.x + 105);
                address.setY(config.y + 10);
            }
        }

        if (browser != null && mouseY >= browserY()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMouseMove((int) ((mouseX - browserX()) * scale),
                    (int) ((mouseY - browserY()) * scale));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (browser != null && mouseY >= browserY()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMouseWheel((int) ((mouseX - browserX()) * scale),
                    (int) ((mouseY - browserY()) * scale), verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER && address != null && address.isFocused()) {
            navigate(address.getText());
            return true;
        }

        if (browser != null && (address == null || !address.isFocused())) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
            browser.setFocus(true);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (browser != null && (address == null || !address.isFocused())) {
            browser.sendKeyRelease(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (address != null && address.isFocused()) {
            return super.charTyped(codePoint, modifiers);
        }
        if (browser != null) {
            browser.sendKeyTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void navigate(String text) {
        if (browser == null || text == null || text.isBlank()) return;

        String url = text.trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            if (url.contains(" ")) {
                url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            } else {
                url = "https://" + url;
            }
        }

        browser.loadURL(url);
        address.setText(url);
        address.setSelectionStart(0);
        address.setSelectionEnd(url.length());
    }

    private void toggleMaximize() {
        if (!maximized) {
            oldX = config.x;
            oldY = config.y;
            oldW = config.width;
            oldH = config.height;
            config.x = 0;
            config.y = 0;
            config.width = width;
            config.height = height;
            maximized = true;
        } else {
            config.x = oldX;
            config.y = oldY;
            config.width = oldW;
            config.height = oldH;
            clampWindow();
            maximized = false;
        }

        if (address != null) {
            address.setX(config.x + 105);
            address.setY(config.y + 10);
            address.setWidth(Math.max(180, config.width - 190));
        }
        resizeBrowser();
    }

    @Override
    public void close() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        config.save();
        super.close();
    }
}
