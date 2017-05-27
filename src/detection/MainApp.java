package detection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cybozu.labs.langdetect.Language;

import detection.view.InterfaceController;
import detection.view.RootController;
import edu.stanford.nlp.simple.Sentence;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    
    private ObservableList<String> usernames = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> userEmotions = FXCollections.observableArrayList();

    private List<Message> forumMessages;
    private Map<String,float[]> usersData;
    private Map<LocalDate,float[]> statisticByDays;
    private Series graphDataNeg; 
    private Series graphDataNeu; 
    private Series graphDataPos; 
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    private String username;
    
    private boolean translate = true;
    
    private String textEmotion;
    
    public MainApp() throws Exception {
    	detection.init();
    	
    	forumMessages = detection.ParseForum();
    	endDate = forumMessages.get(forumMessages.size()-1).Time.toLocalDate();
    	startDate = forumMessages.get(0).Time.toLocalDate();
    	username = "";
    	graphDataNeg = new Series<>(); 
    	graphDataNeu = new Series<>(); 
    	graphDataPos = new Series<>(); 
    	graphDataNeg.setName("Negative");
    	graphDataNeu.setName("Neutral");
		graphDataPos.setName("Positive");
    	
    	Update();
    	translate = false;
    	
    	usernames.addAll(usersData.keySet());
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Эмоциональная окраска");
        initRootLayout();
        showPersonOverview();
    }

    /**
     * Инициализирует корневой макет.
     */
    public void initRootLayout() {
        try {
            // Загружаем корневой макет из fxml файла.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();
            
            RootController controller = loader.getController();
            controller.setMainApp(this);

            // Отображаем сцену, содержащую корневой макет.
            Scene scene = new Scene(rootLayout);
            scene.getStylesheets().add("f.css");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Показывает в корневом макете сведения об адресатах.
     */
    public void showPersonOverview() {
        try {
            // Загружаем сведения об адресатах.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Interface.fxml"));
            AnchorPane personOverview = (AnchorPane) loader.load();

            // Помещаем сведения об адресатах в центр корневого макета.
            rootLayout.setCenter(personOverview);
            
            InterfaceController controller = loader.getController();
            controller.setMainApp(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Series getGraphic(int n) throws FileNotFoundException {
    	if (n == 0)
    		return graphDataNeg;
    	else if (n == 1)
    		return graphDataNeu;
    	return graphDataPos;
    }
    
    public void Update () throws Exception {
    	userEmotions.clear();
    	graphDataNeg.getData().clear();
    	graphDataNeu.getData().clear();
    	graphDataPos.getData().clear();
    	
    	usersData = detection.AnalyzeForum(forumMessages, startDate, endDate, translate);
    	
    	statisticByDays = detection.ForumStatisticByDays(forumMessages, startDate, endDate, username);

    	float[] allEmo = new float[4];
    	if (username.equals("")) {
	    	for(String user : usersData.keySet()) {
	         	float[] userEmo = usersData.get(user);
	     		allEmo[0] += userEmo[0];
	     		allEmo[1] += userEmo[1];
	     		allEmo[2] += userEmo[2];
	     		allEmo[3] += userEmo[3];
	        }
	    	for (int i = 0; i<3; i++) {
	    		allEmo[i] /= allEmo[3];
	    	}
    	}
    	else {
    		if (usersData.containsKey(username)) {
    			float[] userEmo = usersData.get(username);
	     		allEmo[0] += userEmo[0];
	     		allEmo[1] += userEmo[1];
	     		allEmo[2] += userEmo[2];
	     		allEmo[3] += userEmo[3];
		    	for (int i = 0; i<3; i++) {
		    		allEmo[i] /= allEmo[3];
		    	}
    		}
    	} 
    		
    	userEmotions.add(new PieChart.Data("Negative", allEmo[0]));
    	userEmotions.add(new PieChart.Data("Neutral", allEmo[1]));
    	userEmotions.add(new PieChart.Data("Positive", allEmo[2]));
    	
    	List<LocalDate> days = new ArrayList<LocalDate>(statisticByDays.keySet());
    	Collections.sort(days);
    	for (LocalDate day : days) {
    		graphDataNeg.getData().add(new Data<>(day.toString(), statisticByDays.get(day)[0]));   
    		graphDataNeu.getData().add(new Data<>(day.toString(), statisticByDays.get(day)[1]));   
    		graphDataPos.getData().add(new Data<>(day.toString(), statisticByDays.get(day)[2]));    		
    	}   	
    }
    
    public ObservableList<String> getUsers() {
    	return usernames;
    }
   
    
    public ObservableList<PieChart.Data> getUserStatistic() {
    	return userEmotions;
    }
    
    public LocalDate getEndDate() {
    	return endDate;
    }
    
    public LocalDate getStartDate() {
    	return startDate;
    }
    
    public void setEndDate(LocalDate _endDate) throws Exception {
    	endDate = _endDate;
    	Update();
    }
    
    public void setStartDate(LocalDate _startDate) throws Exception {
    	startDate = _startDate;
    	Update();
    }
    
    public void setUsername(String _username) throws Exception {
    	if (_username == null)
    		_username = "";
    	username = _username;
    	Update();
    }
    
    /* Для интерфейса определения окраски */
    
    public List<Sentence> getSentencesFromText(String text) {
    	return detection.getTextSentences(text);
	}
    
    public int[] getSentencesEmotions(String text) throws Exception {
    	String translation = "";
    	ArrayList<Language> languages = detection.detectLangs(text);
		if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.98) {
			translation = detection.sendMessage(text); // выбираем само сообщение
			translation = detection.ParseAndPrintTranslate(translation);
		}
    	return detection.StanfordEmotion(translation);
    }

    /**
     * Возвращает главную сцену.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}