package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

// config

public class Main extends ListenerAdapter {
    private Database db;

    private final String[] permittedChannels = {
            "912353229285765172",  // verify channel
            "912353440749985852" // admin channel
    };

    //******************************
    //****CONFIG***
    //******************************
    public static class constants {
        public static final String[] logPrefixes = {"Module", "ERROR"};
        public static final boolean enableSocialForwarding = false;
        public static final boolean enableSSOVerification = false;
        public static final boolean enableTesting = true;
        public static JDA jda;
    }

    static String BOT_TOKEN = "ODc4OTQyNzk2MjYwNzI0NzY2.YSIhRA.ybuEYxDoa8VjfJQa0rC81W-ay4o";

    public static void main(String[] arguments) throws Exception {
        Close close = new Close();
        Runtime.getRuntime().addShutdownHook(close);
        JDA jda = JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.playing("Being Built"))
                .build();
        constants.jda = jda;
        News covid_news = new News("Covid");
       // todo detect new articles. Currently pushes whatever is the latest without checking.
        // News monash_news = new News("Monash");
    }


    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();
        News news;
        String msgContents = msg.getContentRaw();
        db = new Database();
        if (msgContents.contains(">")) {
            if (channel.getId().equals(permittedChannels[0])) {
                if (msgContents.equals(">verify")) {
                    SSOVerify newSSO = new SSOVerify(user, event.getGuild(), channel, db);
                }

            }
            // for commands with params
            if (channel.getId().equals(permittedChannels[1])) {
                if (msgContents.contains(">userLookup")) {
                    System.out.println("Running userLookup cmd");
                    // todo move this to a different class to prevent function envy
                    String[] parsedContents = msgContents.split(" ");

                    // return discord side of things like nickname, etc.
                    constants.jda.retrieveUserById(parsedContents[1]).map(User::getName).queue(name -> {
                        String discordName = name;
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("User lookup: ");
                        embed.setDescription("Discord Name: " + name + "\n" + db.getDBEntry("CERT", parsedContents[1]));
                        embed.setFooter("data sourced from internal database");
                        channel.sendMessage(embed.build()).queue();
                    });


                } else if (msgContents.contains(">manualUpdate")) {
                    // this might be a pain, will need a delimiter or set many params for fields
                }
            }
        }
    }
}