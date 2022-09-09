package com.github.echo2124;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

// This class is responsible for grabbing static variables from JSON file.
public class ConfigParser {
    public Config[] parseDefaults() {
        // This will be loaded in before JDA therefore activitylog cannot be used instead use system logs
        Config[] configs=null;
        try {
            Gson parser = new Gson();
            // can grab multiple configs now
            String target=System.getenv("CONFIG_FILE");
            String[] parsedConfigs=target.split(",");
            configs = new Config[parsedConfigs.length];
            for (int i=0; parsedConfigs.length<i; i++) {
                configs[i]=parser.fromJson(new BufferedReader(new FileReader("src/main/java/com/github/echo2124/"+parsedConfigs[i])),Config.class);
            }
        } catch (FileNotFoundException e ) {
            System.out.println("[CONFIG][ERROR] Unable to fetch config: "+e.getMessage());
            System.out.println("[FATAL_ERROR] Cannot start program without access to config");
            System.exit(0);
        }
        return configs;
    }
}