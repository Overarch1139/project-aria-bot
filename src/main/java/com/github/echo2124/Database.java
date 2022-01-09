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

    // still crash, check if its switching between the different creds for postgres based on whether its a server or not
    public void checkEnv() {
        // this method checks whether running localhost or on server
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
            DB_URL="localhost:5432/postgres";
            USERNAME="postgres";
            PASSWORD="2008";
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


 /*   public Boolean dbExists() {
        Connection connection = null;
        Statement statement = null;
        boolean exists=false;
        try {
            connection = DriverManager.getConnection(DB_URL,
                    USERNAME, PASSWORD);
            statement = connection.createStatement();
            String sql = "CREATE DATABASE BOT";
            statement.executeUpdate(sql);
            sql = "DROP DATABASE BOT";
            statement.executeUpdate(sql);
            System.out.println("Database created!");
        } catch (SQLException sqlException) {
            if (sqlException.getErrorCode() == 1007) {
                // Database already exists error
                exists=true;
                System.out.println(sqlException.getMessage());
                System.out.println("[DB Module] DB has been found");
            } else {
                // Some other problems, e.g. Server down, no permission, etc
                sqlException.printStackTrace();
            }
            try {
                connection.close();
            } catch (Exception e) {
                System.out.println("Unable to close connection");
            }
        }
        try {
            connection.close();
        } catch (Exception x) {
            System.out.println("Unable to close DB");
        }
        return exists;
    }*/

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
                            "CREATE TABLE NEWS (origin VARCHAR(50), lastTitle text);";
            stmt.executeUpdate(sqlQuery);
            stmt.close();

        } catch (Exception e) {
            if (!e.getMessage().contains("warn_module")) {
                System.err.println("Unable to setup DB " + e.getMessage());
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
                        if (!Boolean.parseBoolean(this.getDBEntry("NEWS_CHECK_CATEGORY", action))) {
                            sqlQuery = connection.prepareStatement("DELETE FROM NEWS WHERE origin=?");
                            sqlQuery.setString(1, action);
                            sqlQuery.executeQuery();
                        }
                        sqlQuery = connection.prepareStatement("INSERT INTO NEWS VALUES (?,?)");
                        sqlQuery.setString(1, action);
                        sqlQuery.setString(2, data.get("title").toString());
                    } catch (Exception e) {
                        System.out.println("Unable to Modify DB: " + e.getMessage());
                    }
                break;
            default:
                System.out.println("[DB] Invalid Origin Module");
        }
        try {
            if (sqlQuery!=null) {
                sqlQuery.executeQuery();
            }
            disconnect(connection);
        } catch (Exception e) {
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
        // need to add execute statement
    }

    // todo this will need to be refactored to work universally with other tables.
    public String getDBEntry(String originModule, String req) {
        System.out.println("Grabbing DB Entry");
        String ret="";
        PreparedStatement sqlQuery;
        Connection connection=connect();
        try {
            switch (originModule) {
                case "CERT":
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
                    sqlQuery=connection.prepareStatement("SELECT * FROM NEWS WHERE origin=?, lastTitle=?");
                    String[] x=req.split("|");
                    sqlQuery.setString(1, x[0]);
                    sqlQuery.setString(2,x[1]);
                    if (sqlQuery!=null) {
                        ResultSet rs = sqlQuery.executeQuery();
                        while (rs.next()) {
                            ret=rs.getString(2);
                        }
                        if (ret.equals(req)) {
                            ret="true";
                        } else {
                            ret="false";
                        }
                        System.out.println("[Database] Last News Title Exists="+ret);
                    }
                    break;
            }

        } catch (SQLException e) {
            System.err.println(this.getClass().getName()+"Unable to get Entry"+e.getMessage());
        }
        disconnect(connection);
        return ret;
    }


}
