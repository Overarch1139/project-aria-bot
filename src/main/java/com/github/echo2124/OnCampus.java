package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.echo2124.Main.constants.ONCAMPUS_ROLE_NAME;

public class OnCampus extends ListenerAdapter {
    public OnCampus() {
        initScheduler();
    }

    public void initScheduler() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        ZonedDateTime nextRun = now.withHour(8).withMinute(15).withSecond(0);
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initialDelay = duration.getSeconds();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String checkUnicode="U+2705";
                System.out.println("[OnCampus] Running task");
                Role oncampus=Main.constants.jda.getRolesByName(Main.constants.ONCAMPUS_ROLE_NAME, true).get(0);
                TextChannel msgChannel= Main.constants.jda.getTextChannelsByName(Main.constants.ONCAMPUS_CHANNEL_NAME, true).get(0);
                // recreating channel
                msgChannel.delete().queue();
                msgChannel.createCopy().queue(textChannel -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Who Is On Campus today?");
                    embed.setDescription("React to the existing reaction below to assign yourself to the OnCampus role");
                    embed.setAuthor("IT @ Monash");
                    embed.setColor(Color.CYAN);
                    embed.setFooter("NOTE: This post will be recreated everyday & role will be removed from everyone");
                    textChannel.sendMessage(embed.build()).queue(message -> {
                        message.addReaction(checkUnicode).queue();
                        // recreating role
                        oncampus.delete().queue();
                        oncampus.createCopy().queue(role -> {
                            System.out.println("[OnCampus] Creating copy of role");
                            role.getGuild().modifyRolePositions().selectPosition(role.getPosition()).moveTo(114).queue();
                            ListenerAdapter reactionListener = new ListenerAdapter() {
                                @Override
                                public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                                    System.out.println("[OnCampus] React Listener triggered");
                                    if (event.getMessageId().equals(message.getId()) && event.getReactionEmote().getName().equals("✅")) {
                                        System.out.println("[OnCampus] Added role to member");
                                        event.getGuild().addRoleToMember(event.getMember(),role).queue();
                                    }
                                    super.onMessageReactionAdd(event);
                                }
                            };
                            Main.constants.jda.addEventListener(reactionListener);
                        });
                        });
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
