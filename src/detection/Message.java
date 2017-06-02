package detection;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Comparable {
	public String Topic;
	public int idMsg;
	public String UserName;
	public String MessageText;
	public LocalDateTime Time;
	public double[] Emotions = {0.0, 0.0, 0.0, 0.0};
	
	public Message() {
		UserName = "Default User";
		MessageText = "Placeholder";
		Time = LocalDateTime.parse("1970-01-01");
		Topic = "Defaul Topic";
		idMsg = 0;
	}
	
	public Message(String _Topic, String _UserName, String _MessageText, String _Time, String _idMsg) {
		UserName = _UserName;
		MessageText = _MessageText;
		Topic = _Topic;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS ZZZZZ");
		Time = ZonedDateTime.parse(_Time, formatter).toLocalDateTime();
		idMsg = Integer.valueOf(_idMsg);
	}
	
	public Message(String _Topic, String _UserName, String _MessageText, String _Time, String _idMsg, String[] emo) {
		UserName = _UserName;
		MessageText = _MessageText;
		Topic = _Topic;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS ZZZZZ");
		Time = ZonedDateTime.parse(_Time, formatter).toLocalDateTime();
		idMsg = Integer.valueOf(_idMsg);
		addEmotions(emo);
	}
	
	public void addEmotions(String[] Emo) {
		Emotions[0] = Double.valueOf(Emo[0]);
		Emotions[1] = Double.valueOf(Emo[1]);
		Emotions[2] = Double.valueOf(Emo[2]);
		Emotions[3] = Double.valueOf(Emo[3]);
	}
		
	public String toString() {
		String text = "";
		text += "Topic: " + Topic + "; ";
		text += "UserName: " + UserName + "; ";
		text += "MessageText: " + MessageText + "; ";
		text += "Date: " + Time.toString();
		return text;
	}

	@Override
	public int compareTo(Object arg0) {
		return this.Time.compareTo(((Message)arg0).Time);
	}
}
