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
    private Connection connection;
    // this class is used for all instances of communication between db and application
    public Database() {
            checkEnv();
            connection=openDB();
            if (!tableExists("CERT_MODULE", connection)) {
                setupDB(connection);
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

    public Connection openDB() {
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
        switch (originModule) {
            case "CERT":
                    if (action.equals("add")) {
                        Date date = new Date();
                        Timestamp ts=new Timestamp(date.getTime());
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        PreparedStatement stmt;
                        try {
                             sqlQuery=connection.prepareStatement("INSERT INTO CERT_MODULE VALUES (?,?,?,?,?)");
                            sqlQuery.setLong(1, (long) data.get("discordID"));
                            sqlQuery.setString(2, data.get("name").toString());
                            sqlQuery.setString(3, data.get("emailAddr").toString());
                            sqlQuery.setBoolean(4, Boolean.parseBoolean(data.get("isVerified").toString()));
                            sqlQuery.setTimestamp(5, ts);
                        } catch (Exception e) {
                            System.out.println("Unable to Modify DB: "+ e.getMessage());
                        }
                    }
                break;
            default:
                System.out.println("[DB] Invalid Origin Module");
        }
        try {
            if (sqlQuery!=null) {
                ResultSet rs = sqlQuery.executeQuery();
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
        // need to add execute statement
    }

    // todo this will need to be refactored to work universally with other tables.
    public String getDBEntry(String originModule, String req) {
        String ret="";
        PreparedStatement sqlQuery=null;
        try {
            switch (originModule) {
                case "CERT":
                    // build sql query here
                    sqlQuery=connection.prepareStatement("SELECT * FROM CERT_MODULE WHERE discordID=?");
                    sqlQuery.setLong(1,Long.parseLong(req));
                    break;
                case "NEWS":
                    break;
                case "WARN":
                    break;
                default:
                    System.err.println("[Database Module] Invalid origin module selected");
            }
                     if (sqlQuery!=null) {
                     ResultSet rs = sqlQuery.executeQuery();
                        // loop through the result set
                         if (rs.next()) {
                             while (rs.next()) {
                                 ret="Name: "+rs.getString("name")+"\n";
                                 ret+="Email: "+rs.getString("emailAddr")+"\n";
                                 ret+="Verified Status: "+rs.getBoolean("isVerified")+"\n";
                             }
                         } else {
                             ret="No results found";
                         }
                    }

        } catch (SQLException e) {
            System.err.println(this.getClass().getName()+"Unable to get Entry"+e.getMessage());
        }
        return ret;
    }

    public void closeDB() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new Error("DB close failure: " + e.getMessage());
        }
    }

}
