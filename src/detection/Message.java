package detection;

import java.time.LocalDateTime;

public class Message implements Comparable {
	public String Section;
	public String Subsection;
	public String Topic;
	//public int id;
	public String UserName;
	public String MessageText;
	public LocalDateTime Time;
	
	public Message() {
		UserName = "Default User";
		MessageText = "Placeholder";
		Time = LocalDateTime.parse("1970-01-01");
		Topic = "Defaul Topic";
		Section = "Defaul Section";
		Subsection = "Defaul Subsection";
	}
	
	public Message(String _Section, String _Topic, String _UserName, String _MessageText, String _Time) {
		UserName = _UserName;
		MessageText = _MessageText;
		Topic = _Topic;
		Section = _Section;
		Time = LocalDateTime.parse(_Time);
	}
	
	public String toString() {
		String text = "";
		text += "Section: " + Section + "; ";
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
