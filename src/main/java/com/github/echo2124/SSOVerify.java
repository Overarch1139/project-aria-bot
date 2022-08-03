package com.github.echo2124;

import com.github.scribejava.core.builder.ScopeBuilder;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth2.OAuth2Error;
import com.iwebpp.crypto.TweetNaclFast;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.github.echo2124.Main.constants.activityLog;

public class SSOVerify extends Thread {
    private static final String NETWORK_NAME = "Google";
    private static final String PROTECTED_RESOURCE_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final int MAX_NAME_LEN = 2048;
    private User user;
    private Guild guild;
    private MessageChannel msgChannel;
    private Database db;
    private OAuth20Service service = null;
    private DeviceAuthorization deviceAuthorization = null;
    private long intervalMillis=5000;

    public SSOVerify(User user, Guild guild, MessageChannel channel, Database db) {
        this.user = user;
        this.guild = guild;
        this.msgChannel = channel;
        this.db = db;
    }

    public void run() {
        System.out.println("[CERT MODULE] Thread #" + Thread.currentThread().getId() + " is active!");
        activityLog.sendActivityMsg("[VERIFY] Thread #"+Thread.currentThread().getId() + " is active!",1);

        try {
            if (!Main.constants.serviceMode) {
                if (!checkVerification()) {
                    verify();
                } else {
                    sendPublicMsg();
                    sendMsg(user.getAsMention() + ", have already been verified! Aria.");
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void timeout() {
        TimerTask task = new TimerTask() {
            public void run() {
                if (!checkVerification()) {
                    sendFailureNotification("timeout");
                }
                try {
                    System.out.println("Put polling thread #" + Thread.currentThread().getId()+" into inactive state");
                    activityLog.sendActivityMsg("[VERIFY]" +"Put polling thread #" + Thread.currentThread().getId()+" into inactive state",1);
                    intervalMillis=1800*1000;
                    System.out.println("Attempt to close #" + Thread.currentThread().getId() + "'s oauth service");
                    service.close();
                    activityLog.sendActivityMsg("[VERIFY]" +"Attempt to close #" + Thread.currentThread().getId() + "'s oauth service",1);

                } catch (Exception e) {
                    System.out.println("Unable to close thread #" + Thread.currentThread().getId() + "'s oauth service");
                    activityLog.sendActivityMsg("[VERIFY]"+"Unable to close thread #" + Thread.currentThread().getId() + "'s oauth service",3);
                }
                System.out.println("[CERT MODULE] Thread #" + Thread.currentThread().getId() + " has stopped!");
                Thread.currentThread().interrupt();
                activityLog.sendActivityMsg("[VERIFY]"+"[CERT MODULE] Thread #" + Thread.currentThread().getId() + " has stopped!",1);
            }
        };
        Timer timer = new Timer("Timer");
        // Equiv to 5mins and 10 secs.
        long delay = 306000L;
        timer.schedule(task, delay);
    }

    public boolean checkVerification() {
        boolean isVerified = false;
        if (db.getDBEntry("CERT", user.getId()).contains("true")) {
            activityLog.sendActivityMsg("[VERIFY] User has already been verified!",1);
            isVerified = true;
        }
        return isVerified;
    }


    public void sendMsg(String msg) {
        activityLog.sendActivityMsg("[VERIFY] Send private msg to user to indicate verification state",1);
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessage(
                msg
        )).queue();
    }

    public void sendPublicMsg() {
        msgChannel.sendMessage(user.getAsMention() + " , Please check your DMs, you should receive the verification instructions there.").queue();
    }

    // TODO: Consider moving a lot of this text to a JSON object
    public void sendVerifiedNotification(String name) {
        activityLog.sendActivityMsg("[VERIFY] Send verified notification via DMs",1);
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Verified!");
        embed.setColor(Color.green);
        embed.setDescription("Hi " + name + ",\n you have been successfully verified, you can now access channels that are exclusive for verified Monash University students only. \n Thanks for verifying, Aria");
        embed.setFooter("If you have any problems please contact Echo2124#3778 (creator of Aria)");
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(embed.build())).queue();
    }

    public void sendFailureNotification(String type) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.red);
        switch (type) {
            case "invalid_account":
                embed.setTitle("Invalid Google Account");
                embed.setDescription("Aria was unable to verify you. Please ensure that you are using a Monash Google Account, it should have an email that ends in @student.monash.edu.au . If the issues persist please contact Echo2124#3778 with a screenshot and description of the issue that you are experiencing. \n Best Regards, Aria. ");
                activityLog.sendActivityMsg("[VERIFY] REASON: Unable to verify user due to invalid google account",1);
                break;
            case "invalid_name":
                embed.setTitle("Invalid First Name");
                embed.setDescription("Your profile name too large, therefore verification has failed. You can change your first name in the Google Account settings. Please ensure that your account firstname is under 2048 characters.");
                activityLog.sendActivityMsg("[VERIFY] REASON: Unable to verify user due to invalid profile name",1);
                break;
            case "timeout":
                embed.setTitle("Verification timeout");
                embed.setDescription("Aria has noticed that the provided token was not used within the allocated timeframe. This is likely because you might of not followed the aforementioned steps. Please try to generate a new token by typing >verify at the specified verification channel on the IT @ Monash server.");
                activityLog.sendActivityMsg("[VERIFY] REASON: User did not verify in time",1);
                break;
        }
        activityLog.sendActivityMsg("[VERIFY] Send failure notification via DMs",1);
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(embed.build())).queue();
    }

    public void sendAuthRequest(String link, String code) {
        EmbedBuilder authEmbed = new EmbedBuilder();
        EmbedBuilder faqEmbed = new EmbedBuilder();
        faqEmbed.setColor(Color.BLUE);
        faqEmbed.setTitle("Frequently Asked Questions (FAQs)");
        faqEmbed.addField("What does this do?", "This OAuth request will ask access for two main scopes (Email & Profile).", false);
        faqEmbed.addField("What information will this Aria store?", "Aria will store the following information: Email Address, First Name, DiscordID, Time of Verification and Verification Status.", false);
        faqEmbed.addField("Why do we need this data?", "In order to verify whether you are a Monash student we need to check the Email Domain in order to see if it would match a student's Monash email domain. If it does, then you are likely a student. We store your first name, as Aria will be able to refer to you in a more personalised manner. This name will only be used when Aria sends you a private message", false);
        authEmbed.setColor(Color.YELLOW);
        authEmbed.setTitle("Authorisation Request");
        authEmbed.setDescription("Steps to verify yourself:\n **1)**  Open provided link in your browser. \n **2)** Paste provided code into input. \n **3)** Select your Monash Google Account. \n **4)** Done!");
        authEmbed.addField("Link: ", link, false);
        authEmbed.addField("Code: ", code, false);
        authEmbed.setFooter("This access token will expire in **5 Mins!**");
        activityLog.sendActivityMsg("[VERIFY] Send FAQ & Auth request message via DMs",1);
        try {
            this.user.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(faqEmbed.build())).queue();
            this.user.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(authEmbed.build())).queue();
        } catch (ErrorResponseException e) {
            if (e.getErrorCode()==50007) {
                sendIssuePrompt();
            }
        }
    }

    // Explains to the user why they aren't receiving a DM
    public void sendIssuePrompt() {
        activityLog.sendActivityMsg("[VERIFY] Sending DM Issue Prompt....", 2);
        EmbedBuilder issueEmbed = new EmbedBuilder();
        issueEmbed.setColor(Color.RED);
        issueEmbed.setTitle("\uD83D\uDEA8 Unable To Send DM! \uD83D\uDEA8");
        issueEmbed.addField("How to fix the problem: ", "Go to IT @ Monash > Privacy Settings > Enable Allow direct messages from server members. (NOTE: After you are verified you can disable DMs again). If you need a visual guide then click the visual demonstration button.", false);
        issueEmbed.setFooter("Contact Echo2124#3778 for help with the bot");
        ArrayList<net.dv8tion.jda.api.interactions.components.buttons.Button> btns = new ArrayList<net.dv8tion.jda.api.interactions.components.buttons.Button>();
        btns.add(Button.link("https://cdn.discordapp.com/attachments/802537874590269460/996675231940874270/SPOILER_re-enable-dms2.gif", "Visual Demonstration").withEmoji(Emoji.fromUnicode("U+2139")));
        msgChannel.sendMessageEmbeds(issueEmbed.build()).setActionRow(btns).queue();
    }

    public void verify() throws IOException, InterruptedException, ExecutionException {
        final String clientId = System.getenv("GOOGLE_SSO_CLIENT_ID");
        final String clientSecret = System.getenv("GOOGLE_SSO_CLIENT_SECRET");
        service = new ServiceBuilder(clientId)
                .debug()
                .apiSecret(clientSecret)
                .defaultScope(new ScopeBuilder("profile", "email")) // replace with desired scope
                .build(GoogleApi20.instance());
        System.out.println("Requesting a set of verification codes...");
        deviceAuthorization = service.getDeviceAuthorizationCodes();
        sendPublicMsg();
        timeout();
        sendAuthRequest(deviceAuthorization.getVerificationUri(), deviceAuthorization.getUserCode());
        if (deviceAuthorization.getVerificationUriComplete() != null) {
            System.out.println("Or visit " + deviceAuthorization.getVerificationUriComplete());
        }
        final OAuth2AccessToken accessToken = pollAccessToken(deviceAuthorization);

        final String requestUrl;
        requestUrl = PROTECTED_RESOURCE_URL;
        final OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
        service.signRequest(accessToken, request);
        try (Response response = service.execute(request)) {
            JSONObject parsedObj = new JSONObject(response.getBody());

            if (verifyEmail(parsedObj) == true) {
                /// insert into db > add role > notify user
                if (parsedObj.getString("given_name").length() <= MAX_NAME_LEN) {
                    addVerifiedRole();
                    HashMap<String, String> parsedData = new HashMap<String, String>();
                    parsedData.put("discordID", user.getId());
                    parsedData.put("name", parsedObj.getString("given_name"));
                    parsedData.put("emailAddr", parsedObj.getString("email"));
                    parsedData.put("isVerified", "true");
                    db.modifyDB("CERT", "add", parsedData);
                    sendVerifiedNotification(parsedObj.getString("given_name"));
                } else {
                    sendFailureNotification("invalid_name");
                }
            } else {
                sendFailureNotification("invalid_account");
            }
        }
    }

    public Boolean verifyEmail(JSONObject obj) {
        boolean isValid = false;
        if (obj.has("hd")) {
            if (obj.getString("hd").equals("student.monash.edu") || obj.getString("hd").equals("monash.edu")) {
                isValid = true;
                activityLog.sendActivityMsg("[VERIFY] Email matches a Monash University domain",1);
            }
        }
        return isValid;
    }

    public void addVerifiedRole() {
        try {
            guild.addRoleToMember(UserSnowflake.fromId(user.getIdLong()), guild.getRoleById(Main.constants.config.getRoleVerifiedId())).queue();
            activityLog.sendActivityMsg("[VERIFY] Gave user ("+user.getAsTag()+") verified role",1);
            System.out.println("[VERBOSE] Added role");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("[ERROR] Probably a permission issue");
        }
    }

    // Custom implementation of token polling
    public OAuth2AccessToken pollAccessToken(DeviceAuthorization deviceAuthorization)
            throws InterruptedException, ExecutionException, IOException {
        while (true) {
            try {
                return service.getAccessTokenDeviceAuthorizationGrant(deviceAuthorization);
            } catch (OAuth2AccessTokenErrorResponse e) {
                if (e.getError() != OAuth2Error.AUTHORIZATION_PENDING) {
                    if (e.getError() == OAuth2Error.SLOW_DOWN) {
                        intervalMillis += 5000;
                    } else {
                        throw e;
                    }
                }
            }
            Thread.sleep(intervalMillis);
        }
    }
}
