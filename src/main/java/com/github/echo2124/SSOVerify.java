package com.github.echo2124;

import com.github.scribejava.core.builder.ScopeBuilder;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.DeviceAuthorization;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.iwebpp.crypto.TweetNaclFast;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class SSOVerify {
    private static final String VERIFIED_ROLE_ID="909827233194070039";
    private static final String NETWORK_NAME = "Google";
    private static final String PROTECTED_RESOURCE_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private User user;
    private Guild guild;
    private MessageChannel msgChannel;
    private Database db;
    public SSOVerify(User user, Guild guild, MessageChannel channel, Database db) {
        this.user=user;
        this.guild=guild;
        this.msgChannel=channel;
        this.db=db;
        try {
            if (!checkVerification()) {
                verify();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean checkVerification() {
        boolean isVerified = false;
        if (db.getDBEntry("CERT", user.getId()).contains("true")) {
            isVerified=true;
        }
        return isVerified;
    }


    public void sendMsg(String msg) {
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessage(
                msg
        )).queue();
    }

    public void sendPublicMsg() {
        msgChannel.sendMessage(user.getAsMention()+" , Please check your DMs, you should receive the verification instructions there.").queue();
    }

    // TODO: Consider moving a lot of this text to a JSON object
    public void sendVerifiedNotification(String name) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Verified!");
        embed.setColor(Color.green);
        embed.setDescription("Hi " +name +",\n you have been successfully verified, you can now access channels that are exclusive for verified Monash University students only.");
        embed.setFooter("If you have any problems please contact Echo2124#3778 (creator of bot)");
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessage(embed.build())).queue();
    }

    public void sendFailureNotification() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Invalid Google Account");
        embed.setColor(Color.red);
        embed.setDescription("Please ensure that you are using a Monash Google Account, it should have an email that ends in @student.monash.edu.au . If the issues persist please contact Echo2124#3778 with a screenshot and description of the issue that you are experiencing..");
        this.user.openPrivateChannel().flatMap(channel -> channel.sendMessage(embed.build())).queue();
    }

    public void verify() throws IOException, InterruptedException, ExecutionException {
        // Replace these with your client id and secret
        final String clientId = "977901865384-ofgaulegobvdvu0um2i9l9n68cg6db66.apps.googleusercontent.com";
        final String clientSecret = "XUaViKEXt140sprNyUYkawF-";
        final OAuth20Service service = new ServiceBuilder(clientId)
                .debug()
                .apiSecret(clientSecret)
                .defaultScope(new ScopeBuilder("profile", "email")) // replace with desired scope
                .build(GoogleApi20.instance());
        System.out.println("Requesting a set of verification codes...");
        final DeviceAuthorization deviceAuthorization = service.getDeviceAuthorizationCodes();
        sendPublicMsg();
        sendMsg("Link: "+deviceAuthorization.getVerificationUri()+"\n Token: " +deviceAuthorization.getUserCode());
        if (deviceAuthorization.getVerificationUriComplete() != null) {
            System.out.println("Or visit " + deviceAuthorization.getVerificationUriComplete());
        }
        final OAuth2AccessToken accessToken = service.pollAccessTokenDeviceAuthorizationGrant(deviceAuthorization);
            final String requestUrl;
                requestUrl = PROTECTED_RESOURCE_URL;
            final OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
            service.signRequest(accessToken, request);
            try (Response response = service.execute(request)) {
                JSONObject parsedObj = new JSONObject(response.getBody());

                if (verifyEmail(parsedObj) == true) {
                   /// insert into db > add role > notify user
                    addVerifiedRole();
                    HashMap<String, String> parsedData= new HashMap<String, String>();
                    parsedData.put("discordID", user.getId());
                    parsedData.put("name", parsedObj.getString("given_name"));
                    parsedData.put("emailAddr", parsedObj.getString("email"));
                    parsedData.put("isVerified", "true");
                    db.modifyDB("CERT", "add", parsedData);
                    sendVerifiedNotification(parsedObj.getString("given_name"));
                } else {
                    sendFailureNotification();
                }
                sendMsg(response.getBody());

            }
        }

        public Boolean verifyEmail(JSONObject obj) {
            boolean isValid = false;
            if (obj.has("hd")) {
                if (obj.getString("hd").equals("student.monash.edu")) {
                    isValid = true;
                }
            }
            return isValid;
        }

        // TODO Might be worth switching to ID instead encase someone changes the name of the role
        public void addVerifiedRole() {
        try {
            guild.addRoleToMember(user.getIdLong(), guild.getRoleById(VERIFIED_ROLE_ID)).queue();
            System.out.println("[VERBOSE] Added role");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("[ERROR] Probably a permission issue");
        }
        }

    }