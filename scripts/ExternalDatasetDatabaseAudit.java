import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Read-only connectivity probe for the HNBLUE development database.
 * Credentials are read only from the runtime environment.
 */
public final class ExternalDatasetDatabaseAudit {
    private ExternalDatasetDatabaseAudit() {
    }

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:mysql://127.0.0.1:3306/hnblue_v2_dev_control"
                        + "?allowPublicKeyRetrieval=true&useSSL=true"
                        + "&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
        );
        String user = System.getenv().getOrDefault("DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("select database()")) {
                resultSet.next();
                System.out.println("DATABASE_READABLE=" + resultSet.getString(1));
            }
        }
    }
}
