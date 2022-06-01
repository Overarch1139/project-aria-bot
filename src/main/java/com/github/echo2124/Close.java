package com.github.echo2124;

import static com.github.echo2124.Main.constants.activityLog;

public class Close extends Thread {

    @Override
    public void run() {
        activityLog.sendActivityMsg("[MAIN] Aria bot is restarting...",1);
        System.out.println("Performing Shutdown Sequence");
    }
}
