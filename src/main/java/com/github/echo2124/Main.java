package com.github.echo2124;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

// config

public class Main extends ListenerAdapter {
    //******************************
    //****CONFIG***
    //******************************
    public static class constants {
        public static final String[] logPrefixes={"Module", "ERROR"};
        public static final boolean enableSocialForwarding=false;
        public static final boolean enableSSOVerification=false;
        public static final boolean enableTesting=true;
    }

    static String BOT_TOKEN = "ODc4OTQyNzk2MjYwNzI0NzY2.YSIhRA.ybuEYxDoa8VjfJQa0rC81W-ay4o";
    public static void main(String[] arguments) throws Exception {
        JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.playing("Being Built"))
                .build();
    }
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        User user= event.getAuthor();
        MessageChannel channel=event.getChannel();
        News news;
        String msgContents=msg.getContentRaw();
        if (msgContents.contains(">")) {
            switch (msgContents) {
                case ">verify":
                    SSOVerify newSSO = new SSOVerify(user, event.getGuild(), channel);
                    break;
                case ">test":
                    break;
                case ">covid":
                    news = new News("Covid", msg);
                    break;
                case ">monashUpdates":
                    news = new News("Monash", msg);
                    break;

                default:
                    System.out.println("Invalid command");
            }
        }


        if (constants.enableTesting==true) {
            }
        }
    }