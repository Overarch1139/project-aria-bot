package com.github.echo2124;
import com.google.gson.Gson;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

// This class is responsible for grabbing static variables from JSON file.

public class ConfigParser {
    public Config parseDefaults() {
        // load in file based on env var, e.g. if env var config_file="production.json" then load that one in
        // This will be loaded in before JDA therefore activitylog cannot be used instead use system logs
        Config config=null;
        try {
            Gson parser = new Gson();
            config =parser.fromJson(new BufferedReader(new FileReader("src/main/java/com/github/echo2124/"+System.getenv("CONFIG_FILE"))),Config.class);
            System.out.println(config);

        } catch (FileNotFoundException e ) {
            System.out.println("[CONFIG][ERROR] Unable to fetch config: "+e.getMessage());
            System.out.println("[FATAL_ERROR] Cannot start program without access to config");
            System.exit(0);
        }
        return config;
    }
}