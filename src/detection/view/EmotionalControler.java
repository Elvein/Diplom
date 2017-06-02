package detection.view;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import detection.MainApp;
import edu.stanford.nlp.simple.Sentence;

public class EmotionalControler {
	@FXML
	private Button buttonSave;
	@FXML
	private Button buttonRun;
	@FXML
	private TextArea text;
	@FXML
	private TextFlow outText;
	@FXML
	private TextFlow textLegend;
	@FXML
	private TextFlow result;
	
	private MainApp mainApp;
	
	private Stage stage;
	
	public EmotionalControler() {
		
	}
	
	@FXML
	public void initialize() {
		buttonSave.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open Resource File");
				fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Text Files", "*.txt"));
				File selectedFile = fileChooser.showOpenDialog(stage);
				
				StringBuilder sb = new StringBuilder();
				if (selectedFile != null) {
					try(BufferedReader in = new BufferedReader(new FileReader(selectedFile.getAbsoluteFile())))
				    {
						try {
				            //В цикле построчно считываем файл
				            String s;
				            while ((s = in.readLine()) != null) {
				                sb.append(s);
				                sb.append("\n");
				            }
				        } finally {
				            //Также не забываем закрыть файл
				            in.close();
				        }
				    }
					catch(IOException ex){
						System.out.println(ex.getMessage());
					} 
					text.setText(sb.toString());
				}
			}
		});
		
		buttonRun.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				try {
					textProcessing();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
		});
		/*Добавление легенды*/
		Text negative = new Text("Negative ");
		Text neutral = new Text("| Neutral |");
		Text positive = new Text(" Positive ");
		negative.setFill(Color.RED);
		neutral.setFill(Color.GRAY);
		positive.setFill(Color.GREEN);
		textLegend.getChildren().addAll(negative, neutral, positive);
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	public void setMainApp(MainApp mainApp)  {
        this.mainApp = mainApp;
        
        // Добавление в интерфейс данных из наблюдаемого списка
    }
	
	public final void textProcessing() throws Exception {
		outText.getChildren().clear();
		outText.setVisible(false);
		int[] allEmo = new int[3];
		List<Sentence> sentences = mainApp.getSentencesFromText(text.getText());
		
		for (Sentence sentence : sentences) {
			int[] emotions = mainApp.getSentencesEmotions(sentence.text());
			Text sent = new Text(sentence.text());
			if (emotions[2] > emotions[1] && emotions[2] > emotions[0]) {
				sent.setFill(Color.GREEN);
			} else if (emotions[0] > emotions[1]) {
				sent.setFill(Color.RED);
			} else {
				sent.setFill(Color.GRAY);
			}

			allEmo[0] += emotions[0];
			allEmo[1] += emotions[1];
			allEmo[2] += emotions[2];
			outText.getChildren().add(sent);
		}
		outText.setVisible(true);
		
		result.getChildren().clear();
		Text textResult = new Text("Итоговый текст: ");
		result.setVisible(false);
		result.getChildren().add(textResult);
		if (allEmo[2] > allEmo[1] && allEmo[2] > allEmo[0]) {
			textResult = new Text("позитивный");
		} else if (allEmo[0] > allEmo[1]) {
			textResult = new Text("негативный");
		} else 
			textResult = new Text("нейтральный");
		result.getChildren().add(textResult);
		result.setVisible(true);
	}	
	
}
