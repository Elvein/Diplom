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
import java.util.Set;

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
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    
    private ObservableList<String> usernames = FXCollections.observableArrayList();
    private ObservableList<String> levels = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> userEmotions = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> letterEmotions = FXCollections.observableArrayList();
    private ObservableList<PieChart.Data> forumTop = FXCollections.observableArrayList();

    private List<Message> forumMessages;
    private List<Letter> Letter;
    private Map<String,double[]> usersData;
    private Map<String,double[]> lettersData;
    private Map<String,String[]> forums;
    private Map<String,String[]> topics;
    private Map<LocalDate, double[]> statisticByDays;
    private Map<LocalDate, double[]> statisticByLetters;
    private Map<LocalDate, Integer> activityStatistic;
    
    private Series graphDataNeg; 
    private Series graphDataNeu; 
    private Series graphDataPos; 
    private Series letterDataNeg; 
    private Series letterDataNeu; 
    private Series letterDataPos; 
    private Series activityData;
    
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    private String username;
    private int level;
    private int topCount;
    
    private boolean translate = true;
    
    private String textEmotion;
    
    public MainApp() throws Exception {
    	detection.init();
    	
    	forumMessages = detecEasy.ParseForum();
    	endDate = forumMessages.get(forumMessages.size()-1).Time.toLocalDate();
    	startDate = forumMessages.get(0).Time.toLocalDate();
    	Letter = detecEasy.ParseLetter(forumMessages, startDate, endDate);
    	topics = detecEasy.ParseForumTopic();
    	forums = detecEasy.ParseForumForum();
    	username = "";
    	level = -1;
    	topCount = 10;
    	graphDataNeg = new Series<>(); 
    	graphDataNeu = new Series<>(); 
    	graphDataPos = new Series<>(); 
    	graphDataNeg.setName("Negative");
    	graphDataNeu.setName("Neutral");
		graphDataPos.setName("Positive");
		
    	letterDataNeg = new Series<>(); 
    	letterDataNeu = new Series<>(); 
    	letterDataPos = new Series<>(); 
		letterDataNeg.setName("Negative");
		letterDataNeu.setName("Neutral");
		letterDataPos.setName("Positive");
		
		activityData = new Series<>(); 
		activityData.setName("Кол-во сообщений в день");
    	
    	Update();
    	translate = false;
    	
    	usernames.addAll(usersData.keySet());
    	levels.addAll(detecEasy.GetLevels(forums));
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
    
    public void UpdateTopStatistic() {
    	forumTop.clear();
    	Map<String,Integer> forumMessageTopStatistic = detecEasy.TopForum(level, forumMessages, topics, forums, startDate, endDate, username);
    	int count = forumMessageTopStatistic.size();
    	Object[] keys = forumMessageTopStatistic.keySet().toArray();
    	for (int i = count - 1; i > count - 1 - topCount && i >= 0; i--) {
    		forumTop.add(new PieChart.Data(String.valueOf(keys[i]), forumMessageTopStatistic.get(keys[i]).intValue()));
    		
    	}
    }
    
    public void Update () throws Exception {
    	userEmotions.clear();
    	letterEmotions.clear();
    	graphDataNeg.getData().clear();
    	graphDataNeu.getData().clear();
    	graphDataPos.getData().clear();
    	letterDataNeg.getData().clear();
    	letterDataNeu.getData().clear();
    	letterDataPos.getData().clear();
    	activityData.getData().clear();
    	
    	usersData = detecEasy.AnalyzeForum(forumMessages, startDate, endDate, translate);
    	lettersData = detecEasy.TranslateLetters(Letter, startDate, endDate);
    	statisticByDays = detecEasy.ForumStatisticByDays(forumMessages, startDate, endDate, username);
    	statisticByLetters = detecEasy.LetterStatisticByDays(Letter, startDate, endDate, username);
    	activityStatistic = detecEasy.ForumActivityStatistic(forumMessages, startDate, endDate, username);

    	double[] allEmo = new double[4];
    	if (username.equals("")) {
	    	for(String user : usersData.keySet()) {
	    		double[] userEmo = usersData.get(user);
	     		allEmo[0] += userEmo[0];
	     		allEmo[1] += userEmo[1];
	     		allEmo[2] += userEmo[2];
	     		allEmo[3] += userEmo[3];
	        }
	    	if (allEmo[3] == 0)
     			allEmo[3] = 1;
	    	for (int i = 0; i<3; i++)
	    		allEmo[i] /= allEmo[3];
    	}
    	else {
    		if (usersData.containsKey(username)) {
    			double[] userEmo = usersData.get(username);
	     		allEmo[0] += userEmo[0];
	     		allEmo[1] += userEmo[1];
	     		allEmo[2] += userEmo[2];
	     		allEmo[3] += userEmo[3];
	     		if (allEmo[3] == 0)
	     			allEmo[3] = 1;
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
    		double[] stat = statisticByDays.get(day);
    		graphDataNeg.getData().add(new Data(day.toString(), stat[0]));   
    		graphDataNeu.getData().add(new Data(day.toString(), stat[1]));   
    		graphDataPos.getData().add(new Data(day.toString(), stat[2]));    		
    	}
    	
    	double[] allEmoLetter = new double[4];
    	if (username.equals("")) {
	    	for(String user : lettersData.keySet()) {
	    		double[] userEmo = lettersData.get(user);
	    		allEmoLetter[0] += userEmo[0];
	    		allEmoLetter[1] += userEmo[1];
	    		allEmoLetter[2] += userEmo[2];
	    		allEmoLetter[3] += userEmo[3];
	        }
	    	if (allEmoLetter[3] == 0)
	    		allEmoLetter[3] = 1;
	    	for (int i = 0; i<3; i++)
	    		allEmoLetter[i] /= allEmoLetter[3];
    	}
    	else {
    		if (lettersData.containsKey(username)) {
    			double[] userEmo = lettersData.get(username);
    			allEmoLetter[0] += userEmo[0];
    			allEmoLetter[1] += userEmo[1];
    			allEmoLetter[2] += userEmo[2];
    			allEmoLetter[3] += userEmo[3];
	     		if (allEmoLetter[3] == 0)
	     			allEmoLetter[3] = 1;
		    	for (int i = 0; i<3; i++) {
		    		allEmoLetter[i] /= allEmoLetter[3];
		    	}
    		}
    	} 
    	
    	days = new ArrayList<LocalDate>(activityStatistic.keySet());
    	Collections.sort(days);
    	for (LocalDate day : days) {
    		activityData.getData().add(new Data<Object,Object>(day.toString(), activityStatistic.get(day)));	
    	}
    	
    	days = new ArrayList<LocalDate>(statisticByLetters.keySet());
    	Collections.sort(days);
    	for (LocalDate day : days) {
    		double[] stat = statisticByLetters.get(day);
    		
    		letterDataNeg.getData().add(new Data(day.toString(), stat[0]));   
    		letterDataNeu.getData().add(new Data(day.toString(), stat[1]));   
    		letterDataPos.getData().add(new Data(day.toString(), stat[2]));    		
    	}
    	
    	letterEmotions.add(new PieChart.Data("Negative", allEmoLetter[0]));
    	letterEmotions.add(new PieChart.Data("Neutral", allEmoLetter[1]));
    	letterEmotions.add(new PieChart.Data("Positive", allEmoLetter[2]));
    	
    	UpdateTopStatistic();
    }
    
    public Series getGraphic(int n) throws FileNotFoundException {
    	if (n == 0)
    		return graphDataNeg;
    	else if (n == 1)
    		return graphDataNeu;
    	return graphDataPos;
    }
    
    public Series getGraphicLetter(int n) throws FileNotFoundException {
    	if (n == 0)
    		return letterDataNeg;
    	else if (n == 1)
    		return letterDataNeu;
    	return letterDataPos;
    }
    
    public ObservableList<PieChart.Data> getForumStatistic() throws FileNotFoundException {
    	return forumTop;
    }
    
    public Series getActivityGraphic() {
    	return activityData;
    }
    
    public ObservableList<String> getUsers() {
    	return usernames;
    }
   
    public ObservableList<String> getLevel() {
    	return levels;
    }
    
    public ObservableList<PieChart.Data> getUserStatistic() {
    	return userEmotions;
    }
    
    public ObservableList<PieChart.Data> getLetterStatistic() {
    	return letterEmotions;
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
    
    public void setLevel(int level) throws Exception {
    	this.level = level;
    	UpdateTopStatistic();
    }
    
    public void setTopCount(double val) throws Exception {
    	topCount = (int)val;
    	UpdateTopStatistic();
    }
    
    /* Для интерфейса определения окраски */
    
    public List<Sentence> getSentencesFromText(String text) {
    	return detection.getTextSentences(text);
	}
    
    public int[] getSentencesEmotions(String text) throws Exception {
    	String translation = text;
    	if (text.length() < 5)
    		return new int[]{0,0,0,0};
    	ArrayList<Language> languages = detection.detectLangs(text);
		if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.8) {
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