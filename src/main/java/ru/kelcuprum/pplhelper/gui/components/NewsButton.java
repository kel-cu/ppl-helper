package ru.kelcuprum.pplhelper.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ru.kelcuprum.alinlib.AlinLib;
import ru.kelcuprum.alinlib.gui.GuiUtils;
import ru.kelcuprum.alinlib.gui.components.builder.button.ButtonBuilder;
import ru.kelcuprum.alinlib.gui.components.buttons.Button;
import ru.kelcuprum.pplhelper.api.components.News;
import ru.kelcuprum.pplhelper.gui.TextureHelper;
import ru.kelcuprum.pplhelper.gui.screens.project.NewsScreen;

public class NewsButton extends Button {
    protected News news;

    public NewsButton(int x, int y, int width, News news, Screen screen) {
        super(new ButtonBuilder().setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new NewsScreen(screen, news))).setTitle(Component.empty()).setStyle(GuiUtils.getSelected()).setSize(width, 40).setPosition(x, y));
        this.news = news;
        setMessage(Component.literal(news.title));
    }

    @Override
    public void renderText(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (getY() < guiGraphics.guiHeight() && !(getY() <= -getHeight())) {
            int x = 5;
            if(news.icon != null && !news.icon.isEmpty()) {
                ResourceLocation icon = TextureHelper.getTexture(news.icon, String.format("news_%s", news.id));
                guiGraphics.blit(RenderType::guiTextured, icon, getX() + 2, getY() + 2, 0.0F, 0.0F, 36, 36, 36, 36);
                x+=40;
            }
            renderString(guiGraphics, news.title, getX() + x, getY() + 8);
            renderString(guiGraphics, news.description, getX() + x, getY() + height - 8 - AlinLib.MINECRAFT.font.lineHeight);
        }
    }

    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, Component message, int y) {
        int k = this.getX() + (news.icon != null && !news.icon.isEmpty() ? 45 : 5);
        int l = this.getX() + this.getWidth() - 5;
        renderScrollingString(guiGraphics, font, message, k, y, l, y + font.lineHeight, -1);
    }

    protected void renderString(GuiGraphics guiGraphics, String text, int x, int y) {
        if (getWidth() - (news.icon != null && !news.icon.isEmpty() ? 50 : 10) < AlinLib.MINECRAFT.font.width(text))
            renderScrollingString(guiGraphics, AlinLib.MINECRAFT.font, Component.literal(text), y - 1);
        else guiGraphics.drawString(AlinLib.MINECRAFT.font, text, x, y, -1);
    }
}
