package com.serversniffer;

import com.serversniffer.commands.ScanCommand;
import com.serversniffer.commands.ServerInfoCommand;
import com.serversniffer.hud.ServerHud;
import com.serversniffer.modules.ServerSnifferModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.hud.Hud;
import meteordevelopment.meteorclient.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ServerSnifferAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("ServerSniffer");
    public static final HudGroup HUD_GROUP = new HudGroup("ServerSniffer");

    public static HudElementInfo<ServerHud> HUD_INFO;

    @Override
    public void onInitialize() {
        LOG.info("Initializing ServerSniffer v{} — sniffing servers…", getPackage().getImplementationVersion());

        // Modules
        Modules.get().add(new ServerSnifferModule());

        // Commands
        Commands.add(new ServerInfoCommand());
        Commands.add(new ScanCommand());

        // HUD
        HUD_INFO = new HudElementInfo<>(HUD_GROUP, "server-hud", "Shows sniffed server intel on-screen.", ServerHud::new);
        Hud.get().register(HUD_INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() { return "com.serversniffer"; }

    @Override
    public GithubRepo getRepo() { return new GithubRepo("yourname", "ServerSniffer"); }
}
