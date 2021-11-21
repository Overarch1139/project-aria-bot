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
        if (dbExists()) {
            connection=openDB();
        } else {
            connection=openDB();
            setupDB(connection);
        }
    }

    public void checkEnv() {
        // this method checks whether running localhost or on server
        try {
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            if (dbUri!=null) {
                DB_URL= dbUri.getHost();
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



    public Boolean dbExists() {
        Connection connection = null;
        Statement statement = null;
        boolean exists=false;
        try {
            Class.forName("org.postgresql.Driver");
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
        } catch (ClassNotFoundException e) {
            // No driver class found!
        }
        try {
            connection.close();
        } catch (Exception x) {
            System.out.println("Unable to close DB");
        }
        return exists;
    }

    public Connection openDB() {
        Connection connect = null;
        Statement statement = null;
        try {
            Class.forName("org.postgresql.Driver");
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
        String sqlQuery="";
        switch (originModule) {
            case "CERT":
                    if (action.equals("add")) {
                        Date date = new Date();
                        Timestamp ts=new Timestamp(date.getTime());
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sqlQuery = "INSERT INTO CERT_MODULE VALUES ('" + data.get("discordID") + "','" + data.get("name") + "','" + data.get("emailAddr") + "','" + data.get("isVerified") + "','" + formatter.format(ts) +"');";
                    }
                break;
            default:
                System.out.println("[DB] Invalid Origin Module");
        }
        try {
            if (!sqlQuery.equals("")) {
                Statement st = connection.createStatement();
                st.executeUpdate(sqlQuery);
                st.close();
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
        // need to add execute statement
    }

    // todo this will need to be refactored to work universally with other tables.
    public String getDBEntry(String originModule, String req) {
        String ret="";
        String sqlQuery="";
        try {
            switch (originModule) {
                case "CERT":
                    // build sql query here
                    sqlQuery="SELECT * FROM CERT_MODULE WHERE discordID="+req;
                    break;
                case "NEWS":
                    break;
                case "WARN":
                    break;
                default:
                    System.err.println("[Database Module] Invalid origin module selected");
            }
                     Statement stmt  = this.connection.createStatement();
                     if (!sqlQuery.equals("")) {
                     ResultSet rs = stmt.executeQuery(sqlQuery);
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
