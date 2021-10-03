package com.github.echo2124;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Preconditions;
import net.dv8tion.jda.api.entities.Message;
import java.io.IOException;

public class SSOVerify {
    public SSOVerify() {

    }

    public String initRedirectReceiver() {
        String redirectUri="";
        VerificationCodeReceiver receiver = new LocalServerReceiver();
        receiver = Preconditions.checkNotNull(receiver);
        try {
            redirectUri = receiver.getRedirectUri();
        } catch (Exception e) {
            System.out.println("[SSOVerify] ERROR: Retrieving Redirect URI failed");
        }
        return redirectUri;
    }



}
