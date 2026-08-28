package com.khiar.modweb;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BrowserScreen extends Screen {
    private final ModWebConfig config;
    private MCEFBrowser browser;
    private TextFieldWidget address;
    private boolean maximized;
    private boolean dragging;
    private int dragX, dragY;
    private int oldX, oldY, oldW, oldH;

    private static final int BAR = 58;
    private static final int RADIUS = 8;

    public BrowserScreen(Text title, ModWebConfig config) {
        super(title);
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        int w = Math.min(config.width, this.width - 20);
        int h = Math.min(config.height, this.height - 20);
        if (config.x + w > this.width) config.x = Math.max(0, this.width - w);
        if (config.y + h > this.height) config.y = Math.max(0, this.height - h);

        if (browser == null) {
            browser = MCEF.createBrowser("https://www.google.com", false);
            browser.setFocus(true);
        }
        resizeBrowser();

        address = new TextFieldWidget(textRenderer, config.x + 105, config.y + 10, Math.max(180, w - 190), 30,
                Text.literal("Telusuri Google atau ketik URL"));
        address.setMaxLength(2048);
        address.setPlaceholder(Text.literal("Telusuri Google atau ketik URL"));
        address.setChangedListener(value -> { });
        addDrawableChild(address);
    }

    private int browserX() { return config.x; }
    private int browserY() { return config.y + BAR; }
    private int browserW() { return Math.max(100, config.width); }
    private int browserH() { return Math.max(100, config.height - BAR); }

    private void resizeBrowser() {
        if (browser != null) {
            double scale = client.getWindow().getScaleFactor();
            browser.resize((int) (browserW() * scale), (int) (browserH() * scale));
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        init(client, width, height);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Keep the world visible and draw a simple crosshair behind the browser.
        drawCrosshair(ctx);

        int x = config.x, y = config.y, w = config.width, h = config.height;
        ctx.fill(x, y, x + w, y + BAR, 0xFF202124);
        ctx.fill(x, y + BAR, x + w, y + h, 0xFF101114);

        ctx.drawText(textRenderer, "◉  Google Chrome", x + 12, y + 21, 0xFFE8EAED, false);
        ctx.drawText(textRenderer, "—", x + w - 72, y + 20, 0xFFBDC1C6, false);
        ctx.drawText(textRenderer, maximized ? "❐" : "□", x + w - 48, y + 20, 0xFFBDC1C6, false);
        ctx.drawText(textRenderer, "×", x + w - 25, y + 20, 0xFFE8EAED, false);

        ctx.fill(x + 100, y + 9, x + w - 82, y + 41, 0xFF303134);
        ctx.drawText(textRenderer, "🎤   ◉", x + w - 72, y + 20, 0xFFBDC1C6, false);

        drawBrowserTexture(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBrowserTexture(DrawContext ctx) {
        if (browser == null) return;
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(browserX(), browserY() + browserH(), 0).texture(0, 1).color(255,255,255,255).next();
        b.vertex(browserX() + browserW(), browserY() + browserH(), 0).texture(1, 1).color(255,255,255,255).next();
        b.vertex(browserX() + browserW(), browserY(), 0).texture(1, 0).color(255,255,255,255).next();
        b.vertex(browserX(), browserY(), 0).texture(0, 0).color(255,255,255,255).next();
        tess.draw();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }

    private void drawCrosshair(DrawContext ctx) {
        int cx = width / 2, cy = height / 2;
        ctx.fill(cx - 5, cy, cx + 6, cy + 1, 0xFFFFFFFF);
        ctx.fill(cx, cy - 5, cx + 1, cy + 6, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = config.x, y = config.y, w = config.width;
        if (mouseY >= y && mouseY < y + BAR) {
            if (mouseX >= x + w - 32) { close(); return true; }
            if (mouseX >= x + w - 58) { toggleMaximize(); return true; }
            if (mouseX >= x + w - 85) { config.height = 0; return true; }
            if (mouseX < x + 100) { dragging = true; dragX = (int) mouseX - x; dragY = (int) mouseY - y; return true; }
        }
        if (mouseY >= browserY() && mouseX >= browserX() && mouseX <= browserX()+browserW()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMousePress((int)((mouseX-browserX())*scale), (int)((mouseY-browserY())*scale), button);
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
            browser.sendMouseRelease((int)((mouseX-browserX())*scale), (int)((mouseY-browserY())*scale), button);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging) {
            config.x = Math.max(0, Math.min(width-config.width, (int)mouseX-dragX));
            config.y = Math.max(0, Math.min(height-config.height, (int)mouseY-dragY));
            if (address != null) { address.setX(config.x + 105); address.setY(config.y + 10); }
        }
        if (browser != null && mouseY >= browserY()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMouseMove((int)((mouseX-browserX())*scale), (int)((mouseY-browserY())*scale));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (browser != null && mouseY >= browserY()) {
            double scale = client.getWindow().getScaleFactor();
            browser.sendMouseWheel((int)((mouseX-browserX())*scale), (int)((mouseY-browserY())*scale), delta, 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER && address != null && address.isFocused()) {
            navigate(address.getText());
            return true;
        }
        if (browser != null) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
            browser.setFocus(true);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (browser != null) browser.sendKeyRelease(keyCode, scanCode, modifiers);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (address != null && address.isFocused()) return super.charTyped(codePoint, modifiers);
        if (browser != null) browser.sendKeyTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    private void navigate(String text) {
        if (text == null || text.isBlank()) return;
        String url = text.trim();
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            if (url.contains(" ")) url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            else url = "https://" + url;
        }
        browser.loadURL(url);
        address.setText(url);
        address.setSelectionStart(0);
        address.setSelectionEnd(url.length());
    }

    private void toggleMaximize() {
        if (!maximized) {
            oldX=config.x; oldY=config.y; oldW=config.width; oldH=config.height;
            config.x=0; config.y=0; config.width=width; config.height=height;
            maximized=true;
        } else {
            config.x=oldX; config.y=oldY; config.width=oldW; config.height=oldH;
            maximized=false;
        }
        if (address != null) { address.setX(config.x+105); address.setY(config.y+10); address.setWidth(Math.max(180, config.width-190)); }
        resizeBrowser();
    }

    @Override
    public void close() {
        if (browser != null) { browser.close(); browser = null; }
        config.save();
        super.close();
    }
}
