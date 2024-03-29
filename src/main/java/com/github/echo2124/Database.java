// update codename ARIA - Administrate, Relay, Identify, Attest.

package com.github.echo2124;


import net.dv8tion.jda.api.EmbedBuilder;

import java.net.URI;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;

import static com.github.echo2124.Main.constants.activityLog;
import static com.github.echo2124.Main.constants.db;

public class Database {
    private String DB_URL;
    private String USERNAME;
    private String PASSWORD;
    // this class is used for all instances of communication between db and application
    public Database() {
            checkEnv();
            Connection tempConnection=connect();
            //migrateDB(tempConnection)
            disconnect(tempConnection);
    }

    public void disconnect(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Unable to disconnect from db");
        }
    }
    public void checkEnv() {
        try {
            URI dbUri = new URI(System.getProperty("DATABASE_URL"));
            System.out.println("Host:"+dbUri.getHost());
            System.out.println("Port:"+dbUri.getPort());
            System.out.println("User:"+dbUri.getUserInfo());
            System.out.println("Path:"+dbUri.getPath());
            if (dbUri!=null) {
                DB_URL= "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
                USERNAME=dbUri.getUserInfo().split(":")[0];
                PASSWORD = dbUri.getUserInfo().split(":")[1];
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            // todo move to config
            DB_URL="jdbc:postgresql://localhost:5432/project_aria_bot";
            USERNAME="system";
            PASSWORD="test1234";

        }
        }

    public boolean tableExists(String tableName, Connection conn) {
        boolean found = false;
        try {
        DatabaseMetaData databaseMetaData = conn.getMetaData();
        ResultSet rs = databaseMetaData.getTables(null, null, tableName, null);
        while (rs.next()) {
            String name = rs.getString("CERT_MODULE");
            if (tableName.equals(name)) {
                found = true;
                break;
            }
        }} catch (Exception e ) {
            System.out.println("Error thrown trying to see if table exists. Error: "+e.getMessage());
        }

        return found;
    }

    public Connection connect() {

        Connection connect = null;
        try {
            connect = DriverManager.getConnection(DB_URL,
                    USERNAME, PASSWORD);

            Statement stmt = connect.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database");

            boolean databaseExists = false;
            while (rs.next()) {
                String dbName = rs.getString("datname");
                if (dbName.equals("project_aria_bot")) {
                    databaseExists = true;
                    break;
                }
            }

            if (!databaseExists) {
                stmt.executeUpdate("CREATE DATABASE project_aria_bot");
                setupDB(connect);
            }

            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
      //  activityLog.sendActivityMsg("[DATABASE] Connected Successfully",1, null);
       // System.out.println("Opened database successfully");
    return connect;
    }

    public void setupDB(Connection connect) {
            // handles setting up database if blank
        try {
            // sanitisation not needed here as no inputs are received
            Statement stmt = connect.createStatement();
            String sqlQuery = "CREATE TABLE WARN_MODULE (discordID bigint, issuerID bigint, warnDesc text, issueTime TIMESTAMP);"+
                            "CREATE TABLE CERT_MODULE (discordID bigint, name VARCHAR(2048), emailAddr VARCHAR(100), isVerified bool, verifiedTime TIMESTAMP, guildID VARCHAR(64));"+
                            "CREATE TABLE NEWS (origin VARCHAR(50), lastTitle text);"+
                    "CREATE TABLE CLUB_MEMBERS (club_name text, first_name text, email text)";
            stmt.executeUpdate(sqlQuery);
            stmt.close();

        } catch (Exception e) {
            if (!e.getMessage().contains("warn_module")) {
                System.err.println("Unable to setup DB " + e.getMessage());
                activityLog.sendActivityMsg("[DATABASE] Unable to setup DB",2, null);
            }
        }
    }

    // Migrate to new schema for multiguild support
    public void migrateDB(Connection connect) {
        try {
            Statement stmt = connect.createStatement();
            String query = "ALTER TABLE CERT_MODULE ADD guildID VARCHAR(64);"+
                           "UPDATE CERT_MODULE SET guildID=CAST(802526304745553930 AS VARCHAR);";
            stmt.executeUpdate(query);
            stmt.close();
        } catch (Exception e) {
            System.out.println("Unable to migrate db schema"+ e.getMessage());
        }
    }

    public void upstreamSchemaChanges(Connection connect) {
        try {
            Statement stmt = connect.createStatement();
            String query = "CREATE TABLE CLUB_MEMBERS (club_name text, first_name text, email text);";
            stmt.executeUpdate(query);
            stmt.close();
        } catch (Exception e) {
            System.out.println("Unable to migrate db schema"+ e.getMessage());
        }
    }

    public void modifyDB(String originModule, String action, HashMap data) {
        PreparedStatement sqlQuery=null;
        Connection connection = connect();
        Date date = new Date();
        Timestamp ts=new Timestamp(date.getTime());
        switch (originModule) {
            case "CERT":
                    if (action.equals("add")) {
                        try {
                            activityLog.sendActivityMsg("[DATABASE] Inserting verify data into verify table",1, data.get("guildID").toString());
                             sqlQuery=connection.prepareStatement("INSERT INTO CERT_MODULE VALUES (?,?,?,?,?,?)");
                            sqlQuery.setLong(1, Long.parseLong(data.get("discordID").toString()));
                            sqlQuery.setString(2, data.get("name").toString());
                            sqlQuery.setString(3, data.get("emailAddr").toString());
                            sqlQuery.setBoolean(4, Boolean.parseBoolean(data.get("isVerified").toString()));
                            sqlQuery.setTimestamp(5, ts);
                            sqlQuery.setString(6, data.get("guildID").toString());
                        } catch (Exception e) {
                            activityLog.sendActivityMsg("[DATABASE] Unable to insert verify data into table", 3, data.get("guildID").toString());
                            System.out.println("Unable to Modify DB: "+ e.getMessage());
                        }
                    } else if (action.equals("remove")) {
                        try {
                            activityLog.sendActivityMsg("[DATABASE] Removing entry from verify table",1, data.get("guildID").toString());
                            sqlQuery=connection.prepareStatement("DELETE FROM CERT_MODULE WHERE Discordid=? AND Guildid=?");
                            sqlQuery.setLong(1, Long.parseLong(data.get("discordID").toString()));
                            sqlQuery.setString(2, data.get("guildID").toString());
                        } catch (Exception e) {
                            activityLog.sendActivityMsg("[DATABASE] Unable to remove verify data from table", 3, data.get("guildID").toString());
                            System.out.println("Unable to Modify DB: "+ e.getMessage());
                        }
                    }
                break;
            case "CLUB_MEMBERS":
                if (action.equals("add")) {
                    try {
                        sqlQuery=connection.prepareStatement("INSERT INTO CLUB_MEMBERS VALUES (?,?,?)");
                        System.out.println("Data dump:"+data.get("club_name")+";"+data.get("first_name")+";"+data.get("email"));
                        sqlQuery.setString(1, data.get("club_name").toString());
                        sqlQuery.setString(2, data.get("first_name").toString());
                        sqlQuery.setString(3, data.get("email").toString());
                    } catch (Exception e) {
                        e.printStackTrace();

                        activityLog.sendActivityMsg("[DATABASE] Unable to insert verify data into table", 3, data.get("guildID").toString());
                        System.out.println("(ADD) Unable to Modify DB: "+ e.getMessage());
                    }
                } else if (action.equals("remove")) {
                    try {
                        activityLog.sendActivityMsg("[DATABASE] Removing entry from verify table",1, null);
                        sqlQuery=connection.prepareStatement("DELETE FROM CLUB_MEMBERS WHERE club_name=?");
                        System.out.println("CONFIG NAME:  "+data.get("club_name"));
                        sqlQuery.setString(1, data.get("club_name").toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        activityLog.sendActivityMsg("[DATABASE] Unable to remove verify data from table", 3, null);
                        System.out.println("(REMOVE) Unable to Modify DB: "+ e.getMessage());

                    }
                }
                break;
            case "NEWS":
                    try {
                        if (Boolean.parseBoolean(this.getDBEntry("NEWS_CHECK_CATEGORY", action))) {
                            sqlQuery = connection.prepareStatement("DELETE FROM NEWS WHERE origin=?");
                            sqlQuery.setString(1, action);
                            sqlQuery.execute();
                        }
                        activityLog.sendActivityMsg("[DATABASE] Updating news data in news table",1, null);
                        sqlQuery = connection.prepareStatement("INSERT INTO NEWS VALUES (?,?)");
                        sqlQuery.setString(1, action);
                        sqlQuery.setString(2, data.get("title").toString());
                    } catch (Exception e) {
                        System.out.println("Unable to Modify DB: " + e.getMessage());
                    }
                break;
            case "EXPOSURE_SITE":
                try {
                    activityLog.sendActivityMsg("[DATABASE] Inserting exposure data into exposure table",1, null);
                    sqlQuery = connection.prepareStatement("UPDATE exposure SET len=? WHERE origin='EXPOSURE_SITE'");
                    sqlQuery.setInt(1,Integer.parseInt(data.get("size").toString()));
                } catch (Exception e) {
                    System.out.println("UNABLE TO MODIFY EXPOSURE_SITE MSG:"+e.getMessage());
                }
                break;
            default:
                System.out.println("[DB] Invalid Origin Module");
        }
        try {
            if (sqlQuery!=null) {
                sqlQuery.execute();
            }
            disconnect(connection);
       //     activityLog.sendActivityMsg("[DATABASE] Connection closed",1, null);
        } catch (Exception e) {

            activityLog.sendActivityMsg("[DATABASE] Failed to modify: "+e.getMessage(),3, null);
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
    }

    public String getDBEntry(String originModule, String req) {
        System.out.println("Grabbing DB Entry");
        String ret="";
        String[] parsed;
        ResultSet rs;
        PreparedStatement sqlQuery;
        Connection connection=connect();
        try {
            switch (originModule) {
                case "CERT":
                    activityLog.sendActivityMsg("[DATABASE] Fetching verify data from verify table",1, null);
                sqlQuery=connection.prepareStatement("SELECT * FROM CERT_MODULE WHERE discordID=? AND guildID=?");
                parsed=req.split("##");
                sqlQuery.setLong(1,Long.parseLong(parsed[0]));
                sqlQuery.setString(2, parsed[1]);
                if (sqlQuery!=null) {
                    rs = sqlQuery.executeQuery();
                    System.out.println("Ran query");
                    // loop through the result set
                    while (rs.next()) {
                        ret="Name: "+rs.getString(2)+"\n";
                        ret+="Verified Status: "+rs.getBoolean(4)+"\n";
                        ret+="Time of Verification: "+rs.getTimestamp(5)+"\n";
                    }
                    System.out.println("Query result: \n"+req);
                    if (ret=="") {
                        ret = "No results found";
                    }
                }
                break;
                // alternate method (using name & email to fetch entry)
                case "CERT_ALT":
                    activityLog.sendActivityMsg("[DATABASE] Fetching verify data from verify table",1, null);
                    sqlQuery=connection.prepareStatement("SELECT * FROM CERT_MODULE WHERE name=? AND email=? AND guildID=?");
                    parsed=req.split("##");
                    sqlQuery.setString(1,parsed[0]);
                    sqlQuery.setString(2, parsed[1]);
                    sqlQuery.setString(3, parsed[2]);
                       rs = sqlQuery.executeQuery();
                        System.out.println("Ran query");
                        // loop through the result set
                        while (rs.next()) {
                            ret="Name: "+rs.getString(2)+"\n";
                            ret+="Verified Status: "+rs.getBoolean(4)+"\n";
                            ret+="Time of Verification: "+rs.getTimestamp(5)+"\n";
                        }
                        System.out.println("Query result: \n"+req);
                        if (ret=="") {
                            ret = "No results found";
                        }
                    break;
                case "CLUB_MEMBERS":
                    activityLog.sendActivityMsg("[DATABASE] Fetching verify data from verify table",1, null);
                    sqlQuery=connection.prepareStatement("SELECT * FROM CLUB_MEMBERS WHERE club_name=? AND first_name=? AND email=?");
                    parsed=req.split("##");
                    sqlQuery.setString(1,parsed[0]);
                    sqlQuery.setString(2, parsed[1]);
                    sqlQuery.setString(3, parsed[2]);
                    rs = sqlQuery.executeQuery();
                    System.out.println("Ran query");
                    // loop through the result set
                    while (rs.next()) {
                        ret="Name: "+rs.getString(2)+"\n";
                        ret+="Verified Status: "+rs.getBoolean(4)+"\n";
                        ret+="Time of Verification: "+rs.getTimestamp(5)+"\n";
                    }
                    System.out.println("Query result: \n"+req);
                    if (ret=="") {
                        ret = "No results found";
                    }
                    break;

                case "NEWS_CHECK_CATEGORY":
                    activityLog.sendActivityMsg("[DATABASE] Fetching news data from news table",1, null);
                    sqlQuery=connection.prepareStatement("SELECT * FROM NEWS WHERE origin=?");
                    sqlQuery.setString(1, req);
                    if (sqlQuery!=null) {
                         rs = sqlQuery.executeQuery();
                        while (rs.next()) {
                            ret=rs.getString(1);
                        }
                        if (ret.equals(req)) {
                            ret="true";
                        } else {
                            ret="false";
                        }
                        System.out.println("[Database] News Category Exists="+ret);
                    }

                    break;
                case "NEWS_CHECK_LASTITLE":
                    sqlQuery=connection.prepareStatement("SELECT * FROM NEWS WHERE origin=? AND lastTitle=?");
                    parsed=req.split("##");
                    System.out.println("[Database] Split value origin: "+parsed[0]);
                    System.out.println("[Database] Split value lastTitle: "+parsed[1]);
                    sqlQuery.setString(1, parsed[0]);
                    sqlQuery.setString(2,parsed[1]);
                    if (sqlQuery!=null) {
                        rs = sqlQuery.executeQuery();
                        while (rs.next()) {
                            ret=rs.getString(2);
                        }
                        if (ret.equals(parsed[1])) {
                            ret="true";
                        } else {
                            ret="false";
                        }
                        System.out.println("[Database] Last News Title Exists="+ret);
                    }
                    break;
                case "CHECK_EXPOSURE_INDEX":
                    activityLog.sendActivityMsg("[DATABASE] Fetching exposure data from exposure table",1, null);
                    //TODO check for origin instead (there is probably an issue with the current method of checking for a table which is causing these sorts of problems that exist currently)
                    rs = connection.prepareStatement("SELECT EXISTS ( SELECT FROM pg_tables WHERE tablename='exposure');").executeQuery();
                    while (rs.next()) {
                        if (rs.getBoolean(1)) {
                            System.out.println("[Database] checking db for exposure info");
                            sqlQuery = connection.prepareStatement("SELECT len FROM EXPOSURE WHERE origin=?");
                            sqlQuery.setString(1, req);
                            if (sqlQuery != null) {
                                ResultSet res = sqlQuery.executeQuery();
                                while (res.next()) {
                                    ret = String.valueOf(res.getInt(1));
                                }
                                if (ret == null || ret == "") {
                                    ret = "0";
                                }
                            }
                        } else {
                            System.out.println("[Database] exposure table doesn't exist. creating...");
                            // ADD TABLE TO DB (EXPOSURE)
                            connection.prepareStatement("CREATE TABLE EXPOSURE (origin VARCHAR(50), len NUMERIC(15));").executeQuery();
                            ret="0";
                        }
                    break;
                    }
            }
        } catch (SQLException e) {
            System.err.println(this.getClass().getName()+"Unable to get Entry"+e.getMessage());
        }
        activityLog.sendActivityMsg("[DATABASE] Connection closed",1, null);
        disconnect(connection);
        return ret;
    }

    // TODO Merge getClubMembers and getGuildVerified
    public ArrayList<String> getClubMembers(String clubName) {
        ResultSet rs;
        PreparedStatement sqlQuery;
        Connection connection=connect();
        ArrayList<String> data= new ArrayList<String>();
        try {
            sqlQuery=connection.prepareStatement("SELECT * FROM CLUB_MEMBERS WHERE club_name=?;");
            sqlQuery.setString(1, clubName);
            rs = sqlQuery.executeQuery();
            System.out.println("Ran query");
            // loop through the result set
            while (rs.next()) {
                data.add(rs.getString(3));
            }
            } catch(SQLException e){
            System.out.println(e);
            }
        return data;
    }

    // Hashmap (discordId, email)
    public HashMap<Long, String> getGuildVerified(String guildId) {
        ResultSet rs;
        PreparedStatement sqlQuery;
        Connection connection=connect();
        HashMap<Long, String> data= new HashMap<Long, String>();
        try {
            sqlQuery=connection.prepareStatement("SELECT * FROM CERT_MODULE WHERE guildId=?;");
            sqlQuery.setString(1, guildId);
            rs = sqlQuery.executeQuery();
            System.out.println("Ran query");
            // loop through the result set
            while (rs.next()) {
                data.put(rs.getLong(1),rs.getString(3));
            }
        } catch(SQLException e){
            System.out.println(e);
        }
        return data;
    }
}
