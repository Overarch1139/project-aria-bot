package com.github.echo2124;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
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
    User member;
    String memberEmail;
    Message.Attachment msgattached;
    public SheetParser(Message.Attachment msgattached, String serverId, User member, String email, int modeset) {
        this.serverId=serverId;
        guild=Main.constants.jda.getGuildById(serverId);
        this.msgattached=msgattached;
        this.memberEmail=email;
        this.member=member;
        switchActiveState(modeset);
    }


    // 0=trigger scheduler; 1=add spreadsheet; 2=add spreadsheet (test method)
    public void switchActiveState(int modeset) {
        switch (modeset) {
            case 0:
                clubMemberSupervisor();
                break;
            case 1:
                initSpreadsheetParser();
                break;
            case 2:
                initTestSpreadsheetParser();
                break;
            case 3:
                modifyMemberStatus(member, memberEmail);
                break;
            default:
                System.out.println("Invalid modeset selected for sheetparser");
        }
    }

    public void initTestSpreadsheetParser() {
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



    public void initSpreadsheetParser() {
        String fileName="";
        CompletableFuture<InputStream> futureStream = new CompletableFuture<InputStream>();
        // using input stream instead of file due to given file not being stored & current file impl being changed in DiscordJDA
        try {
            fileName=msgattached.getFileName();
            futureStream=msgattached.getProxy().download();
            moduleEmbedResponses(0, 0);
            parser(futureStream.get(), serverId);
        } catch (Exception e) {
            //activityLog.sendActivityMsg("[SHEET_PARSER] "+e.getMessage(),3, null);
            System.out.println(e.getMessage());
        }
    }


    public void parser(InputStream msgattached, String serverId) {
        try {
            Workbook workbook = new XSSFWorkbook(msgattached);
            Sheet sheet = workbook.getSheetAt(0);
            if (getColumnIndexes(sheet)) {
                HashMap<String, String> data = new HashMap<>();
                data.put("club_name", Main.constants.config.get(serverId).getConfigName());
                db.modifyDB("CLUB_MEMBERS", "remove", data);
                getTableData(sheet);
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
        Boolean processDone=false;
        int x=0;
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
                processDone=true;
                // Print the values
                System.out.println("First name: " + firstName + ", email: " + email);
                insertEntry(email, firstName);
                x++;
            }
            i++;
        }
        if (processDone) {
            moduleEmbedResponses(1, x);
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


    // test method (for testing class functionality)
    private void loadTestData() {
/*
    INSERT INTO cert_module VALUES ("Aria Test Server", "Test_First_Name", "test0@student.monash.edu
    INSERT INTO cert_module (discordID, name, emailAddr, isVerified, verifiedTime, guildID) VALUES (538660576704856075, Joshua, test0@student.monash.edu, true, current_timestamp,878943527608938508);
    INSERT INTO cert_module (discordID, name, emailAddr, isVerified, verifiedTime, guildID) VALUES (257468559309930509, 'Test1', 'test1@student.monash.edu', true, current_timestamp,878943527608938508);
    INSERT INTO cert_module (discordID, name, emailAddr, isVerified, verifiedTime, guildID) VALUES (538660576704856075, 'Test2', 'test2@student.monash.edu', true, current_timestamp,878943527608938508);
INSERT INTO cert_module (discordID, name, emailAddr, isVerified, verifiedTime, guildID) VALUES (538660576704856075, 'Joshua', 'test0@student.monash.edu', true, current_timestamp,878943527608938508);
 */

    }


    // Handles checking current verified users against club member list
    private void clubMemberSupervisor() {
        /*
        TODO         also get everyone with member role and check them against member list.
        Drop role from user no longer in member list.
         */
        // make new method within DB interface to get club members since dbgetentry method services different way
        // something to look at later I guess
        ArrayList<String> clubMembers;
        clubMembers=db.getClubMembers(Main.constants.config.get(serverId).getConfigName());
        HashMap<Long, String> verifiedUsers;
        verifiedUsers=db.getGuildVerified(serverId);
        Role memberRole = guild.getRoleById(Main.constants.config.get(serverId).getRoleClubMemberId());
        Thread checkMembers = new Thread(){
            public void run(){
                moduleEmbedResponses(3,0);
                guild.loadMembers().get();
                // compute & time expensive loop, look at a more efficient method
                for (HashMap.Entry<Long, String> entry : verifiedUsers.entrySet()) {
                    Long discordId = entry.getKey();
                    String email = entry.getValue();
                    for (int i=0; i<clubMembers.size(); i++) {
                        if (clubMembers.get(i).contains(email)) {
                            // check if member already has role, if not then assign role
                            System.out.println("Discord ID:"+discordId);
                            Member member = guild.getMemberById(discordId);
                            System.out.println("Avatar ID:"+member.getAvatarId());
                            // if problem then its probably this. members are cached by jda, may need to force update cache
                            if (!member.getRoles().contains(memberRole)) {
                                manageMemberRole(member.getId(), 0);
                            }
                        }
                    }
                }
            }
        };
        checkMembers.start();
    }

    public void modifyMemberStatus(User user, String email) {
        Thread checkMembers = new Thread(){
           public void run() {
                guild.loadMembers().get();
                ArrayList<String> clubMembers;
                clubMembers = db.getClubMembers(Main.constants.config.get(serverId).getConfigName());
                Role memberRole = guild.getRoleById(Main.constants.config.get(serverId).getRoleClubMemberId());
                Member member = guild.getMemberById(user.getId());
                for (int i = 0; i < clubMembers.size(); i++) {
                   if (clubMembers.get(i).contains(email)) {
                       if (!member.getRoles().contains(memberRole)) {
                           manageMemberRole(member.getId(), 0);
                       }
                    }
                }
          }
        };
        checkMembers.run();
    }



    /* TODO
    - check for people to add every 24hrs or on request (to implement)
    - show statistics (on request/every week): wired_members/verified,
                       wired_members/wired_members (spreadsheet),
                       verified/discord_members
     */


    // 0=processing, 1=success, 2=failure
    private void moduleEmbedResponses(int modeset, int membersAdded) {
        EmbedBuilder embed = new EmbedBuilder();
        switch (modeset) {
            case 0:
                embed.setColor(Color.orange);
                embed.setTitle("Processing spreadsheet...");
                embed.setDescription("Parsing spreadsheet based on provided schema.");
                break;
            case 1:
                embed.setColor(Color.green);
                embed.setTitle("Spreadsheet has been parsed successfully!");
                embed.setDescription("Number of club members added to DB: "+membersAdded);
                break;
            case 2:
                embed.setColor(Color.red);
                embed.setTitle("Invalid Spreadsheet Detected!");
                embed.setDescription("Make sure that spreadsheet matches schema and it is using the correct format (xlsx). If issues still persist contact Joshua.");
                break;
            case 3:
                embed.setColor(Color.CYAN);
                embed.setTitle("Running Club Member Supervisor...");
                embed.setDescription("Will check club members against verified table and add any newly detected club members to role.");
                break;
        }
        Main.constants.jda.getTextChannelById(Main.constants.config.get(serverId).getChannelAdminId()).sendMessageEmbeds(embed.build()).queue();
    }
    private String queryEntry(String email, String firstName, String guildId) {
        String data=null;
        data=(firstName+DELIMITER+email+DELIMITER+guildId);
        db.getDBEntry("CERT_ALT", data);
        return data;
    }



}
