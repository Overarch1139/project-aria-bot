package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class SheetParser {

    public SheetParser(Message.Attachment msgattached) {
        // todo implement async or hold thread until file downloaded
        File file =null;
       CompletableFuture<File> futureFile= msgattached.getProxy().downloadToFile();
       futureFile.wait();

    }

    public String parser() {
        String output="";

        return output;
    }

}
