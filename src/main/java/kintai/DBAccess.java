package kintai;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * データベースへの接続を提供するクラス。
 */
public class DBAccess {

    /** データベースの接続情報 */
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost/kintai";
    private static final String DB_USER = "root";
    private static final String DB_PWD = "1234"; // 本番環境用パスワード

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        // 1. JDBCドライバをロード
        Class.forName(DB_DRIVER);
        
        // 2. データベースに接続し、その接続を直接返す
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
    }
}
