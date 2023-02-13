package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

import static com.github.echo2124.Main.constants.activityLog;

public class SheetParser {

    public SheetParser(Message.Attachment msgattached, String serverId) {
        InputStream stream;
        String fileName="";
        CompletableFuture<InputStream> futureStream = new CompletableFuture<InputStream>();
        // using input stream instead of file due to given file not being stored & current file impl being changed in DiscordJDA
        try {
            fileName=msgattached.getFileName();
            msgattached.getProxy().download();
            parser(futureStream.get(), serverId);
        } catch (Exception e) {
            //activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
            System.out.println(e.getMessage());

        }
    }


    // test constructor
    public SheetParser(String serverId) {
        // todo implement direct file input (for testing purpose)
        File file= new File(System.getProperty("TEST_ENV_PATH")+"test.xlsx");
        InputStream targetStream=null;
        System.out.println(serverId);
        try {
            targetStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
        }
        parser(targetStream, serverId);
    }

    public void parser(InputStream msgattached, String serverId) {
        try {
            Workbook workbook = new XSSFWorkbook(msgattached);
            Sheet sheet = workbook.getSheetAt(0);

            /* TODO:
                Make columnName define header of column. Unable to grab from Apache POI
                without hacky stuff like checking if its bold. Instead iterate through
                first few rows looking for columns that contain one of the "parentColumns"
                Then get the given index (position) to id the column.
             */
            int i=0;
            System.out.println(serverId);
            System.out.println(Arrays.toString(Main.constants.config.get(serverId).getSheetParserParentColumns()));
            LinkedHashMap<String, String> nameIndexes;
            nameIndexes=setNameIndexes(sheet, serverId);
            for (Row row : sheet) {
                HashMap<String, String> data = new HashMap<String, String>();
                for (Cell cell : row) {
                    String[] parentColumns=Main.constants.config.get(serverId).getSheetParserParentColumns();

                    String currentIndex=CellReference.convertNumToColString(cell.getColumnIndex());
                    // Will need to offset this by row as parent loop is iterating over the whole sheet, something to think about I guess
                    for (String key: nameIndexes.keySet()) {
                        if (nameIndexes.get(key).contains(currentIndex)) {

                        }
                    }
                }
                i++;
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));

            //  activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
        }
    }

    private LinkedHashMap<String, String> setNameIndexes(Sheet sheet, String serverID) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        // Todo clean up this test prototyping code
        for (Row row : sheet) {
            for (Cell cell : row) {
                for (int i=0; i<Main.constants.config.get(serverID).getSheetParserParentColumns().length; i++) {
                   if (getColumnIndexOffset(Main.constants.config.get(serverID).getSheetParserParentColumns()[i], cell)==null) {
                   } else {
                       data.put(Main.constants.config.get(serverID).getSheetParserParentColumns()[i], getColumnIndexOffset(Main.constants.config.get(serverID).getSheetParserParentColumns()[i], cell));
                   }
                }
            }
        }
        return data;
    }

    private String getColumnIndexOffset(String targetColumn, Cell targetCell) {
        String columnIndex=null;
        if (targetCell.getStringCellValue().toLowerCase().contains(targetColumn)) {
            columnIndex=CellReference.convertNumToColString(targetCell.getColumnIndex());
        }
        return columnIndex;
    }

}
