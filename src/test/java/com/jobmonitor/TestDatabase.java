package com.jobmonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Lets database tests skip themselves until db/create-database.sql has been run. */
public final class TestDatabase {

    private static final String URL = "jdbc:postgresql://localhost:5432/job_monitor_test";
    private static Boolean available;

    private TestDatabase() {
    }

    public static synchronized boolean isAvailable() {
        if (available == null) {
            String password = System.getenv("JOB_MONITOR_DB_PASSWORD");
            if (password == null || password.isBlank()) {
                available = false;
            } else {
                DriverManager.setLoginTimeout(3);
                try (Connection ignored = DriverManager.getConnection(URL, "job_monitor_app", password)) {
                    available = true;
                } catch (SQLException e) {
                    available = false;
                }
            }
        }
        return available;
    }
}
