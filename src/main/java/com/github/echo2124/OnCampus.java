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

import static com.github.echo2124.Main.constants.ONCAMPUS_CHANNEL_ID;
import static com.github.echo2124.Main.constants.ONCAMPUS_ROLE_ID;


public class OnCampus extends ListenerAdapter {
    public OnCampus(Boolean state) {
           initScheduler(state);
    }

    public void initScheduler(Boolean state) {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Australia/Melbourne"));
        ZonedDateTime nextRun = now.withHour(5).withMinute(0).withSecond(0);
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initialDelay = duration.getSeconds();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                Guild guild = Main.constants.jda.getGuilds().get(0);
                String checkUnicode = "U+2705";
                System.out.println("[OnCampus] Running task");
                Role oncampus = Main.constants.jda.getRoleById(ONCAMPUS_ROLE_ID);
                TextChannel msgChannel = Main.constants.jda.getTextChannelById(ONCAMPUS_CHANNEL_ID);
                // TODO: remove *all* messages from channel & remove *all* users from role before creating msg
                MessageHistory msgHistory = msgChannel.getHistory();
                try {
                    msgHistory.retrievePast(1).queue(messages -> {
                        messages.get(0).delete().queue();
                    });
                } catch (Exception e) {
                    System.out.println("[OnCampus] Unable to grab last message");
                }
                Collection<Member> members = guild.getMembersWithRoles(oncampus);
                for (Member member : members) {
                    guild.removeRoleFromMember(member, oncampus).queue();
                }

                if (day!=Calendar.SUNDAY && day!=Calendar.SATURDAY || state) {
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
                                System.out.println("[OnCampus] React Listener triggered");
                                if (event.getMessageId().equals(message.getId()) && event.getReactionEmote().getName().equals("✅") && !event.getMember().getUser().isBot()) {
                                    System.out.println("[OnCampus] Added role to member");
                                    event.getGuild().addRoleToMember(event.getMember(), oncampus).queue();
                                }
                                super.onMessageReactionAdd(event);
                            }
                        };
                        Main.constants.jda.addEventListener(reactionListener);
                    });
                } else {

                }
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task,
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
        if (state) {
            task.run();
        }
    }


}