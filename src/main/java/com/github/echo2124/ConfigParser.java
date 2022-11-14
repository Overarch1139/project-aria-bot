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
            String target=System.getenv("CONFIG_FILE");
            ArrayList<String> parsedConfigs=new ArrayList<String>();
            if (target.contains(",")) {
                 parsedConfigs=(ArrayList<String>) Arrays.asList(target.split(","));
                System.out.println("[DEBUG] Target config"+parsedConfigs.get(0));
                System.out.println("[DEBUG] Target config"+parsedConfigs.get(1));

            } else {
                parsedConfigs.add(target);
                System.out.println("[DEBUG] Target config"+parsedConfigs.get(0));
                Gson parser = new Gson();
                Config config=parser.fromJson(new BufferedReader(new FileReader("src/main/java/com/github/echo2124/"+parsedConfigs.get(0))),Config.class);
                System.out.println("[DEBUG] Config Name: "+config.getConfigName());
            }
            System.out.println("Attempting config load...");
            // likely the culprit, tbd
            for (int i=0; i<parsedConfigs.size(); i++) {
                Gson parser = new Gson();
                Config config=parser.fromJson(new BufferedReader(new FileReader("src/main/java/com/github/echo2124/"+parsedConfigs.get(i))),Config.class);
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