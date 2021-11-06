package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;


// Will require some sort of DB implementation
public class Warn {

    public Warn(String action,Message msg) {
        switch (action) {
            case "fetchWarnings":
                break;
            case "addWarning":
                break;
            case "rmWarning":
                break;
            default:
                System.out.println("["+this.getClass().getName()+" "+Main.constants.logPrefixes[0]+"] Invalid Warn action");
        }
    }

    public Boolean fetchWarnings() {
        boolean state=false;

        return state;
    }
}
