package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.echo2124.Main.constants.*;


public class OnCampus extends ListenerAdapter {
    public final String checkUnicode = "U+2705";
    public OnCampus(Boolean state) {
           initScheduler(state);
           restoreListener();
    }

    public void initScheduler(Boolean state) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        ZonedDateTime generateNextRun = now.withHour(6).withMinute(30).withSecond(0);
        ZonedDateTime resetNextRun = now.withHour(0).withMinute(0).withSecond(0);
        if(now.compareTo(generateNextRun) > 0)
            generateNextRun = generateNextRun.plusDays(1);
        Duration generateDuration = Duration.between(now, generateNextRun);
        if(now.compareTo(resetNextRun) > 0)
            resetNextRun = resetNextRun.plusDays(1);
        Duration resetDuration = Duration.between(now, resetNextRun);

        long generateInitialDelay = generateDuration.getSeconds();
        long resetInitialDelay = resetDuration.getSeconds();
        Runnable generateHandler = new Runnable() {
            @Override
            public void run() {
                activityLog.sendActivityMsg("[ONCAMPUS] Running generate task",1);
                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                Guild guild = Main.constants.jda.getGuilds().get(0);
                // test
                System.out.println("[OnCampus] Running task");
                Role oncampus = Main.constants.jda.getRoleById(ONCAMPUS_ROLE_ID);
                TextChannel msgChannel = Main.constants.jda.getTextChannelById(ONCAMPUS_CHANNEL_ID);
                resetEntities(oncampus, msgChannel, guild);
                if (day!=Calendar.SUNDAY && day!=Calendar.SATURDAY || state) {
                    generateMsg(oncampus,msgChannel);
                }

            }
        };
        Runnable resetHandler = new Runnable() {
            @Override
            public void run() {
                activityLog.sendActivityMsg("[ONCAMPUS] Running reset task",1);
                Role oncampus = Main.constants.jda.getRoleById(ONCAMPUS_ROLE_ID);
                TextChannel msgChannel = Main.constants.jda.getTextChannelById(ONCAMPUS_CHANNEL_ID);
                Guild guild = Main.constants.jda.getGuilds().get(0);
                resetEntities(oncampus,msgChannel,guild);
            }
        };
        ScheduledExecutorService generateScheduler = Executors.newScheduledThreadPool(1);
        generateScheduler.scheduleAtFixedRate(generateHandler,
                generateInitialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);

        ScheduledExecutorService resetScheduler = Executors.newScheduledThreadPool(1);
        resetScheduler.scheduleAtFixedRate(resetHandler,
                resetInitialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);

        if (state) {
            generateHandler.run();
        }
    }
    public void resetEntities(Role oncampus, TextChannel msgChannel, Guild guild) {
        guild.loadMembers();
        MessageHistory msgHistory = msgChannel.getHistory();
        try {
            msgHistory.retrievePast(1).queue(messages -> {
                messages.get(0).delete().queue();
            });
        } catch (Exception e) {
            activityLog.sendActivityMsg("[ONCAMPUS] Unable to fetch last message",2);
            System.out.println("[OnCampus] Unable to grab last message");
        }
        try {
            Collection<Member> members = guild.getMembersWithRoles(oncampus);
            for (Member member : members) {
                guild.removeRoleFromMember(member, oncampus).queue();
            }
        } catch (Exception e) {
            System.out.println("[OnCampus] Unable to remove role from users");
            activityLog.sendActivityMsg("[ONCAMPUS] No users to remove role from",2);
        }
        activityLog.sendActivityMsg("[ONCAMPUS] Removed old On Campus message & removed all users from role",1);
    }

    public void generateMsg(Role oncampus, TextChannel msgChannel) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Who Is On Campus today?");
        embed.setDescription("React to the existing reaction below to assign yourself to the OnCampus role");
        embed.setAuthor("IT @ Monash");
        embed.setColor(Color.CYAN);
        embed.setFooter("NOTE: This post will be recreated everyday & role will be removed from everyone");
        msgChannel.sendMessageEmbeds(embed.build()).queue(message -> {
            message.addReaction(checkUnicode).queue();
            ListenerAdapter reactionListener = new ListenerAdapter() {
                @Override
                public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                    if (event.getMessageId().equals(message.getId()) && event.getReactionEmote().getName().equals("✅") && !event.getMember().getUser().isBot()) {
                        activityLog.sendActivityMsg("[ONCAMPUS] React Listener triggered",1);
                        System.out.println("[OnCampus] Added role to member");
                        activityLog.sendActivityMsg("[ONCAMPUS] Giving On Campus role to user",1);
                        event.getGuild().addRoleToMember(event.getMember(), oncampus).queue();
                    }
                    super.onMessageReactionAdd(event);
                }
            };
            Main.constants.jda.addEventListener(reactionListener);
        });
        activityLog.sendActivityMsg("[ONCAMPUS] Generated OnCampus Message",1);
    }


    public void restoreListener() {
        Role oncampus = Main.constants.jda.getRoleById(ONCAMPUS_ROLE_ID);
        TextChannel msgChannel = Main.constants.jda.getTextChannelById(ONCAMPUS_CHANNEL_ID);
        MessageHistory msgHistory = msgChannel.getHistory();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        activityLog.sendActivityMsg("[ONCAMPUS] Attempting to restore listener...",1);
        try {
            msgHistory.retrievePast(1).queue(messages -> {
                // checks if last oncampus message was made same day if so then try to reattach the listener
                if (messages.get(0).getTimeCreated().getDayOfWeek()==now.getDayOfWeek()) {
                    try {
                        ListenerAdapter reactionListener = new ListenerAdapter() {
                            @Override
                            public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                                if (event.getMessageId().equals(messages.get(0).getId()) && event.getReactionEmote().getName().equals("✅") && !event.getMember().getUser().isBot()) {
                                    activityLog.sendActivityMsg("[ONCAMPUS] React Listener triggered", 1);
                                    System.out.println("[OnCampus] Added role to member");
                                    activityLog.sendActivityMsg("[ONCAMPUS] Giving On Campus role to user", 1);
                                    event.getGuild().addRoleToMember(event.getMember(), oncampus).queue();
                                }
                            }
                        };
                        Main.constants.jda.addEventListener(reactionListener);
                        activityLog.sendActivityMsg("[ONCAMPUS] Restore successful, attached listener!",2);
                    } catch (Exception e) {
                        activityLog.sendActivityMsg("[ONCAMPUS] Unable to restore: cannot attach listener",2);
                    }
                }
            });
        } catch (Exception e) {
            activityLog.sendActivityMsg("[ONCAMPUS] Unable to restore: cannot fetch last message",1);
            System.out.println("[OnCampus] Unable to grab last message");
        }
    }
}