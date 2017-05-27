package detection.view;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.LineChart.SortingPolicy;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

import java.io.FileNotFoundException;

import detection.MainApp;

public class InterfaceController {
	@FXML
	private ComboBox<String> usernames;
	@FXML
	private PieChart diagramm;
	@FXML
	private DatePicker startDate;
	@FXML
	private DatePicker endDate;
	@FXML
	private LineChart graphic;
	
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
        diagramm.setData(mainApp.getUserStatistic());
        startDate.setValue(mainApp.getStartDate());
        endDate.setValue(mainApp.getEndDate());
        graphic.getData().add(mainApp.getGraphic(0));
        graphic.getData().add(mainApp.getGraphic(1));
        graphic.getData().add(mainApp.getGraphic(2));
        graphic.setAxisSortingPolicy(SortingPolicy.X_AXIS);
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
}
