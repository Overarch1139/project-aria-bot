package com.github.echo2124;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.LinkedHashMap;

import static com.github.echo2124.Main.constants.*;

public class Main extends ListenerAdapter {
    public static class constants {
        public static final String[] logPrefixes = {"Module", "ERROR"};
        public static JDA jda;
        public static ActivityLog activityLog=null;
        public static boolean serviceMode=false;
        public static Database db=null;
        public static LinkedHashMap<String, Config> config;
        // 3 fields (discordid, name, addr)
        public static final int FIELD_NUM=3;
    }

    // todo switch to sys props instead of env vars (will make testing easier)
    public static void main(String[] arguments) throws Exception {
        // Initialise System Properties
        new InitSystemProps();
        // load config here
        ConfigParser parser = new ConfigParser();
        LinkedHashMap<String, Config> config =parser.parseDefaults();
        Main.constants.config=config;
        String activity="Loading...";
        if (System.getProperty("IS_DEV").contains("true")) {
            initDevMode();
            initModules();
        } else {
            constants.jda = initJDA(activity);
            initModules();
        }
    }


    public static JDA initJDA(String activity) {
        JDA jda=null;
        try {
            String BOT_TOKEN = System.getProperty("DISCORD_CLIENT_SECRET");
             jda = JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                    .addEventListeners(new Main())
                    .setActivity(Activity.playing(activity))
                    .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            System.out.println(e);

            System.out.println("[ERROR] UNABLE TO INIT JDA");
        }
        return jda;
    }

    public static void initModules() {
        activityLog = new ActivityLog();
        Close close = new Close();
        Runtime.getRuntime().addShutdownHook(close);
        activityLog.sendActivityMsg("[MAIN] Aria Bot is starting up...",1, null);
        db = new Database();
        //new News("Covid", db);
        // new News("Monash", db);
        // dual purpose loop - report config loaded & generate oncampus module for supported guilds
        // grabs from last, since we are using the same bot instance with different guilds the bot activity *must* remain the same
        for (String key: config.keySet()) {
            jda.getPresence().setActivity(Activity.playing(config.get(key).getActivityState()));
            activityLog.sendActivityMsg("Config File For "+config.get(key).getConfigName()+" has loaded successfully!", 1, key);
            if (config.get(key).getOnCampusModuleEnabled()) {
                new OnCampus(false, config.get(key).getServerId());
            }
        }
        activityLog.sendActivityMsg("[MAIN] Aria Bot has initialised successfully!",1, null);
    }

    public static void initDevMode() {
        if (!System.getProperty("EMULATED_GUILD").contains("null")) {
            String serverId=System.getProperty("EMULATED_GUILD");
            String[] parsedModuleList = System.getProperty("ENABLED_MODULES").split(",");
            for (int i=0; i<parsedModuleList.length; i++) {
                switch(parsedModuleList[i]) {
                    case "sheetParser":
                        //new SheetParser(null, serverId,2);
                        break;
                    case "covidUpdate":
                        // **requires db
                        new News("Covid", db);
                        break;
                    case "monashNews":
                        // **requires db
                        new News("Monash", db);
                        break;
                    default:
                        System.out.println("[ERROR] INVALID MODULE SELECTED");
                }
            }
        } else {
            System.out.println("[ERROR] NO VALID GUILD TO EMULATE");
        }
    }


    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        String msgContents = msg.getContentRaw();
        if (msgContents.contains(">")) {
            new Command(event);
        }
    }
}