package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import static com.github.echo2124.Main.constants.activityLog;

public class SheetParser {

    public SheetParser(Message.Attachment msgattached) {
        InputStream stream;
        String fileName="";
        CompletableFuture<InputStream> futureStream = new CompletableFuture<InputStream>();
        // using input stream instead of file due to given file not being stored & current file impl being changed in DiscordJDA
        try {
            fileName=msgattached.getFileName();
            msgattached.getProxy().download();
            parser(futureStream.get());
        } catch (Exception e) {
            activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);

        }
    }

    public void parser(InputStream msgattached) {
        try {
            Workbook workbook = new XSSFWorkbook(msgattached);
            Sheet sheet = workbook.getSheetAt(0);
            /* TODO:
             -- (1) determine each column and index them for next part
             -- (2) iterate through each row and grab needed information to cross reference
             */
            // -- (1)


            // -- (2)
            for (Row row : sheet) {
                for (Cell cell : row) {

                }
            }
        } catch (Exception e) {
            activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
        }
    }
}
