package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.*;

import static com.github.echo2124.Main.constants.*;

public class Command {
    Message msg;
    User user;
    MessageChannel channel;
    String msgContents;
    MessageReceivedEvent event;
    String serverId;

    public Command(MessageReceivedEvent event) {
        // Assign instance vars
        this.msg = event.getMessage();
        this.user = event.getAuthor();
        this.channel = event.getChannel();
        this.msgContents = msg.getContentRaw();
        this.serverId="";
        this.event=event;
        if (event.isFromGuild()) {
            this.serverId = event.getGuild().getId();
        }
        if (channel.getId().equals(config.get(serverId).getChannelVerifyId())) {
            VerifiedModule();
        }
        if (channel.getId().equals(config.get(serverId).getChannelAdminId())) {
            AdminModule();
        }
    }

    public void VerifiedModule() {
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
            channel.sendMessageEmbeds(embed.build()).queue();
            activityLog.sendActivityMsg("[MAIN] about command triggered!",1, serverId);
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
            channel.sendMessageEmbeds(embed.build()).queue();
            activityLog.sendActivityMsg("[MAIN] help command triggered!",1, serverId);
        } else if (msgContents.equals(">verifyinfo")) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("User lookup: ");
            try {
                activityLog.sendActivityMsg("[MAIN] User lookup command triggered",1, serverId);
                String id=msg.getAuthor().getId();
                embed.setDescription("This command has returned **all** information associated with your account that was collected during the verification process.");
                if (db.getDBEntry("CERT", id+"##"+serverId).equals("No results found")) {
                    embed.setColor(Color.RED);
                    embed.addField("Status:", "Your account has not been verified therefore there is no collected data associated with your discord id", false);
                } else {
                    embed.setColor(Color.ORANGE);
                    embed.addField("Status:", db.getDBEntry("CERT", id+"##"+serverId), false);
                }
                embed.setFooter("Data sourced from Aria's internal database");
            } catch (Exception e) {
                System.out.println("Long failed");
                embed.setDescription("**Lookup failed, please try again later");
                embed.setFooter("data sourced from internal database");
            }
            msg.getAuthor().openPrivateChannel().flatMap(verifyinfoch -> verifyinfoch.sendMessageEmbeds(embed.build())).queue();
            channel.sendMessage(user.getAsMention() + " , Please check your DMs, you should receive your verification data there.").queue();
        }
    }

    public void AdminModule() {
        if (msgContents.contains(">userLookup")) {
            System.out.println("Running userLookup cmd");
            String[] parsedContents = msgContents.split(" ");
            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle("User lookup: ");
            try {
                Long.parseLong(parsedContents[1]);
                if (!msg.getMentions().getUsers().isEmpty()) {
                    User x= msg.getMentions().getUsers().get(0);
                    embed.setDescription("Results for: " +  x.getId()+"\n" + db.getDBEntry("CERT", x.getId()+"##"+serverId));
                } else {
                    embed.setDescription("Results for: " + parsedContents[1] + "\n" + db.getDBEntry("CERT", parsedContents[1]+"##"+serverId));
                }
                embed.setFooter("data sourced from internal database");
            } catch (Exception e) {
                System.out.println("Long failed");
                System.out.println(ExceptionUtils.getStackTrace(e));
                activityLog.sendActivityMsg("[MAIN] "+e.getMessage(),3, serverId);
                embed.setDescription("**Lookup failed, please ensure you've correctly copied the discord ID**");
                embed.setFooter("data sourced from internal database");
            }
            channel.sendMessageEmbeds(embed.build()).queue();

        } else if (msgContents.contains(">manualVerify")) {
            activityLog.sendActivityMsg("[MAIN] Manual verify active", 2, serverId);
            SSOVerify verify= new SSOVerify(user, event.getGuild(), channel, db);
            verify.manualModify(msgContents, msg.getMember(), channel,0);
        }
        else if (msgContents.contains(">manualDelete")) {
            activityLog.sendActivityMsg("[MAIN] Manual delete active", 2, serverId);
            SSOVerify verify= new SSOVerify(user, event.getGuild(), channel, db);
            verify.manualModify(msgContents, msg.getMember(), channel,1);
        }
        else if (msgContents.contains(">resetOnCampus")) {
            try {
                OnCampus x = new OnCampus(true, event.getGuild().getId());
                activityLog.sendActivityMsg("[MAIN] resetOnCampus command has been activated!",2, serverId);
                channel.sendMessage("On Campus feature has been successfully reset!");
            } catch (Exception e) {
                activityLog.sendActivityMsg("[MAIN] unable to generate oncampus module", 3, serverId);
            }

        } else if (msgContents.contains(">serviceMode")) {
            String[] parsedContents = msgContents.split(" ");
            serviceMode=true;
            Misc misc = new Misc();
            MessageChannel verify= Main.constants.jda.getTextChannelById(config.get(serverId).getChannelVerifyId());
            misc.sendServiceModeMsg(verify,"Aria is currently in maintenance mode. The ability to verify has now been temporarily disabled, the estimated downtime will be "+parsedContents[1]+". Sorry for any inconvenience.");
            activityLog.sendActivityMsg("[MAIN] Service mode is now active",2, serverId);
        } else if (msgContents.contains(">reactivate")) {
            Misc misc = new Misc();
            MessageChannel verify = Main.constants.jda.getTextChannelById(config.get(serverId).getChannelVerifyId());
            misc.sendServiceModeMsg(verify, "Aria has reactivated the ability to verify and has exited maintenance mode.");
            activityLog.sendActivityMsg("[MAIN] Aria bot has exited service mode", 2, serverId);
        } else if (msgContents.contains(">parseSheet")) {
            new ClubMember(msg.getAttachments().get(0), serverId, null, null, 1);
            activityLog.sendActivityMsg("[MAIN] Parse sheet active!", 1, serverId);
        } else if (msgContents.contains(">activateClubSupervisor")) {
            if (Main.constants.config.get(serverId).getSheetParserModuleEnabled()) {
                new ClubMember(null, serverId, null, null, 0);
            }
        } else if (msgContents.contains(">help")) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.MAGENTA);
            embed.setTitle("ADMIN Commands");
            embed.setDescription("Here are the following commands that you are able to use:");
            embed.addField(">userLookup <discordID>", "This command will lookup a user's verification status and other recorded details.", false);
            embed.addField(">reactivate", "Will re-enable the ability to verify and other parts of the bot that have been deactivated", false);
            embed.addField(">serviceMode <Time> E.g. 10mins","Can be used to deactivate interruption sensitive parts of the bot, e.g. verify module", false);
            channel.sendMessageEmbeds(embed.build()).queue();
            activityLog.sendActivityMsg("[MAIN] Help command has been activated",1, serverId);
        }
    }


}
