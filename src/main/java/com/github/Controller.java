package com.github;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    public Label linkRocketLeagueLabel;
    public ListView<File> listView = new ListView<>();

    private String rocketLeaguePathPrefix = "";
    private final String rocketLeaguePathSuffix = "/TAGame/CookedPCConsole";
    private final String originalMapFilename = "/Labs_CirclePillars_P.upk";
    private final String moddedMapFilename = "/Labs_CirclePillars_P.upk.old";

    private String originalMapPath;
    private String moddedMapPath;

    private Alert error = new Alert(Alert.AlertType.ERROR);
    private Alert info = new Alert(Alert.AlertType.INFORMATION);
    private Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeAlerts();
        initializeApp();
    }

    public void initializeApp() {
        String os = System.getProperty("os.name").toLowerCase();

        if(os.indexOf("win") >= 0) {
            if (!isRocketLeagueOpened()) {
                return;
            }

            linkToRocketLeague(findRocketLeaguePath());
        } else {
            this.error.setContentText("You must be on Windows to use this application !");
            this.error.showAndWait();
            System.exit(1);
        }
    }

    public void initializeAlerts() {
        // Setting error alert box
        this.error.setTitle("Error");
        this.error.setHeaderText(null);

        // Setting information alert box
        this.info.setTitle("Information");
        this.info.setHeaderText(null);

        // Setting confirmation alert box
        this.confirm.setTitle("Success");
        this.confirm.setHeaderText(null);
    }

    public String findRocketLeaguePath() {
        try {
            // Get the path of the process through powershell
            Process p = Runtime.getRuntime().exec("powershell (Get-Process RocketLeague).Path");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), "Cp850"));
            p.getOutputStream().close();

            return input.readLine().replace("\\", "/");
        } catch (Exception err) {
            err.printStackTrace();
        }

        return "";
    }

    public boolean isRocketLeagueOpened() {
        try {
            String line;
            Process p = Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe /fo csv /nh");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (line.split(",")[0].equals("\"RocketLeague.exe\"")) {
                    return true;
                }
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    public void linkToRocketLeague(String path) {
        StringBuilder sb = new StringBuilder("");
        for (String splitPath : path.split("/")) {
            if (splitPath.equals("rocketleague")) {
                sb.append(splitPath);
                break;
            }
            sb.append(splitPath).append("/");
        }

        this.rocketLeaguePathPrefix = sb.toString();
        this.originalMapPath = rocketLeaguePathPrefix + rocketLeaguePathSuffix + originalMapFilename;
        this.moddedMapPath = rocketLeaguePathPrefix + rocketLeaguePathSuffix + moddedMapFilename;

        this.linkRocketLeagueLabel.setText("Linked !");
        this.linkRocketLeagueLabel.setTextFill(Color.web("#00FF00"));

        this.info.setContentText("Successfully Linked !");
        this.info.showAndWait();
    }

    //TODO : Ability to save the path to workshop map
    public void rocketLeagueDirectoryChooser() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(null);

        if (f == null) {
            return;
        }

        // Clear the listView
        this.listView.getItems().clear();
        try {
            // Find workshop maps into the folder : it is recursive : 10 directories max
            List<Path> paths = Files.find(f.toPath(), 10, (filePath, fileAttr) -> fileAttr.isRegularFile()).filter(path -> path.toAbsolutePath().toString().endsWith(".upk") || path.toAbsolutePath().toString().endsWith(".udk")).collect(Collectors.toList());
            if (paths.isEmpty()) {
                this.error.setContentText("The directory you provided does not contain any workshop map !");
                this.error.showAndWait();
            } else {
                // Add file paths to the listView
                paths.forEach(p -> {
                    File file = new File(p.toString());
                    this.listView.getItems().add(file.getAbsoluteFile());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void injectMap(ActionEvent actionEvent) throws IOException {
        if (this.rocketLeaguePathPrefix.isEmpty() && !isRocketLeagueOpened()) {
            this.error.setContentText("Link to Rocket League before injecting a map !");
            this.error.showAndWait();
            return;
        } else if (this.listView.getSelectionModel().getSelectedItem() == null){
            this.error.setContentText("Select a map before injecting !");
            this.error.showAndWait();
            return;
        }

        // If there is already a .old file in the directory
        if (isAlreadyInjected(this.moddedMapPath)) {
            // Restore the original map first
            reverseMap(actionEvent);
        }

        // Rename the file into xxx.upk.old
        File originalFile = new File(originalMapPath);
        if (originalFile.exists()) originalFile.renameTo(new File(this.moddedMapPath));

        // Copy the workshop map into the game folder
        Files.copy(this.listView.getSelectionModel().getSelectedItem().toPath(), originalFile.toPath().toAbsolutePath(), StandardCopyOption.COPY_ATTRIBUTES);

        this.info.setContentText("Successfully injected !");
        this.info.showAndWait();
    }

    public boolean isAlreadyInjected(String trainingMapPath) {
        return new File(trainingMapPath).exists();
    }

    public void reverseMap(ActionEvent actionEvent) throws IOException {
        String idButton = ((Button) actionEvent.getSource()).getId();

        if (isRocketLeagueOpened()) {
            this.error.setContentText("Need to link to Rocket League first !");
            this.error.showAndWait();
            return;
        } else if (!isAlreadyInjected(moddedMapPath)) {
            this.error.setContentText("You need to inject first before restoring the original map !");
            this.error.showAndWait();
            return;
        }

        File workshopMapFile = new File(this.originalMapPath);
        File originalMapFile = new File(this.moddedMapPath);

        if (workshopMapFile.exists()) Files.delete(workshopMapFile.toPath());
        originalMapFile.renameTo(new File(this.originalMapPath));


        if (idButton.equals("reverseMapButton")) {
            this.info.setContentText("Successfully restored the original map !");
            this.info.showAndWait();
        }
    }

}
