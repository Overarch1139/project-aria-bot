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
    public LinkedHashMap<String, BotConfig> parseDefaults() {
        // This will be loaded in before JDA therefore activitylog cannot be used instead use system logs
        LinkedHashMap<String, BotConfig> configs= new LinkedHashMap<String, BotConfig>();
        try {
            // can grab multiple configs now
            String target=System.getenv("CONFIG_FILE");
            String[] parsedConfigs=target.split(",");
            System.out.println("Attempting config load...");
            for (int i=0; i< parsedConfigs.length; i++) {
                Gson parser = new Gson();
                BotConfig config=parser.fromJson(new BufferedReader(new FileReader("src/main/java/com/github/echo2124/"+parsedConfigs[i])),BotConfig.class);
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