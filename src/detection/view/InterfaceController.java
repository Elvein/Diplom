package detection.view;

import javafx.event.EventHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.LineChart.SortingPolicy;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;

import java.io.FileNotFoundException;

import detection.MainApp;

public class InterfaceController {
	@FXML
	private ComboBox<String> usernames;
	@FXML
	private ComboBox<String> usernamesTopic;
	@FXML
	private PieChart diagramm;
	@FXML
	private DatePicker startDate;
	@FXML
	private DatePicker endDate;
	@FXML
	private LineChart graphic;
	@FXML
	private LineChart activityGraphic;
	@FXML
	private PieChart diagrammLetter;
	@FXML
	private LineChart graphicLetter;
	@FXML
	private Slider slider;
	@FXML
	private PieChart diagrammTop;
	@FXML
	private CheckBox negativeChoice;
	@FXML
	private CheckBox neutralChoice;
	@FXML
	private CheckBox positiveChoice;
	@FXML
	private RadioButton emotionalDinamyc;
	@FXML
	private RadioButton loyalityIndex;
	@FXML
	private RadioButton reputationIndex;
	
	private MainApp mainApp;
	
	public InterfaceController() {
		
	}
	
	@FXML
	public void initialize() {
		
	}
	
	public void setMainApp(MainApp mainApp) throws FileNotFoundException {
        this.mainApp = mainApp;

        // Добавление в интерфейс данных из наблюдаемого списка
        usernames.setItems(mainApp.getUsers());
        new AutoCompleteComboBoxListener<String>(usernames);
        usernamesTopic.setItems(mainApp.getLevel());
        new AutoCompleteComboBoxListener<String>(usernamesTopic);
        diagramm.setData(mainApp.getUserStatistic());
        startDate.setValue(mainApp.getStartDate());
        endDate.setValue(mainApp.getEndDate());
        graphic.getData().addAll(mainApp.getGraphic(0), mainApp.getGraphic(1), mainApp.getGraphic(2));
        graphicLetter.getData().addAll(mainApp.getGraphicLetter(0), mainApp.getGraphicLetter(1), mainApp.getGraphicLetter(2));
        
        diagrammLetter.setData(mainApp.getLetterStatistic());
    	diagrammTop.setData(mainApp.getForumStatistic());
    	diagrammTop.getStylesheets().add("a.css");
    	
        activityGraphic.getData().add(mainApp.getActivityGraphic());
        
        slider.valueProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) {
            	try {
					mainApp.setTopCount(slider.getValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        });
    }
	
	public final void getOnActionChoiceNegative() throws Exception {
		mainApp.setEmotionsToTopics(negativeChoice.isSelected(), 0);
	}
	
	public final void getOnActionChoiceNeutral() throws Exception {
		mainApp.setEmotionsToTopics(neutralChoice.isSelected(), 1);
	}
	
	public final void getOnActionChoicePositive() throws Exception {
		mainApp.setEmotionsToTopics(positiveChoice.isSelected(), 2);
	}
	
	public final void getOnActionEndDate() throws Exception {
		mainApp.setEndDate(endDate.getValue());
	}
	
	public final void getOnActionStartDate() throws Exception {
		mainApp.setStartDate(startDate.getValue());
	}
	
	public final void getOnActionUsername() throws Exception {
		mainApp.setUsername(usernames.getValue());
	}
	
	public final void getOnActionComboBox() throws Exception {
		int index = usernamesTopic.getSelectionModel().getSelectedIndex();
		if (index == usernamesTopic.getItems().size() - 1)
			mainApp.setLevel(-1);
		else
			mainApp.setLevel(index);
	}
	
	public final void selectRadioButton() throws Exception {
		int code = emotionalDinamyc.isSelected() ? 0 : (loyalityIndex.isSelected() ? 1 : 2);
		mainApp.changeGraphicType(code);
	}
}
