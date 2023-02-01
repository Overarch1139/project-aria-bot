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
            for (Row row : sheet) {
                HashMap<String, String> data = new HashMap<String, String>();
                for (Cell cell : row) {
                    String columnName=getCellName(cell);
                    System.out.println("Column Name:" +columnName);
                    String[] parentColumns=Main.constants.config.get(serverId).getSheetParserParentColumns();
                    for (int a=0; a<parentColumns.length; a++) {
                        if (columnName.contains(parentColumns[a])) {
                            String cellContents=cell.getStringCellValue();
                            if (cell.getStringCellValue().isEmpty()) {
                                cellContents="null";
                            }
                            // convert to db column format
                            if (parentColumns[a].toLowerCase().contains("email")) {
                                data.put("emailAddr", cellContents);
                                System.out.println("Email: "+cellContents);
                            }
                            if (parentColumns[a].toLowerCase().contains("first name")) {
                                data.put("name", cellContents);
                                System.out.println("Email: "+cellContents);
                            }
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

    // https://stackoverflow.com/questions/8202319/get-columns-names-in-excel-file-using-apache-poi
    private static String getCellName(Cell cell)
    {
        return CellReference.convertNumToColString(cell.getColumnIndex()) + (cell.getRowIndex() + 1);
    }

}
