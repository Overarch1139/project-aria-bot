package com.github.echo2124;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OnCampus {
    public OnCampus() {
        initScheduler();
    }

    public void initScheduler() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        ZonedDateTime nextRun = now.withHour(2).withMinute(48).withSecond(0);
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initialDelay = duration.getSeconds();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String checkUnicode="U+2705";
                System.out.println("Running task");
               // remove previous msgs & remove role from everyone
                Role oncampus=Main.constants.jda.getRolesByName(Main.constants.ONCAMPUS_ROLE_NAME, true).get(0);
                // recreating role
                oncampus.createCopy().queue();
                oncampus.delete().queue();
                // re-ref role
                TextChannel msgChannel= Main.constants.jda.getTextChannelsByName(Main.constants.ONCAMPUS_CHANNEL_NAME, true).get(0);
                // recreating channel
                msgChannel.createCopy().queue();
                msgChannel.delete().queue();
                // re-ref this as the channel it is pointing to no longer exists
                msgChannel=Main.constants.jda.getTextChannelsByName(Main.constants.ONCAMPUS_CHANNEL_NAME, true).get(0);
                // generate msg
                // consider adding date to this msg
                System.out.println("new msgChannelID: "+msgChannel.getId());
                msgChannel.sendMessage("React below to the following emoji listed if you are heading to campus today").queue(message -> {
                    message.addReaction(checkUnicode);
                    /*
                    ListenerAdapter s = new ListenerAdapter() {
                        @Override
                        public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                            if (event.getMessageId().equals(message.getId()) && event.getReactionEmote().getName().equals("white_check_mark")) {
                                event.getGuild().addRoleToMember(event.getMember(),oncampus);
                            }
                            super.onMessageReactionAdd(event);
                        }
                    };*/
                });

            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task,
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }



}
