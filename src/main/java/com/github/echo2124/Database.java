// update codename ARIA - Administrate, Relay, Identify, Attest.

package com.github.echo2124;


import net.dv8tion.jda.api.EmbedBuilder;

import java.net.URI;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;

import static com.github.echo2124.Main.constants.activityLog;

public class Database {
    private String DB_URL;
    private String USERNAME;
    private String PASSWORD;
    // this class is used for all instances of communication between db and application
    public Database() {
            checkEnv();
            Connection tempConnection=connect();
            if (!tableExists("CERT_MODULE", tempConnection)) {
                setupDB(tempConnection);
            }
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
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            if (dbUri!=null) {
                DB_URL= "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?sslmode=require";
                USERNAME=dbUri.getUserInfo().split(":")[0];
                PASSWORD = dbUri.getUserInfo().split(":")[1];
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            System.out.println("Unable to check env: "+e.getMessage());
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
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        activityLog.sendActivityMsg("[DATABASE] Connected Successfully",1);
        System.out.println("Opened database successfully");
    return connect;
    }

    public void setupDB(Connection connect) {
            // handles setting up database if blank
        try {
            // sanitisation not needed here as no inputs are received
            Statement stmt = connect.createStatement();
            String sqlQuery = "CREATE TABLE WARN_MODULE (discordID bigint, issuerID bigint, warnDesc text, issueTime TIMESTAMP);"+
                            "CREATE TABLE CERT_MODULE (discordID bigint, name VARCHAR(2048), emailAddr VARCHAR(100), isVerified bool, verifiedTime TIMESTAMP);"+
                            "CREATE TABLE NEWS (origin VARCHAR(50), lastTitle text);"+
                            "CREATE TABLE EXPOSURE (origin VARCHAR(50), len NUMERIC(15));"+
                            "CREATE TABLE ONCAMPUS (discordID bigint, )";
            stmt.executeUpdate(sqlQuery);
            stmt.close();

        } catch (Exception e) {
            if (!e.getMessage().contains("warn_module")) {
                System.err.println("Unable to setup DB " + e.getMessage());
                activityLog.sendActivityMsg("[DATABASE] Unable to setup DB",2);
            }
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
                            activityLog.sendActivityMsg("[DATABASE] Inserting verify data into verify table",1);
                             sqlQuery=connection.prepareStatement("INSERT INTO CERT_MODULE VALUES (?,?,?,?,?)");
                            sqlQuery.setLong(1, Long.parseLong(data.get("discordID").toString()));
                            sqlQuery.setString(2, data.get("name").toString());
                            sqlQuery.setString(3, data.get("emailAddr").toString());
                            sqlQuery.setBoolean(4, Boolean.parseBoolean(data.get("isVerified").toString()));
                            sqlQuery.setTimestamp(5, ts);
                        } catch (Exception e) {
                            System.out.println("Unable to Modify DB: "+ e.getMessage());
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
                        activityLog.sendActivityMsg("[DATABASE] Updating news data in news table",1);
                        sqlQuery = connection.prepareStatement("INSERT INTO NEWS VALUES (?,?)");
                        sqlQuery.setString(1, action);
                        sqlQuery.setString(2, data.get("title").toString());
                    } catch (Exception e) {
                        System.out.println("Unable to Modify DB: " + e.getMessage());
                    }
                break;
            case "EXPOSURE_SITE":
                try {
                    activityLog.sendActivityMsg("[DATABASE] Inserting exposure data into exposure table",1);
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
            activityLog.sendActivityMsg("[DATABASE] Connection closed",1);
        } catch (Exception e) {
            activityLog.sendActivityMsg("[DATABASE] Failed to modify: "+e.getMessage(),3);
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
    }

    // Doing this because heroku db is readonly via the web app query tool and using postgres remotely via their app is aids.
    // Run once on existing table to update structure, if initial run don't bother
    public void modifyDbSchemaForExposure() {
        Connection connection=connect();
        try {
            connection.prepareStatement("ALTER TABLE exposure DROP COLUMN origin, len;").executeQuery();
            // UID (auto-gen, int), Campus string, Building String, ExposurePeriod String, CleaningStatus String, HealthAdvice String, retrieved DATETIME
            connection.prepareStatement("ALTER TABLE exposure ADD (UID SERIAL PRIMARY KEY, Building TEXT, ExposurePeriod TEXT, CleaningStatus TEXT, HealthAdvice TEXT, RetrievedTime TIMESTAMP);").executeQuery();
        } catch (SQLException e) {
            System.out.println("Unable to modify schema: "+e.getMessage());
        }
    }


    public String getDBEntry(String originModule, String req) {
        System.out.println("Grabbing DB Entry");
        String ret="";
        PreparedStatement sqlQuery;
        Connection connection=connect();
        try {
            switch (originModule) {
                case "CERT":
                    activityLog.sendActivityMsg("[DATABASE] Fetching verify data from verify table",1);
                sqlQuery=connection.prepareStatement("SELECT * FROM CERT_MODULE WHERE discordID=?");
                sqlQuery.setLong(1,Long.parseLong(req));
                if (sqlQuery!=null) {
                    ResultSet rs = sqlQuery.executeQuery();
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
                case "NEWS_CHECK_CATEGORY":
                    activityLog.sendActivityMsg("[DATABASE] Fetching news data from news table",1);
                    sqlQuery=connection.prepareStatement("SELECT * FROM NEWS WHERE origin=?");
                    sqlQuery.setString(1, req);
                    if (sqlQuery!=null) {
                        ResultSet rs = sqlQuery.executeQuery();
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
                    String[] parsed=req.split("##");
                    System.out.println("[Database] Split value origin: "+parsed[0]);
                    System.out.println("[Database] Split value lastTitle: "+parsed[1]);
                    sqlQuery.setString(1, parsed[0]);
                    sqlQuery.setString(2,parsed[1]);
                    if (sqlQuery!=null) {
                        ResultSet rs = sqlQuery.executeQuery();
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
                    activityLog.sendActivityMsg("[DATABASE] Fetching exposure data from exposure table",1);
                    //TODO check for origin instead (there is probably an issue with the current method of checking for a table which is causing these sorts of problems that exist currently)
                    ResultSet rs = connection.prepareStatement("SELECT EXISTS ( SELECT FROM pg_tables WHERE tablename='exposure');").executeQuery();
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
        activityLog.sendActivityMsg("[DATABASE] Connection closed",1);
        disconnect(connection);
        return ret;
    }


}
