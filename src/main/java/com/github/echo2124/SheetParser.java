package com.github.echo2124;

import net.dv8tion.jda.api.entities.*;
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
    int firstNameIndex = -1;
    int emailIndex = -1;
    int rowIndex = -1;
    Guild guild;
    String serverId;
    public SheetParser(Message.Attachment msgattached, String serverId) {
        initClassVariables(serverId);
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
        initClassVariables(serverId);
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

    // Doing it this way because there are two constructors. Means that I don't need to repeat the same code in both constructors
    public void initClassVariables(String serverId) {
        serverId=this.serverId;
        guild=Main.constants.jda.getGuildById(serverId);
    }

    public void parser(InputStream msgattached, String serverId) {
        try {
            Workbook workbook = new XSSFWorkbook(msgattached);
            Sheet sheet = workbook.getSheetAt(0);
            if (getColumnIndexes(sheet)) {
                getTableData(sheet);
                HashMap<String, String> data = new HashMap<>();
                data.put("club_name", Main.constants.config.get(serverId).getConfigName());
                db.modifyDB("CLUB_MEMBERS", "remove", data);
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));

            //  activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
        }

    }


    private Boolean getColumnIndexes(Sheet sheet) {
        Boolean isValid=false;
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
        if (firstNameIndex>0 || emailIndex>0) {
           isValid=true;
        } else {
            System.out.println("Could not find both columns in the sheet");
        }
        return isValid;
    }

    private void getTableData(Sheet sheet) {
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
                insertEntry(email, firstName);
            }
            i++;
        }
    }


    // inserts into CLUB_MEMBERS table
    private void insertEntry(String email, String firstName) {
        HashMap<String, String> data= new HashMap<>();
        data.put("club_name", Main.constants.config.get(serverId).getConfigName());
        data.put("first_name", firstName);
        data.put("email", email);
        db.modifyDB("CLUB_MEMBERS", "add", data);
    }



    private void manageMemberRole(String discordID, int modeset) {

        Member member = guild.retrieveMemberById(discordID).complete();
        User user = Main.constants.jda.getUserById(discordID);
        // modeset==0 means add role; modeset==1 remove role
        if (modeset==0) {
            guild.addRoleToMember(member, guild.getRoleById(Main.constants.config.get(serverId).getRoleClubMemberId())).queue();
        } else if (modeset==1) {
            try {
                guild.removeRoleFromMember(member, guild.getRoleById(Main.constants.config.get(serverId).getRoleClubMemberId())).queue();
            } catch (NullPointerException e) {
                activityLog.sendActivityMsg("Unable to remove role, discord id is probably wrong or doesn't exist", 3, serverId);
            }
        }
    }


    // Handles checking current verified users against club member list
    private void clubMemberSupervisor() {
        /* grab all members from verified db table that have wired guild id
        check it against club member list and if match add member

        also get everyone with member role and check them against member list.
        Drop role from user no longer in member list.
         */
        // make new method within DB interface to get club members since dbgetentry method services different way
        // something to look at later I guess
        ArrayList<String> clubMembers;
        clubMembers=db.getClubMembers(Main.constants.config.get(serverId).getConfigName());
        HashMap<Long, String> verifiedUsers;
        verifiedUsers=db.getGuildVerified(serverId);
        Role memberRole = guild.getRoleById(Main.constants.config.get(serverId).getRoleClubMemberId());
        for (HashMap.Entry<Long, String> entry : verifiedUsers.entrySet()) {
            Long discordId = entry.getKey();
            String email = entry.getValue();
            for (int i=0; i<clubMembers.size(); i++) {
                if (clubMembers.contains(email)) {
                    // check if member already has role, if not then assign role
                    Member member = guild.getMemberById(discordId);
                    if (!member.getRoles().contains(memberRole)) {
                        manageMemberRole(member.getId(), 0);
                    }
                }
            }
        }

    }



    /* TODO
    When we parse a new spreadsheet we remove all data that has "club_name" from CLUB_MEMBERS table
    Then insert all relevant data from spreadsheet into table.

    - if verified person is in spreadsheet then add them to a "wired member role"
    - check for people to add every 24hrs or on request
    - show statistics (on request/every week): wired_members/verified,
                       wired_members/wired_members (spreadsheet),
                       verified/discord_members



     */
    private String queryEntry(String email, String firstName, String guildId) {
        String data=null;
        data=(firstName+DELIMITER+email+DELIMITER+guildId);
        db.getDBEntry("CERT_ALT", data);
        return data;
    }



}
