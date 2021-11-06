package com.github.echo2124;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    // this class is used for all instances of communication between db and application
    public Database() {
    }

    public Connection openDB() {
        Connection connect = null;
        try {
            Class.forName("org.postgresql.Driver");
            connect= DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/masterDB",
                            "admin", "2008");
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
            String sqlQuery = "CREATE TABLE WARN_MODULE (discordID int, issuerID int, warnDesc text, time DATETIME);"+
                            "CREATE TABLE CERT_MODULE (discordID int, name VARCHAR(50), emailAddr VARCHAR(50), time DATETIME);"+
                            "CREATE TABLE NEWS (origin VARCHAR(50), lastTitle text);";
            stmt.executeUpdate(sqlQuery);
            stmt.close();

        } catch (Exception e) {
            System.err.println("Unable to setup DB " + e.getMessage());
        }
    }

    public void modifyDB(Connection connect, String originModule, String action, String data) {
        try {
            String sqlQuery;
            Statement st = connect.createStatement();
        } catch (Exception e) {
            System.err.println(this.getClass().getName()+"Modify DB failed"+e.getMessage());
        }
    }

    public void getDBEntry(Connection connect, String originModule, String req) {
        try {
            switch (originModule) {
                case "CERT":
                    // build sql query here
                    break;
                case "NEWS":
                    break;
                case "WARN":
                    break;
                default:
                    System.err.println("[Database Module] Invalid origin module selected");
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getName()+"Unable to get Entry"+e.getMessage());
        }
    }

}
