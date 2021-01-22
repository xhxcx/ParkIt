package com.parkit.parkingsystem.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DataBaseConfig {

    private static final Logger logger = LogManager.getLogger("DataBaseConfig");

    public Connection getConnection() throws ClassNotFoundException, SQLException {
        logger.info("Create DB connection");
        Properties dbProperties = new Properties();
        try (FileInputStream dbConfigFile = new FileInputStream("src/main/resources/db_config.properties")){
            dbProperties.load(dbConfigFile);

        } catch (IOException e) {
            logger.error("Fail to load database config",e);
        }
        Class.forName(dbProperties.getProperty("jdbc.driver.class"));
        return DriverManager.getConnection(dbProperties.getProperty("jdbc.host.prod"),dbProperties.getProperty("jdbc.username"),dbProperties.getProperty("jdbc.password"));
    }

    public void closeConnection(Connection con){
        if(con!=null){
            try {
                con.close();
                logger.info("Closing DB connection");
            } catch (SQLException e) {
                logger.error("Error while closing connection",e);
            }
        }
    }

    public void closePreparedStatement(PreparedStatement ps) {
        if(ps!=null){
            try {
                ps.close();
                logger.info("Closing Prepared Statement");
            } catch (SQLException e) {
                logger.error("Error while closing prepared statement",e);
            }
        }
    }

    public void closeResultSet(ResultSet rs) {
        if(rs!=null){
            try {
                rs.close();
                logger.info("Closing Result Set");
            } catch (SQLException e) {
                logger.error("Error while closing result set",e);
            }
        }
    }
}
