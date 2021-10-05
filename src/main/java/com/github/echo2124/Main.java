package com.github.echo2124;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

// config

public class Main extends ListenerAdapter {
    //******************************
    //****CONFIG***
    //******************************
    public final boolean enableSocialForwarding=false;
    public final boolean enableSSOVerification=false;
    public final boolean enableTesting=true;

    static String BOT_TOKEN = "ODc4OTQyNzk2MjYwNzI0NzY2.YSIhRA.ybuEYxDoa8VjfJQa0rC81W-ay4o";
    public static void main(String[] arguments) throws Exception {
        JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.playing("Being Built"))
                .build();
    }
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        News news;
        String msgContents=msg.getContentRaw();
        if (msgContents.contains(">")) {
            switch (msgContents) {
                case ">verify":
                    SSOVerify newSSO = new SSOVerify();
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
        if (enableTesting==true) {
            if (msg.getContentRaw().equals("!ping")) {
                MessageChannel channel = event.getChannel();
                long time = System.currentTimeMillis();
                channel.sendMessage("Pong!") /* => RestAction<Message> */
                        .queue(response /* => Message */ -> {
                            response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
                        });
            }
        }
    }
}