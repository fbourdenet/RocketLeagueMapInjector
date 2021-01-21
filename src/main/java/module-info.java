module org.example {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.github to javafx.fxml;
    exports com.github;
}