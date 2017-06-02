package detection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Letter implements Comparable {
	public String UserName;
	public String MessageText;
	public String id;
	public LocalDateTime Time;
	public double[] Emotions = {0.0, 0.0, 0.0, 0.0};
	
	public Letter() {
		id = "";
		UserName = "Default User";
		MessageText = "Placeholder";
		Time = LocalDateTime.parse("1970-01-01");
	}
	
	public Letter(String _id, String _UserName, String _MessageText, String _Time, String[] emo) {
		id = _id;
		UserName = _UserName;
		MessageText = _MessageText;
		Time = LocalDateTime.of(LocalDate.parse(_Time), LocalTime.NOON);
		addEmotions(emo);
	}
	
	public Letter(String _id, String _UserName, String _MessageText, String _Time) {
		id = _id;
		UserName = _UserName;
		MessageText = _MessageText;
		Time = LocalDateTime.of(LocalDate.parse(_Time), LocalTime.NOON);
	}
	
	public void addEmotions(String[] Emo) {
		Emotions[0] = Double.valueOf(Emo[0]);
		Emotions[1] = Double.valueOf(Emo[1]);
		Emotions[2] = Double.valueOf(Emo[2]);
		Emotions[3] = Double.valueOf(Emo[3]);
	}
	
	
	public String toString() {
		String text = "";
		text += "UserName: " + UserName + "; ";
		text += "MessageText: " + MessageText + "; ";
		text += "Date: " + Time.toString();
		return text;
	}

	@Override
	public int compareTo(Object arg0) {
		return this.Time.compareTo(((Letter)arg0).Time);
	}
}
