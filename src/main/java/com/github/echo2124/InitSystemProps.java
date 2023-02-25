package com.github.echo2124;

import java.sql.SQLOutput;

public class InitSystemProps {


    public InitSystemProps() {
        // Local properties
        System.setProperty("IS_DEV", "false");
     /*   System.setProperty("EMULATED_GUILD", "878943527608938508");
        System.setProperty("ENABLED_MODULES", "sheetParser");
        System.setProperty("TEST_ENV_PATH", "/Users/Joshua/Documents/aria/project-aria-bot/src/main/java/com/github/echo2124/");
        */
        // inherited environment vars (doing this way as to not expose API keys)

        if (!System.getProperty("IS_DEV").contains("true")) {
            System.setProperty("CONFIG_FILE", System.getenv("CONFIG_FILE"));
        } else {
            // MANUAL SET SELECTED CONFIG IN TESTING ENVIRONMENT
            System.setProperty("CONFIG_FILE", "monashIT_testing.json");
        }
        try {
            System.setProperty("DISCORD_CLIENT_SECRET", System.getenv("DISCORD_CLIENT_SECRET"));
            System.setProperty("DATABASE_URL", System.getenv("DATABASE_URL"));
            System.setProperty("GOOGLE_SSO_CLIENT_ID", System.getenv("GOOGLE_SSO_CLIENT_ID"));
            System.setProperty("GOOGLE_SSO_CLIENT_SECRET", System.getenv("GOOGLE_SSO_CLIENT_SECRET"));
            System.setProperty("PRODUCTION_ENV", System.getenv("PRODUCTION_ENV"));
            // Soon to be deprecated from Aria
            System.setProperty("TWITTER_ACCESS_SECRET", System.getenv("TWITTER_ACCESS_SECRET"));
            System.setProperty("TWITTER_ACCESS_TOKEN", System.getenv("TWITTER_ACCESS_TOKEN"));
            System.setProperty("TWITTER_CONSUMER_KEY", System.getenv("TWITTER_CONSUMER_KEY"));
            System.setProperty("TWITTER_CONSUMER_SECRET", System.getenv("TWITTER_CONSUMER_SECRET"));
        } catch (Exception e) {
            System.out.println("System props unable to be read, probably not set");
        }
    }

}