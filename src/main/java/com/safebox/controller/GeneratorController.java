package com.safebox.controller;

import com.safebox.MainApp;
import com.safebox.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Password generator dialog controller.
 */
public class GeneratorController {

    @FXML
    private Label lengthLabel;
    @FXML
    private Slider lengthSlider;
    @FXML
    private TextField lengthField;
    @FXML
    private CheckBox upperCheck;
    @FXML
    private CheckBox lowerCheck;
    @FXML
    private CheckBox digitsCheck;
    @FXML
    private CheckBox specialCheck;
    @FXML
    private TextField resultField;
    @FXML
    private Button generateButton;
    @FXML
    private Button copyButton;
    @FXML
    private Button useButton;
    @FXML
    private Button closeButton;

    private MainApp app;
    private Consumer<String> onUse;

    /**
     * Initializes the generator dialog.
     *
     * @param app   main application
     * @param onUse callback when user accepts a generated password
     */
    public void init(MainApp app, Consumer<String> onUse) {
        this.app = app;
        this.onUse = onUse;
        lengthSlider.valueProperty().addListener((obs, o, n) ->
                lengthField.setText(String.valueOf(n.intValue())));
        lengthField.textProperty().addListener((obs, o, n) -> {
            try {
                int value = Integer.parseInt(n);
                if (value >= 8 && value <= 128) {
                    lengthSlider.setValue(value);
                }
            } catch (NumberFormatException ignored) {
                // user is typing
            }
        });
        applyLanguage();
        generate();
    }

    @FXML
    private void onGenerate() {
        generate();
    }

    @FXML
    private void onUse() {
        if (onUse != null && resultField.getText() != null && !resultField.getText().isBlank()) {
            onUse.accept(resultField.getText());
        }
        close();
    }

    @FXML
    private void onCopy() {
        app.getClipboardService().copy(resultField.getText());
    }

    @FXML
    private void onClose() {
        close();
    }

    private void applyLanguage() {
        lengthLabel.setText(I18n.get("generator.length"));
        upperCheck.setText(I18n.get("generator.upper"));
        lowerCheck.setText(I18n.get("generator.lower"));
        digitsCheck.setText(I18n.get("generator.digits"));
        specialCheck.setText(I18n.get("generator.special"));
        generateButton.setText(I18n.get("generator.generate"));
        copyButton.setText(I18n.get("generator.copy"));
        useButton.setText(I18n.get("generator.use"));
        closeButton.setText(I18n.get("generator.close"));
    }

    private void generate() {
        try {
            String password = app.getGeneratorService().generate(
                    (int) lengthSlider.getValue(),
                    upperCheck.isSelected(),
                    lowerCheck.isSelected(),
                    digitsCheck.isSelected(),
                    specialCheck.isSelected()
            );
            resultField.setText(password);
        } catch (IllegalArgumentException e) {
            resultField.setText("");
        }
    }

    private void close() {
        Stage stage = (Stage) resultField.getScene().getWindow();
        stage.close();
    }
}
