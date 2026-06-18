module com.safebox {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires org.xerial.sqlitejdbc;

    opens com.safebox to javafx.graphics;
    opens com.safebox.controller to javafx.fxml;
    opens com.safebox.util to javafx.fxml;

    exports com.safebox;
}
