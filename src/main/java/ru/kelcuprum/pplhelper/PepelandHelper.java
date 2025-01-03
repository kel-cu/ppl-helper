package ru.kelcuprum.pplhelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;
import org.meteordev.starscript.value.Value;
import ru.kelcuprum.alinlib.AlinLib;
import ru.kelcuprum.alinlib.AlinLogger;
import ru.kelcuprum.alinlib.api.KeyMappingHelper;
import ru.kelcuprum.alinlib.api.events.alinlib.LocalizationEvents;
import ru.kelcuprum.alinlib.api.events.client.ClientLifecycleEvents;
import ru.kelcuprum.alinlib.api.events.client.ClientTickEvents;
import ru.kelcuprum.alinlib.api.events.client.TextureManagerEvent;
import ru.kelcuprum.alinlib.config.Config;
import ru.kelcuprum.alinlib.config.Localization;
import ru.kelcuprum.alinlib.gui.GuiUtils;
import ru.kelcuprum.alinlib.gui.components.builder.AbstractBuilder;
import ru.kelcuprum.alinlib.gui.components.builder.button.ButtonBuilder;
import ru.kelcuprum.alinlib.gui.screens.ConfirmScreen;
import ru.kelcuprum.alinlib.gui.toast.ToastBuilder;
import ru.kelcuprum.pplhelper.api.PepeLandAPI;
import ru.kelcuprum.pplhelper.api.PepeLandHelperAPI;
import ru.kelcuprum.pplhelper.api.components.Project;
import ru.kelcuprum.pplhelper.gui.TextureHelper;
import ru.kelcuprum.pplhelper.gui.screens.configs.ConfigScreen;
import ru.kelcuprum.pplhelper.gui.screens.UpdaterScreen;
import ru.kelcuprum.pplhelper.gui.screens.message.NewUpdateScreen;
import ru.kelcuprum.pplhelper.gui.screens.CommandsScreen;
import ru.kelcuprum.pplhelper.gui.screens.ModsScreen;
import ru.kelcuprum.pplhelper.gui.screens.NewsListScreen;
import ru.kelcuprum.pplhelper.gui.screens.ProjectsScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static ru.kelcuprum.alinlib.gui.Icons.*;
import static ru.kelcuprum.pplhelper.PepelandHelper.Icons.COMMANDS;
import static ru.kelcuprum.pplhelper.PepelandHelper.Icons.PROJECTS;

public class PepelandHelper implements ClientModInitializer {
    public static final AlinLogger LOG = new AlinLogger("PPL Helper");
    public static Config config = new Config("config/pplhelper/config.json");
    public static boolean isInstalledABI = FabricLoader.getInstance().isModLoaded("actionbarinfo");
    public static String[] worlds = new String[0];
    public static JsonArray commands = new JsonArray();
    public static JsonArray mods = new JsonArray();
    public static Project selectedProject;

    public static AbstractBuilder[] getPanelWidgets(Screen parent, Screen current){
        return new AbstractBuilder[]{
                new ButtonBuilder(Component.translatable("pplhelper.news")).setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new NewsListScreen(current))).setIcon(WIKI).setCentered(false).setSize(20, 20),
                new ButtonBuilder(Component.translatable("pplhelper.projects")).setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new ProjectsScreen(current))).setIcon(PROJECTS).setCentered(false).setSize(20, 20),
                new ButtonBuilder(Component.translatable("pplhelper.commands")).setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new CommandsScreen().build(parent))).setIcon(COMMANDS).setCentered(false).setSize(20, 20),
                new ButtonBuilder(Component.translatable("pplhelper.mods")).setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new ModsScreen().build(parent))).setIcon(Icons.MODS).setCentered(false).setSize(20, 20),
                new ButtonBuilder(Component.translatable("pplhelper.pack")).setOnPress((s) -> AlinLib.MINECRAFT.setScreen(new UpdaterScreen().build(parent))).setIcon(Icons.PACK_INFO).setCentered(false).setSize(20, 20),
        };
    }

    public static void executeCommand(LocalPlayer player, String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
            player.connection.sendCommand(command);
        } else {
            player.connection.sendChat(command);
        }
    }

    @Override
    public void onInitializeClient() {
        LOG.log("Данный проект не является официальной частью сети серверов PepeLand", Level.WARN);
        ClientLifecycleEvents.CLIENT_FULL_STARTED.register((s) -> {
            try {
                worlds = PepeLandHelperAPI.getWorlds();
                commands = PepeLandHelperAPI.getCommands();
                mods = PepeLandHelperAPI.getRecommendMods();
            } catch (Exception ex){
                ex.printStackTrace();
            }
            String packVersion = getInstalledPack();
            if((config.getBoolean("PACK_UPDATES.NOTICE", true) || config.getBoolean("PACK_UPDATES.AUTO_UPDATE", true)) && !packVersion.isEmpty()){
                JsonObject packInfo = PepeLandAPI.getPackInfo(onlyEmotesCheck());
                if(config.getBoolean("PACK_UPDATES.NOTICE", true) && !config.getBoolean("PACK_UPDATES.AUTO_UPDATE", false)){
                    if(!packInfo.get("version").getAsString().contains(packVersion))
                        AlinLib.MINECRAFT.setScreen(new NewUpdateScreen(s.screen, packVersion, packInfo));
                } else if(config.getBoolean("PACK_UPDATES.AUTO_UPDATE", false)) {
                    if(!packInfo.get("version").getAsString().contains(packVersion)) {
                        PepelandHelper.downloadPack(packInfo, onlyEmotesCheck(), (ss) -> {
                            if(ss) {
                                String fileName = String.format("pepeland-%1$s-v%2$s.zip", onlyEmotesCheck() ? "emotes" : "main", packInfo.get("version").getAsString());
                                AlinLib.MINECRAFT.getResourcePackRepository().reload();
                                for(Pack pack : AlinLib.MINECRAFT.getResourcePackRepository().getSelectedPacks()){
                                    if(pack.getDescription().getString().contains("PepeLand Pack"))
                                        AlinLib.MINECRAFT.getResourcePackRepository().removePack(pack.getId());
                                }
                                for(Pack pack : AlinLib.MINECRAFT.getResourcePackRepository().getAvailablePacks()){
                                    if(pack.getId().contains(fileName))
                                        AlinLib.MINECRAFT.getResourcePackRepository().addPack(pack.getId());
                                }
                                AlinLib.MINECRAFT.options.updateResourcePacks(AlinLib.MINECRAFT.getResourcePackRepository());

                                new ToastBuilder().setTitle(Component.translatable("pplhelper"))
                                        .setIcon(PepelandHelper.Icons.WHITE_PEPE)
                                        .setMessage(Component.translatable("pplhelper.pack.downloaded", packInfo.get("version").getAsString())).buildAndShow();
                            }
                            else new ToastBuilder().setTitle(Component.translatable("pplhelper")).setMessage(Component.translatable("pplhelper.pack.file_broken")).setIcon(DONT).buildAndShow();
                        });
                    }
                }
            }
        });
        KeyMapping key1 = KeyMappingHelper.register(new KeyMapping(
                "pplhelper.key.open.projects",
                GLFW.GLFW_KEY_H, // The keycode of the key
                "pplhelper"
        ));
        KeyMapping key2 = KeyMappingHelper.register(new KeyMapping(
                "pplhelper.key.open.news",
                GLFW.GLFW_KEY_UNKNOWN, // The keycode of the key
                "pplhelper"
        ));
        KeyMapping key3 = KeyMappingHelper.register(new KeyMapping(
                "pplhelper.key.open.config",
                GLFW.GLFW_KEY_UNKNOWN, // The keycode of the key
                "pplhelper"
        ));
        KeyMapping key4 = KeyMappingHelper.register(new KeyMapping(
                "pplhelper.key.unfollow_project",
                GLFW.GLFW_KEY_UNKNOWN, // The keycode of the key
                "pplhelper"
        ));
        ClientTickEvents.START_CLIENT_TICK.register((s) -> {
            if(key1.consumeClick()) AlinLib.MINECRAFT.setScreen(new ProjectsScreen(AlinLib.MINECRAFT.screen));
            if(key2.consumeClick()) AlinLib.MINECRAFT.setScreen(new NewsListScreen(AlinLib.MINECRAFT.screen));
            if(key3.consumeClick()) AlinLib.MINECRAFT.setScreen(new ConfigScreen().build(AlinLib.MINECRAFT.screen));
            if(key4.consumeClick()) selectedProject = null;
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register((s) -> TextureHelper.saveMap());
        TextureManagerEvent.INIT.register(TextureHelper::loadTextures);
        LocalizationEvents.DEFAULT_PARSER_INIT.register(starScript -> starScript.ss.set("pplhelper.world", () -> {
            TabHelper.Worlds world = TabHelper.getWorld();
            return Value.string(world == null ? "" : world.title.getString());
        }).set("pplhelper.world_short", () -> {
            TabHelper.Worlds world = TabHelper.getWorld();
            return Value.string(world == null ? "" : world.shortName);
        }).set("pplhelper.tps", () -> Value.number(TabHelper.getTPS()))
                .set("pplhelper.online", () -> Value.number(TabHelper.getOnline()))
                .set("pplhelper.max_online", () -> Value.number(TabHelper.getMaxOnline())));
    }

    public static boolean onlyEmotesCheck(){
        return !FabricLoader.getInstance().isModLoaded("citresewn") || config.getBoolean("PACK_UPDATES.ONLY_EMOTE", false);
    }

    public static String getInstalledPack(){
        String packVersion = "";
        for(Pack pack : AlinLib.MINECRAFT.getResourcePackRepository().getAvailablePacks()){
            if(Localization.clearFormatCodes(pack.getDescription().getString()).contains("PepeLand Pack") && AlinLib.MINECRAFT.getResourcePackRepository().getSelectedPacks().contains(pack)){
                String[] info = Localization.clearFormatCodes(pack.getDescription().getString()).split("v");
                if(info.length > 1) packVersion = info[1];
            }
        }
        return packVersion;
    }
    public static String getAvailablePack(){
        String packId = "";
        for(Pack pack : AlinLib.MINECRAFT.getResourcePackRepository().getAvailablePacks()) {
            if (Localization.clearFormatCodes(pack.getDescription().getString()).contains("PepeLand Pack") && !AlinLib.MINECRAFT.getResourcePackRepository().getSelectedPacks().contains(pack)) {
                packId = pack.getId();
                break;
            }
        }
        return packId;
    }
    public static boolean playerInPPL(){
        return config.getBoolean("IM_A_TEST_SUBJECT", false) || (AlinLib.MINECRAFT.getCurrentServer() != null && AlinLib.MINECRAFT.getCurrentServer().ip.contains("pepeland.net"));
    }
    public interface Icons {
        ResourceLocation WHITE_PEPE = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/white_pepe.png");
        ResourceLocation PEPE = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/pepe.png");
        ResourceLocation PACK_INFO = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/pack_info.png");
        ResourceLocation PROJECTS = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/projects.png");
        ResourceLocation COMMANDS = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/commands.png");
        ResourceLocation MODS = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/mods.png");
        ResourceLocation WEB = GuiUtils.getResourceLocation("pplhelper", "textures/gui/sprites/web.png");
    }

    public static Thread downloadPack(JsonObject packData, boolean onlyEmote, BooleanConsumer consumer){
        Thread thread = new Thread(() -> {
            try {
                String originalChecksum = packData.get("checksum").getAsString();
                String path = AlinLib.MINECRAFT.getResourcePackDirectory().resolve(String.format("pepeland-%1$s-v%2$s.zip", onlyEmote ? "emotes" : "main", packData.get("version").getAsString())).toString();
                File file = new File(path);
                if(!file.exists()) PepeLandAPI.downloadFile$queue(packData.get("url").getAsString(), AlinLib.MINECRAFT.getResourcePackDirectory().toString(), String.format("pepeland-%1$s-v%2$s.zip", onlyEmote ? "emotes" : "main", packData.get("version").getAsString()), originalChecksum, 5);
                if(file.exists() && originalChecksum.contains(toSHA(path))){
                    consumer.accept(true);
                } else {
                    if(file.exists()) file.deleteOnExit();
                    throw new RuntimeException(Component.translatable("pplhelper.pack.file_broken").getString());
                }
            } catch (Exception e) {
                LOG.error(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
                consumer.accept(false);
            }
        });
        thread.start();
        return thread;
    }

    public static String toSHA(String filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(filePath);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        fis.close();

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void confirmLinkNow(Screen screen, String link) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(screen, Icons.WHITE_PEPE, Component.translatable("pplhelper"), Component.translatable("chat.link.confirmTrusted"), link));
    }
}
