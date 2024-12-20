package ru.kelcuprum.pplhelper.gui.screens.project;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import ru.kelcuprum.alinlib.AlinLib;
import ru.kelcuprum.alinlib.gui.Colors;
import ru.kelcuprum.alinlib.gui.components.ConfigureScrolWidget;
import ru.kelcuprum.alinlib.gui.components.ImageWidget;
import ru.kelcuprum.alinlib.gui.components.builder.button.ButtonBuilder;
import ru.kelcuprum.alinlib.gui.components.text.MessageBox;
import ru.kelcuprum.alinlib.gui.components.text.TextBox;
import ru.kelcuprum.pplhelper.PepelandHelper;
import ru.kelcuprum.pplhelper.api.components.Project;
import ru.kelcuprum.pplhelper.gui.TextureHelper;
import ru.kelcuprum.pplhelper.gui.components.BannerWidget;
import ru.kelcuprum.pplhelper.gui.components.Blockquote;
import ru.kelcuprum.pplhelper.gui.components.HorizontalRule;
import ru.kelcuprum.pplhelper.gui.components.ScaledTextBox;

import java.util.ArrayList;
import java.util.List;

import static ru.kelcuprum.pplhelper.PepelandHelper.Icons.PACK_INFO;
import static ru.kelcuprum.pplhelper.PepelandHelper.Icons.WEB;

public class ProjectScreen extends Screen {
    public final Project project;
    public final Screen parent;
    public ProjectScreen(Screen screen, Project project) {
        super(Component.translatable("pplhelper.project", project.title));
        this.project = project;
        this.parent = screen;
    }

    public int maxY = 35;

    @Override
    protected void init() {
//        initPanel();
        initContent();
    }

    protected void initPanel() {
        int x = 10;
        int size = 200;
        addRenderableWidget(new TextBox(x, 5, size-25, 20, title, true));
        addRenderableWidget(new ButtonBuilder(Component.literal("x"), (s)->onClose()).setPosition( x+size-15, 5).setWidth(20).build()); //, 20, 20,
        int y = 35;
        int iconSize = project.icon != null && !project.icon.isEmpty() ? 41 : 0;
        if(project.icon != null && !project.icon.isEmpty()) addRenderableWidget(new ImageWidget(x, y, 36, 36, TextureHelper.getTexture(project.icon, String.format("project_%s", project.id)), 36, 36, Component.empty()));
        addRenderableWidget(new TextBox(x + iconSize, y, size - iconSize, 18, Component.literal(project.title), false));
        addRenderableWidget(new TextBox(x + iconSize, y + 18, size - iconSize, 18, Component.literal(project.creators), false));
        y += 41;
        if(project.description != null && !project.description.isEmpty()) {
            String desc = project.description;
            if(desc.length() > 93) desc = desc.substring(0, 92)+"...";
            MessageBox msg = new Blockquote(x, y, size, 20, Component.literal(desc), false);
            addRenderableWidget(msg);
            y += (5 + msg.getHeight());
        }
        MutableComponent coord = Component.empty().append(Component.translatable("pplhelper.project.coordinates", project.world));
        if(project.coordinates$overworld != null && !project.coordinates$overworld.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.overworld")).append(": ").append(project.coordinates$overworld);
        if(project.coordinates$nether != null && !project.coordinates$nether.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.nether")).append(": ").append(project.coordinates$nether);
        if(project.coordinates$end != null && !project.coordinates$end.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.end")).append(": ").append(project.coordinates$end);
        MessageBox msg = new MessageBox(x, y, size, 20, coord, false);
        addRenderableWidget(msg);
        y += (5 + msg.getHeight());

        addRenderableWidget(new ButtonBuilder(Component.translatable("pplhelper.project.web"), (s) -> PepelandHelper.confirmLinkNow(this, String.format("https://h.pplmods.ru/projects/%s", project.id))).setPosition(x, y).setWidth(size).build());
        y+=25;
        if(PepelandHelper.isInstalledABI) {
            addRenderableWidget(new ButtonBuilder(Component.translatable((PepelandHelper.selectedProject == null || PepelandHelper.selectedProject.id != project.id) ? "pplhelper.project.abi" : "pplhelper.project.abi.unfollow"), (s) -> {
                PepelandHelper.selectedProject = (PepelandHelper.selectedProject == null || PepelandHelper.selectedProject.id != project.id) ? project : null;
                s.builder.setTitle(Component.translatable(PepelandHelper.selectedProject == null ? "pplhelper.project.abi" : "pplhelper.project.abi.unfollow"));
            }).setPosition(x, y).setWidth(size).build());
            y += 25;
        }

        maxY = y;
    }

    private ConfigureScrolWidget scroller;
    private List<AbstractWidget> widgets = new ArrayList<>();
    private final int maxSize = 400;
    public ResourceLocation lastBanner;
    public void initContent(){
        widgets = new ArrayList<>();
        int size = Math.min(maxSize, width-10);
        double scale = (double) size / maxSize;
        int x = (width-size) / 2;
        int y = 30;
        addRenderableWidget(new ButtonBuilder(Component.literal("x"), (s)->onClose()).setPosition( x+size-20, 5).setWidth(20).build()); //, 20, 20,
        addRenderableWidget(new ButtonBuilder(Component.translatable("pplhelper.project.web"), (s)->PepelandHelper.confirmLinkNow(this, String.format("https://h.pplmods.ru/projects/%s", project.id))).setSprite(WEB).setPosition( x, 5).setWidth(20).build()); //, 20, 20,
        addRenderableWidget(new TextBox(x+25, 5, size-50, 20, title, true));
        this.scroller = addRenderableWidget(new ConfigureScrolWidget(x+size - 3, y, 4, this.height-y, Component.empty(), scroller -> {
            scroller.innerHeight = 0;
            for(AbstractWidget widget : widgets){
                if(widget.visible){
                    if(widget instanceof ImageWidget) widget.setWidth(Math.min(widget.getWidth(), size));
                    else widget.setWidth(size);
                    widget.setPosition(x, (y+(int) (scroller.innerHeight - scroller.scrollAmount())));
                    scroller.innerHeight += (widget.getHeight()+((widget instanceof ImageWidget) ? 5 : (widget instanceof BannerWidget) ? 7 : 3));
                } else widget.setY(-widget.getHeight());
            }
            scroller.innerHeight-=8;
        }));
        if(project.banner != null && !project.banner.isEmpty()){
            lastBanner = TextureHelper.getBanner(project.banner, String.format("project_banner_%s", project.id));
            if(lastBanner != PACK_INFO) widgets.add(new BannerWidget(x, -160, size,(int) (160*scale), project.banner, String.format("project_banner_%s", project.id), Component.empty()));
        }
        widgets.add(new ScaledTextBox(x, -40, size, 12, Component.literal(project.title), true, 1.5f));
        widgets.add(new MessageBox(x, -40, size, 20, Component.literal(project.description), true));
        widgets.add(new Blockquote(x, -40, size, 20, Component.translatable("pplhelper.project.creators", project.creators), false));
        MutableComponent coord = Component.empty().append(Component.translatable("pplhelper.project.coordinates", project.world));
        if(project.coordinates$overworld != null && !project.coordinates$overworld.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.overworld")).append(": ").append(project.coordinates$overworld);
        if(project.coordinates$nether != null && !project.coordinates$nether.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.nether")).append(": ").append(project.coordinates$nether);
        if(project.coordinates$end != null && !project.coordinates$end.isEmpty())
            coord.append("\n").append(Component.translatable("pplhelper.project.coordinates.end")).append(": ").append(project.coordinates$end);
        Blockquote msg = new Blockquote(x, y, size, 20, coord, false);
        widgets.add(msg);
        if(PepelandHelper.isInstalledABI) {
            widgets.add(new ButtonBuilder(Component.translatable((PepelandHelper.selectedProject == null || PepelandHelper.selectedProject.id != project.id) ? "pplhelper.project.abi" : "pplhelper.project.abi.unfollow"), (s) -> {
                PepelandHelper.selectedProject = (PepelandHelper.selectedProject == null || PepelandHelper.selectedProject.id != project.id) ? project : null;
                s.builder.setTitle(Component.translatable(PepelandHelper.selectedProject == null ? "pplhelper.project.abi" : "pplhelper.project.abi.unfollow"));
            }).setPosition(x, y).setWidth(size).build());
        }
        widgets.add(new HorizontalRule(x, -4, size));
        widgets.addAll(parseMarkdown(project.content, x, size, String.format("project_%s_",project.id)+"%s", this));

        addWidgetsToScroller(widgets);
    }

    public void addWidgetsToScroller(List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) addWidgetsToScroller(widget);
    }

    public AbstractWidget addWidgetsToScroller(AbstractWidget widget) {
        this.scroller.addWidget(widget);
        return this.addWidget(widget);
    }

    protected void addRenderableWidgets(@NotNull List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }
    }



    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        int size = Math.min(maxSize, width-10);
        int x = (width-size) / 2;
        guiGraphics.enableScissor(0, 30, width, this.height);
        if (scroller != null) for (AbstractWidget widget : scroller.widgets) widget.render(guiGraphics, i, j, f);
        guiGraphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        int size = Math.min(maxSize, width-10);
        int x = (width-size) / 2;
        boolean st = true;
        GuiEventListener selected = null;
        for (GuiEventListener guiEventListener : this.children()) {
            if (scroller != null && scroller.widgets.contains(guiEventListener)) {
                if ((d >= x && d <= x + size) && e >= 30)
                    if (guiEventListener.mouseClicked(d, e, i)) {
                        st = false;
                        selected = guiEventListener;
                        break;
                    }
            } else if (guiEventListener.mouseClicked(d, e, i)) {
                st = false;
                selected = guiEventListener;
                break;
            }
        }

        this.setFocused(selected);
        if (i == 0)
            this.setDragging(true);

        return st;
    }

    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        int size = Math.min(maxSize, width-10);
        int x = (width-size) / 2;
        guiGraphics.fill(x-5, 0, x+size+5, height, Colors.BLACK_ALPHA); // Затемнение

        guiGraphics.fill(x+25, 5, x+size-25, 25, Colors.BLACK_ALPHA);
    }
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(parent);
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean scr = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!scr && scroller != null) scr = scroller.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return scr;
    }
    @Override
    public void tick(){
        if(scroller != null) scroller.onScroll.accept(scroller);
        if(project.banner != null && !project.banner.isEmpty() && lastBanner != TextureHelper.getTexture(project.banner, String.format("project_banner_%s", project.id), -1, 600))
            rebuildWidgets();
        super.tick();
    }

    public static List<AbstractWidget> parseMarkdown(String content, int x, int width, String idForImage, Screen screen){
        List<AbstractWidget> widgets = new ArrayList<>();
        String[] strings = parse(content, false).split("\n");
        boolean lastIsPlain = false;
        boolean lastIsBlockQuote = false;
        String plain = "";
        String blockquote = "";
        for(String string : strings){
            if(string.matches("!\\[(.+?)]\\((.+?)\\)")){
                if(lastIsPlain){
                    lastIsPlain = false;
                    plain = parse(plain, true);
                    widgets.add(new MessageBox(x, -40, width-230, 20, Component.literal(plain.substring(0, plain.length()-(plain.endsWith("\n") ? 1 : 0))), false));
                    plain = "";
                }
                if(lastIsBlockQuote){
                    lastIsBlockQuote = false;
                    blockquote = parse(blockquote, true);
                    widgets.add(new Blockquote(x, -40, width-230, 20, Component.literal(blockquote.substring(0, blockquote.length()-(blockquote.endsWith("\n") ? 1 : 0))), false));
                    blockquote = "";
                }
                String finalString = string;
                int iWidth = width-230;
                int iHeight = 20;
                if(TextureHelper.urlsTextures.containsKey(unparse(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$2")))){
                    NativeImage i = TextureHelper.urlsTextures.get(unparse(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$2"))).getPixels();
                    if(i != null) {
                        iWidth = i.getWidth();
                        iHeight = i.getHeight();
                    }
                }
                ResourceLocation image = TextureHelper.getTexture(unparse(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$2")), String.format(idForImage, unparse(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$1"))));
                if(image == PACK_INFO) widgets.add(new ButtonBuilder(Component.literal(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$1")),
                        Component.literal(unparse(string.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$2"))),
                        (s) -> PepelandHelper.confirmLinkNow(screen, unparse(finalString.replaceAll("!\\[(.+?)]\\((.+?)\\)", "$2")) )).setPosition(x, -40).setWidth(width-230).build());
                else {
                    widgets.add(new ImageWidget(x, -Integer.MAX_VALUE, iWidth, iHeight, image, iWidth, iHeight, true, Component.empty()));
                }
            } else if(string.startsWith("<hr") && string.endsWith(">")) {
                if(lastIsPlain){
                    lastIsPlain = false;
                    plain = parse(plain, true);
                    widgets.add(new MessageBox(x, -40, width-230, 20, Component.literal(plain.substring(0, plain.length()-(plain.endsWith("\n") ? 1 : 0))), false));
                    plain = "";
                }
                if(lastIsBlockQuote){
                    lastIsBlockQuote = false;
                    blockquote = parse(blockquote, true);
                    widgets.add(new Blockquote(x, -40, width-230, 20, Component.literal(blockquote.substring(0, blockquote.length()-(blockquote.endsWith("\n") ? 1 : 0))), false));
                    blockquote = "";
                }
                widgets.add(new HorizontalRule(x, -40, width-230));
            } else if(string.startsWith("#")){
                if(lastIsPlain){
                    lastIsPlain = false;
                    plain = parse(plain, true);
                    widgets.add(new MessageBox(x, -40, width-230, 20, Component.literal(plain.substring(0, plain.length()-(plain.endsWith("\n") ? 1 : 0))), false));
                    plain = "";
                }
                if(lastIsBlockQuote){
                    lastIsBlockQuote = false;
                    blockquote = parse(blockquote, true);
                    widgets.add(new Blockquote(x, -40, width-230, 20, Component.literal(blockquote.substring(0, blockquote.length()-(blockquote.endsWith("\n") ? 1 : 0))), false));
                    blockquote = "";
                }
                int j = 0;
                for(int i = 0; i<string.length() && string.split("")[i].equals("#"); i++) j = i;
                string = parse(string, true);
                widgets.add(new ScaledTextBox(x, -40, width-230, AlinLib.MINECRAFT.font.lineHeight+5, Component.literal(string.substring(j+(string.contains("# ") ? 2 : 0))), false, 1.5F-((float) j /6)));
            } else if(string.startsWith(">")) {
                if(lastIsPlain){
                    lastIsPlain = false;
                    plain = parse(plain, true);
                    widgets.add(new MessageBox(x, -40, width-230, 20, Component.literal(plain.substring(0, plain.length()-(plain.endsWith("\n") ? 1 : 0))), false));
                    plain = "";
                }
                if(!lastIsBlockQuote) lastIsBlockQuote = true;
                string = string.substring(string.contains("> ") ? 2 : 1);
                blockquote += string += "\n";
            } else {
                if(lastIsBlockQuote){
                    lastIsBlockQuote = false;
                    blockquote = parse(blockquote, true);
                    widgets.add(new Blockquote(x, -40, width-230, 20, Component.literal(blockquote.substring(0, blockquote.length()-(blockquote.endsWith("\n") ? 1 : 0))), false));
                    blockquote = "";
                }
                if(!lastIsPlain) lastIsPlain = true;
                if(!string.isBlank()) plain += string += "\n";
            }
        }
        if(lastIsPlain) widgets.add(new MessageBox(x, -40, width-230, 20, Component.literal(parse(plain, true).substring(0, parse(plain, true).length()-(parse(plain, true).endsWith("\n") ? 1 : 0))), false));
        if(lastIsBlockQuote) widgets.add(new Blockquote(x, -40, width-230, 20, Component.literal(parse(blockquote, true).substring(0, parse(blockquote, true).length()-(parse(blockquote, true).endsWith("\n") ? 1 : 0))), false));
        return widgets;
    }

    public static String parse(String string, boolean isLine){
        String ret = string.replaceAll("\\*\\*(.+?)\\*\\*(?!\\*)", "§l$1§r")
                .replaceAll("\\*(.+?)\\*(?!\\*)", "§o$1§r")
                .replaceAll("__(.+?)__(?!_)", "§n$1§r")
                .replaceAll("_(.+?)_(?!_)", "§o$1§r")
                .replaceAll("~~(.+?)~~(?!~)", "§m$1§r")
                .replaceAll("\\|\\|(.+?)\\|\\|(?!\\|)", "§k$1§r")
                .replace("<br>\n", "\n")
                .replace("\r", "");
        if(isLine){
            ret = ret.replace("<br>", "\n");
        }
        return ret;
    }
    public static String unparse(String string){
        return string.replaceAll("§l(.+?)§r", "**$1**")
                .replaceAll("§o(.+?)§r", "_$1_")
                .replaceAll("§n(.+?)§r", "__$1__")
                .replaceAll("§m(.+?)§r", "~~$1~~")
                .replaceAll("§k(.+?)§r", "||$1||");
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == GLFW.GLFW_KEY_ESCAPE) {
            if (getFocused() != null && getFocused().isFocused()) {
                getFocused().setFocused(false);
                return true;
            }
        }
        return super.keyPressed(i, j, k);
    }
}
