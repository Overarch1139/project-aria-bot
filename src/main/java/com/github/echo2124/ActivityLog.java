package com.github.echo2124;

import net.dv8tion.jda.api.entities.TextChannel;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ActivityLog {
    TextChannel activityCh;
    public ActivityLog() {
        activityCh=Main.constants.jda.getTextChannelById(Main.constants.ACTIVITY_LOG_ID);
    }


    // 1 = info; 2=warn; 3=error;
    public void sendActivityMsg(String msg, int type) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        String concat="```";
        String endingSymbol="";
        switch (type) {
            case 1:
                concat+="yaml\n";
                concat+="[INFO]";
                break;
            case 2:
                concat+="fix\n";
                concat+="[WARN]";
                break;
            case 3:
                concat+="diff\n";
                concat+="- ";
                concat+="[ERROR]";
                endingSymbol=" - ";
                break;
            default:
        }
        concat+="["+now.toLocalTime().toString()+"]";
        concat+=msg;
        concat+=endingSymbol;
        concat+="\n```";
        activityCh.sendMessage(concat).queue();
    }


}
