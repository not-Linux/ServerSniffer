package com.serversniffer;

import com.serversniffer.commands.ScanCommand;
import com.serversniffer.commands.ServerInfoCommand;
import com.serversniffer.hud.ServerHud;
import com.serversniffer.modules.ServerSnifferModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ServerSnifferAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("ServerSniffer");
    public static final HudGroup HUD_GROUP = new HudGroup("ServerSniffer");

    @Override
    public void onInitialize() {
        LOG.info("Initializing ServerSniffer — sniffing servers...");

        Modules.get().add(new ServerSnifferModule());
        Commands.add(new ServerInfoCommand());
        Commands.add(new ScanCommand());
        Hud.get().register(ServerHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() { return "com.serversniffer"; }

    @Override
    public GithubRepo getRepo() { return new GithubRepo("not-Linux", "ServerSniffer"); }
}
