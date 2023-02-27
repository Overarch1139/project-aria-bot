package com.github.echo2124;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

// This class is responsible for grabbing static variables from JSON file.
public class ConfigParser {
    public LinkedHashMap<String, Config> parseDefaults() {
        // This will be loaded in before JDA therefore activitylog cannot be used instead use system logs
        LinkedHashMap<String, Config> configs= new LinkedHashMap<String, Config>();
        try {
            // can grab multiple configs now
            String target=System.getProperty("CONFIG_FILE");
            String[] parsedConfigs=target.split(",");
            System.out.println("Attempting config load...");

            for (int i=0; i< parsedConfigs.length; i++) {
                Gson parser = new Gson();
                Config config = null;
                if (!System.getProperty("IS_DEV").contains("true")) {
                  config = parser.fromJson(new BufferedReader(new FileReader("/home/ubuntu/aria/project-aria-bot/src/main/java/com/github/echo2124/" + parsedConfigs[i])), Config.class);
                } else {
                    config = parser.fromJson(new BufferedReader(new FileReader(System.getProperty("TEST_ENV_PATH")+ parsedConfigs[i])), Config.class);
                }
                System.out.println("Config Detected: "+config.getConfigName());
                configs.put(config.getServerId(),config);
            }
        } catch (FileNotFoundException e ) {
            System.out.println("[CONFIG][ERROR] Unable to fetch config: "+e.getMessage());
            System.out.println("[FATAL_ERROR] Cannot start program without access to config");
            System.exit(0);
        }
        return configs;
    }
}