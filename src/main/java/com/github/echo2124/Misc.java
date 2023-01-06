package com.github.echo2124;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class Misc {
    // run this on start up to remove timeout regardless (in case bot crashes whilst trying to verify someone)
  /*  public void resetTimeout() {
        Guild guild =  Main.constants.jda.getGuildById(Main.constants.config.getServerId());
        List<Member> members = guild.findMembers(member -> {
            return true;
        }).get();
        System.out.println("[MISC MODULE] Reset Timeout");
    }
*/
    public void sendServiceModeMsg(MessageChannel channel, String msg) {
        channel.sendMessage(msg).queue();
    }

    public void sendActiveModeMsg(MessageChannel channel, String msg) {
        channel.sendMessage(msg).queue();
    }

}
