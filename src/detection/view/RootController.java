package detection.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import detection.MainApp;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RootController {
	@FXML
	private MenuItem emoButton;
	@FXML
	private MenuItem saveButton;
	
	private MainApp mainApp;
	
	private Stage stage;
	private AnchorPane layout;
	
	public RootController() {
		
	}
	
	@FXML
	public void initialize() {
		stage = null;
	}
	public void setMainApp(MainApp mainApp) throws FileNotFoundException {
        this.mainApp = mainApp;
        emoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	if (stage == null || !stage.isShowing()) {   		
	                stage = new Stage();
	                FXMLLoader loader = new FXMLLoader();
	                loader.setLocation(MainApp.class.getResource("view/emotional.fxml"));
	                
					try {
						layout = (AnchorPane) loader.load();
						
						EmotionalControler controller = loader.getController();
		                controller.setMainApp(mainApp);
		                controller.setStage(stage);
		                
						 // Отображаем сцену, содержащую корневой макет.
		                Scene scene = new Scene(layout);
		                stage.setScene(scene);
		                stage.show();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
            	}
            }
        });
        
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Сохранить таблицу");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("CSV-таблица", "*.csv")
                    );
                File file = fileChooser.showSaveDialog(stage);
                if (file != null) {
	            	try {
						mainApp.saveTopicsToFile(file);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                }
            }
        });
    }
}
