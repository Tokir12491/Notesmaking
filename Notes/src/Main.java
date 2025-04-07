import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Main extends Application {

    private final File notesDir = new File("notes");
    private final ListView<String> notesList = new ListView<>();
    private final TextArea noteArea = new TextArea();
    private final HashMap<String, String> summaryToFileMap = new HashMap<>();
    private String selectedSummary = null;

    @Override
    public void start(Stage primaryStage) {
        if (!notesDir.exists()) notesDir.mkdir();

        // TextArea setup
        noteArea.setPromptText("Write your notes here...");
        noteArea.setWrapText(true);
        noteArea.setPrefHeight(200);

        // Buttons
        Button submitButton = new Button("Submit");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");

        submitButton.setOnAction(e -> saveNewNote());
        updateButton.setOnAction(e -> updateSelectedNote());
        deleteButton.setOnAction(e -> deleteSelectedNote());

        // Notes List
        notesList.setPrefHeight(150);
        notesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedSummary = newVal;
            if (newVal != null) {
                String filename = summaryToFileMap.get(newVal);
                loadNoteContent(filename);
            }
        });

        loadSavedNotes();

        // Layouts
        HBox buttonBar = new HBox(10, submitButton, updateButton, deleteButton);
        buttonBar.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, noteArea, buttonBar, new Label("Saved Notes:"), notesList);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-font-size: 14px;");

        // Scene
        Scene scene = new Scene(root, 500, 500);
        primaryStage.setTitle("User Notes App");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void saveNewNote() {
        String text = noteArea.getText();
        if (text.isBlank()) return;

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "note_" + timestamp + ".txt";
            File file = new File(notesDir, filename);
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();

            noteArea.clear();
            loadSavedNotes(); // reload list with new entry
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadSavedNotes() {
        notesList.getItems().clear();
        summaryToFileMap.clear();

        File[] files = notesDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) return;

        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                String summary = createSummary(content);
                summaryToFileMap.put(summary, file.getName());
                notesList.getItems().add(summary);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String createSummary(String content) {
        String[] lines = content.split("\n");
        String firstLine = lines.length > 0 ? lines[0] : "";
        String secondLine = lines.length > 1 ? lines[1] : "";
        String combined = (firstLine + " " + secondLine).trim().replaceAll("\\s+", " ");

        // Safely take up to 50 characters
        return combined.length() > 50 ? combined.substring(0, 50) : combined;
    }


    private void loadNoteContent(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(notesDir.getPath(), filename)));
            noteArea.setText(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updateSelectedNote() {
        if (selectedSummary == null) return;

        String filename = summaryToFileMap.get(selectedSummary);
        try {
            FileWriter writer = new FileWriter(new File(notesDir, filename));
            writer.write(noteArea.getText());
            writer.close();
            loadSavedNotes(); // refresh summary text
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteSelectedNote() {
        if (selectedSummary == null) return;

        String filename = summaryToFileMap.get(selectedSummary);
        File file = new File(notesDir, filename);
        if (file.delete()) {
            noteArea.clear();
            selectedSummary = null;
            loadSavedNotes();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
