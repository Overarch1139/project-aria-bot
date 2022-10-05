package com.github.echo2124;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class ActivityLog {
    // 1 = info; 2=warn; 3=error;
    public void sendActivityMsg(String msg, int type, String guildID) {
        // guildID is null then send to all guilds
        if (guildID==null) {
        } else {
            TextChannel msgChannel = Main.constants.jda.getTextChannelById(Main.constants.config.get(guildID).getChannelActivityLogId());
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        String concat="```";
        String endingSymbol="";
        String tag="";
        Role role=null;
        switch (type) {
            case 1:
                concat+="yaml\n";
                tag="[INFO]";
                break;
            case 2:
                concat+="fix\n";
                tag="[WARN]";
                break;
            case 3:
                concat+="diff\n";
                concat+="- ";
                tag="[ERROR]";
                endingSymbol=" - ";
                role=Main.constants.jda.getRoleById(Main.constants.config.getRoleDevTeamId());
                break;
            default:
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss Z");
        concat+="["+now.format(format)+"]";
        concat+=tag;
        concat+=msg;
        concat+=endingSymbol;
        concat+="\n```";
        if (user!=null) {
            msgChannel.sendMessage(concat+"<@&"+Main.constants.config.getRoleDevTeamId()+">").queue();
        } else {
            msgChannel.sendMessage(concat).queue();
        }
    }
}
