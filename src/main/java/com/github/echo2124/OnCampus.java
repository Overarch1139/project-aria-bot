package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.github.echo2124.Main.constants.ARIA_CHANNEL_CATEGORY_ID;
import static com.github.echo2124.Main.constants.ONCAMPUS_ROLE_NAME;

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
                String checkUnicode="U+2705";
                System.out.println("[OnCampus] Running task");
                Role oncampus;
                try {
                     oncampus=Main.constants.jda.getRolesByName(Main.constants.ONCAMPUS_ROLE_NAME, true).get(0);
                } catch (Exception e) {
                    RoleAction role =Main.constants.jda.getGuilds().get(0).createRole();
                    role.setName(ONCAMPUS_ROLE_NAME);
                    role.setColor(Color.cyan);
                    role.setHoisted(true);
                    role.queue();
                    oncampus=Main.constants.jda.getRolesByName(Main.constants.ONCAMPUS_ROLE_NAME, true).get(0);
                }
                Role finalOnCampus=oncampus;
                TextChannel msgChannel;
                try {
                     msgChannel = Main.constants.jda.getTextChannelsByName(Main.constants.ONCAMPUS_CHANNEL_NAME, true).get(0);
                } catch (Exception e) {
                    // might need to explicitly state perms here, I guess we will find out
                    ChannelAction newOnCampusChannel=Main.constants.jda.getGuilds().get(0).createTextChannel(Main.constants.ONCAMPUS_CHANNEL_NAME, Main.constants.jda.getGuilds().get(0).getCategoryById(ARIA_CHANNEL_CATEGORY_ID));
                    msgChannel=Main.constants.jda.getTextChannelsByName(Main.constants.ONCAMPUS_CHANNEL_NAME, true).get(0);

                }
                // recreating channel
                msgChannel.delete().queue();
                msgChannel.createCopy().setPosition(msgChannel.getPosition()).queue(textChannel -> {

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Who Is On Campus today?");
                    embed.setDescription("React to the existing reaction below to assign yourself to the OnCampus role");
                    embed.setAuthor("IT @ Monash");
                    embed.setColor(Color.CYAN);
                    embed.setFooter("NOTE: This post will be recreated everyday & role will be removed from everyone");
                    textChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                        message.addReaction(checkUnicode).queue();
                        // recreating role
                        for (Role role: Main.constants.jda.getRolesByName(Main.constants.ONCAMPUS_ROLE_NAME, true)) {
                            role.delete().queue();
                        }
                        finalOnCampus.createCopy().queue(role -> {
                            System.out.println("[OnCampus] Creating copy of role");
                            role.getGuild().modifyRolePositions().selectPosition(role.getPosition()).moveTo(114).queue();
                            ListenerAdapter reactionListener = new ListenerAdapter() {
                                @Override
                                public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                                    System.out.println("[OnCampus] React Listener triggered");
                                    if (event.getMessageId().equals(message.getId()) && event.getReactionEmote().getName().equals("✅") && !event.getMember().getUser().isBot()) {
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
        if (state) {
            task.run();
        }
    }



}
