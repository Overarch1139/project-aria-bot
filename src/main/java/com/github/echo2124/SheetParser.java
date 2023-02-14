package com.github.echo2124;

import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.github.echo2124.Main.constants.activityLog;
import static com.github.echo2124.Main.constants.db;

public class SheetParser {

    final String DELIMITER="##";
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
            chatgptcode(sheet);
            /* TODO:
                Make columnName define header of column. Unable to grab from Apache POI
                without hacky stuff like checking if its bold. Instead iterate through
                first few rows looking for columns that contain one of the "parentColumns"
                Then get the given index (position) to id the column.
             */
            /*
            int i=0;
            System.out.println(serverId);
            System.out.println(Arrays.toString(Main.constants.config.get(serverId).getSheetParserParentColumns()));
            LinkedHashMap<String, String[]> nameIndexes;
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

             */
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));

            //  activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
        }

    }

    private void chatgptcode(Sheet sheet) {
        // Get the iterator to go through each row in the sheet
        Iterator<Row> rowIterator = sheet.iterator();

        // Keep track of the column indices for the "first name" and "email" columns
        int firstNameIndex = -1;
        int emailIndex = -1;
        int rowIndex = -1;

        // Get the indices for the "first name" and "email" columns
            for (Row row : sheet) {
                if (firstNameIndex!=-1 && emailIndex!=-1) {
                    break;
                }
                for (Cell cell : row) {
                    if (firstNameIndex!=-1 && emailIndex!=-1) {
                        break;
                    }
                    try {
                        switch (cell.getStringCellValue().toLowerCase()) {
                            case "first name":
                                firstNameIndex = cell.getColumnIndex();
                                break;
                            case "email address":
                                emailIndex = cell.getColumnIndex();
                                rowIndex=cell.getRowIndex();
                                break;
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }

            }
        }

        // Check if both indices were found
        if (firstNameIndex == -1 || emailIndex == -1) {
            System.out.println("Could not find both columns in the sheet");
            return;
        }

        // Loop through each row in the sheet
        int i=rowIndex+1;
        System.out.println("First name index:"+firstNameIndex);
        System.out.println("Email index:"+emailIndex);

        while (i<sheet.getLastRowNum()) {
            Row row = sheet.getRow(i);
            String firstName=null, email=null;
            if (row.getCell(firstNameIndex,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)!=null) {
                firstName = row.getCell(firstNameIndex).getStringCellValue();
            }
            if (row.getCell(emailIndex,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)!=null) {
                email= row.getCell(emailIndex).getStringCellValue();
            }
            if (firstName!=null && email!=null) {
                // Print the values
                System.out.println("First name: " + firstName + ", email: " + email);

            }
            i++;
        }



    }


    // got the data now need to figure out what I need to do. Time to make a few calls...
    private String queryEntry(String email, String firstName, String guildId) {
        String data=null;
        data=(firstName+DELIMITER+email+DELIMITER+guildId);
        db.getDBEntry("CERT_SHEET", data);
        return data;
    }



}
