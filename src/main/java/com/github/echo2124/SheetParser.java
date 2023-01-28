package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.github.echo2124.Main.constants.activityLog;

public class SheetParser {

    private String serverId;

    public SheetParser(Message.Attachment msgattached, String serverId) {
        InputStream stream;
        String fileName="";
        this.serverId=serverId;
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


    // test constructor
    public SheetParser() {
        InputStream stream=null;
        String fileName="";
        this.serverId=null;
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
            HashMap<Integer, ArrayList<String>> data;
            /* TODO:
             -- (1) determine each column and index them for next part
             -- (2) iterate through each row and grab needed information to cross reference
             */
            // -- (1)

            // -- (2)
            int i=0;
            for (Row row : sheet) {
                for (Cell cell : row) {
                    String columnName=getCellName(cell);
                    String[] parentColumns=Main.constants.config.get(serverId).getSheetParserParentColumns();
                    for (int a=0; a<parentColumns.length; a++) {
                        if (columnName.contains(parentColumns[a])) {

                        }
                    }
                }
                i++;
            }
        } catch (Exception e) {
            activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
        }
    }

    // https://stackoverflow.com/questions/8202319/get-columns-names-in-excel-file-using-apache-poi
    private static String getCellName(Cell cell)
    {
        return CellReference.convertNumToColString(cell.getColumnIndex()) + (cell.getRowIndex() + 1);
    }

}
