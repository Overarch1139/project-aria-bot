package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.*;

import static java.lang.System.getenv;

// config

public class Main extends ListenerAdapter {
    private static Database db;




    //******************************
    //****CONFIG***
    //******************************
    public static class constants {
        public static final String[] logPrefixes = {"Module", "ERROR"};
        public static final boolean enableSocialForwarding = false;
        public static final boolean enableSSOVerification = false;
        public static final boolean enableTesting = true;
        public static JDA jda;
        public static final String IT_SERVER="802526304745553930";
        public final static String[] permittedChannelsTest = {
                "912353229285765172",  // verify channel
                "912353440749985852" // admin channel
        };
        // for actual location
        public static String[] permittedChannels = {
                "913081082298114058",  // verify channel
                "913082023483174922" // admin channel
        };
        public static final String VERIFIED_ROLE_ID_TEST="909827233194070039";
        public static String VERIFIED_ROLE_ID="912001525432320031";
        public static String VERIFY_TIMEOUT_ROLE_ID="914896421965148160";
        public static final String NEWS_CHANNEL_TEST="912355120723943424";
        // for monash news
        public static String NEWS_CHANNEL="913082864080392213";
        public static final String COVID_UPDATE_CHANNEL_TEST="912726004886294569";
        public static String COVID_UPDATE_CHANNEL="913081128188014592";

    }
    // actual bot ODc4OTQyNzk2MjYwNzI0NzY2.YSIhRA.ybuEYxDoa8VjfJQa0rC81W-ay4o
    // test bot aOTEzNDUxMTY2MzcxODQwMDIw.YZ-rsQ.jO6C-N-7dRKJPjMXkfrWTrEFd4I


    public static void main(String[] arguments) throws Exception {
        String activity ="Open Beta Active!";
        // setters for various props
        String BOT_TOKEN = "ODc4OTQyNzk2MjYwNzI0NzY2.YSIhRA.ybuEYxDoa8VjfJQa0rC81W-ay4o";
        if (Boolean.parseBoolean(System.getenv("IS_DEV"))) {
            BOT_TOKEN = "OTEzNDUxMTY2MzcxODQwMDIw.YZ-rsQ.jO6C-N-7dRKJPjMXkfrWTrEFd4I";
            activity="Dev Build Active";
            constants.VERIFIED_ROLE_ID="909827233194070039";
            constants.COVID_UPDATE_CHANNEL="912726004886294569";
            constants.permittedChannels[0]="912353229285765172";
            constants.permittedChannels[1]="912353440749985852";
        }
        Close close = new Close();
        Runtime.getRuntime().addShutdownHook(close);
        JDA jda = JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.playing(activity))
                .build();
        constants.jda = jda;
        News covid_news = new News("Covid");
       // todo detect new articles. Currently pushes whatever is the latest without checking.
        // News monash_news = new News("Monash");
        db = new Database();
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        User user = event.getAuthor();
        MessageChannel channel = event.getChannel();
        News news;
        String msgContents = msg.getContentRaw();

        if (msgContents.contains(">")) {
            if (channel.getId().equals(constants.permittedChannels[0])) {
                if (msgContents.equals(">verify")) {
                   SSOVerify newVerify= new SSOVerify(user, event.getGuild(), channel, db);
                   newVerify.start();
                    // add timeout here. After 5 mins check if user is verified if not then return failure msg (timeout)
                } else if (msgContents.equals(">about")) {
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(Color.CYAN);
                        embed.setTitle("About me");
                        embed.setDescription("I am Aria, I help out the staff on the server with various administrative tasks and other stuff.");
                        embed.addField("Why am I called Aria?", "My name is actually an acronym: **A**dministrate, **R**elay, **I**dentify, **A**ttest. I was built to cater to this functionality.", false);
                        embed.addField("Who built me?", "I was built entirely by Echo2124 (Joshua) as a side project that aims to automate many different tasks, such as verifying users, automatically relaying local COVID information & announcements from Monash Uni.", false);
                        channel.sendMessage(embed.build()).queue();
                    } else if (msgContents.equals(">help")) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Color.MAGENTA);
                    embed.setTitle("Commands");
                    embed.setDescription("Here are the following commands that you are able to use");
                    embed.addField(">verify", "This command will initiate a verification check for the user. You will be sent a private message with information related to this.",false);
                    embed.addField(">about","Details information about the bot", false);
                    embed.addField("[ADMIN ONLY] >userLookup <discordID>", "This command will lookup a user's verification status and other recorded details.", false);
                    embed.addField("[WIP - ADMIN ONLY] >userUpdate <discordID>", "Will be used by staff to update information or manually verify a user", false);
                    embed.addField("[WIP - ADMIN ONLY] >scheduleMsg <Message> <Timestamp>","Can be used to schedule an announcement for a particular time.", false);
                    channel.sendMessage(embed.build()).queue();

                    }
                } else if (channel.getId().equals(constants.NEWS_CHANNEL)) {
                            String[] parsedContents = msgContents.split(" ");
                           if (msgContents.contains(">monashUpdate")) {
                                new News("Monash",parsedContents[1]);
                        }
                }
            }

        // TODO: Move this to database class
            // for commands with params
            if (channel.getId().equals(constants.permittedChannels[1])) {
                if (msgContents.contains(">userLookup")) {
                    System.out.println("Running userLookup cmd");
                    // todo move this to a different class to prevent function envy
                    String[] parsedContents = msgContents.split(" ");
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("User lookup: ");
                        try {
                            Long.parseLong(parsedContents[1]);
                            if (!msg.getMentionedUsers().isEmpty()) {
                                User x= msg.getMentionedUsers().get(0);
                                embed.setDescription("Results for: " +  x.getId()+"\n" + db.getDBEntry("CERT", x.getId()));
                            } else {
                                embed.setDescription("Results for: " + parsedContents[1] + "\n" + db.getDBEntry("CERT", parsedContents[1]));
                            }
                            embed.setFooter("data sourced from internal database");
                        } catch (Exception e) {
                            System.out.println("Long failed");
                            embed.setDescription("**Lookup failed, please ensure you've correctly copied the discord ID**");
                            embed.setFooter("data sourced from internal database");
                        }
                    channel.sendMessage(embed.build()).queue();

                } else if (msgContents.contains(">manualUpdate")) {
                    // this might be a pain, will need a delimiter or set many params for fields
                }
            }
        }
    }