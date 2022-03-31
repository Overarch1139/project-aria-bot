package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.*;

import static com.github.echo2124.Main.constants.*;
import static java.lang.System.getenv;

// config

public class Main extends ListenerAdapter {




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
        public static Database db=null;
        public static boolean serviceMode=false;
        public static String ONCAMPUS_ROLE_NAME="On Campus";
        public static String ONCAMPUS_CHANNEL_NAME="oncampus";
        public static String EXPOSURE_SITE_CHANNEL="951902910759977001";

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
            constants.NEWS_CHANNEL="927941422512996353";
            constants.EXPOSURE_SITE_CHANNEL="927941422512996353";
            constants.ONCAMPUS_CHANNEL_NAME="oncampus";
            constants.ONCAMPUS_ROLE_NAME="oncampus";
        }
        Close close = new Close();
        Runtime.getRuntime().addShutdownHook(close);
        JDA jda = JDABuilder.createLight(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.playing(activity))
                .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .build();
        constants.jda = jda;
        db = new Database();
         new News("Covid", db);
        new News("Monash", db);
        OnCampus x =new OnCampus(false);
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
                    embed.addField(">verifyinfo", "This command will return any collected information associated with your discord id when you were verified. You will be sent a private message with information related to this.",false);
                    embed.addField(">about","Details information about the bot", false);
                    embed.addField("[ADMIN ONLY] >userLookup <discordID>", "This command will lookup a user's verification status and other recorded details.", false);
                    embed.addField("[WIP - ADMIN ONLY] >userUpdate <discordID>", "Will be used by staff to update information or manually verify a user", false);
                    embed.addField("[WIP - ADMIN ONLY] >scheduleMsg <Message> <Timestamp>","Can be used to schedule an announcement for a particular time.", false);
                    channel.sendMessage(embed.build()).queue();

                    } else if (msgContents.equals(">verifyinfo")) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("User lookup: ");
                    try {
                        String id=msg.getAuthor().getId();
                        embed.setDescription("This command has returned **all** information associated with your account that was collected during the verification process.");
                        if (db.getDBEntry("CERT", id).equals("No results found")) {
                            embed.setColor(Color.RED);
                            embed.addField("Status:", "Your account has not been verified therefore there is no collected data associated with your discord id", false);
                        } else {
                            embed.setColor(Color.ORANGE);
                            embed.addField("Status:", db.getDBEntry("CERT", id), false);
                        }
                        embed.setFooter("Data sourced from Aria's internal database");
                    } catch (Exception e) {
                        System.out.println("Long failed");
                        embed.setDescription("**Lookup failed, please try again later");
                        embed.setFooter("data sourced from internal database");
                    }
                    msg.getAuthor().openPrivateChannel().flatMap(verifyinfoch -> verifyinfoch.sendMessage(embed.build())).queue();
                    channel.sendMessage(user.getAsMention() + " , Please check your DMs, you should receive your verification data there.").queue();
                    }

                else if (msgContents.equals(">exposureBuilding")) {
                    new News("ExposureBuilding",db);
                } else if (msgContents.equals(">exposureClass")) {
                    new News("ExposureClass",db);
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

                } else if (msgContents.contains(">resetOnCampus")) {
                    OnCampus x =new OnCampus(true);
                    channel.sendMessage("On Campus feature has been successfully reset!");
                } else if (msgContents.contains(">serviceMode")) {
                    String[] parsedContents = msgContents.split(" ");
                    serviceMode=true;
                    Misc misc = new Misc();
                    MessageChannel verify= Main.constants.jda.getTextChannelById(Main.constants.permittedChannels[0]);
                    misc.sendServiceModeMsg(verify,"Aria is currently in maintenance mode. The ability to verify has now been temporarily disabled, the estimated downtime will be "+parsedContents[1]+". Sorry for any inconvenience.");
                } else if (msgContents.contains(">reactivate")) {
                    Misc misc = new Misc();
                    MessageChannel verify= Main.constants.jda.getTextChannelById(Main.constants.permittedChannels[0]);
                    misc.sendServiceModeMsg(verify,"Aria has reactivated the ability to verify and has exited maintenance mode.");
                } else if (msgContents.contains(">help")) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setColor(Color.MAGENTA);
                    embed.setTitle("ADMIN Commands");
                    embed.setDescription("Here are the following commands that you are able to use:");
                    embed.addField(">userLookup <discordID>", "This command will lookup a user's verification status and other recorded details.", false);
                    embed.addField(">reactivate", "Will re-enable the ability to verify and other parts of the bot that have been deactivated", false);
                    embed.addField(">serviceMode <Time> E.g. 10mins","Can be used to deactivate interruption sensitive parts of the bot, e.g. verify module", false);
                    channel.sendMessage(embed.build()).queue();
                }
            }
        }
    }