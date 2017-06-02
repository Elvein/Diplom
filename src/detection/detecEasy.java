package detection;
import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;

import org.bson.Document;

import com.cybozu.labs.langdetect.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.opencsv.*;

public class detecEasy {
	private static PrintWriter outputLetter;
	private static String outLetter = "output.txt";
	
	public static void main(String[] args) throws Exception {
		long startTime,endTime,totalTime;
		List<Message> forumMessages = ParseForum();
		Map<String,double[]> usersData;
		Map<String,double[]> LetterData;
		
		LocalDate startDate;
	    LocalDate endDate;
		endDate = forumMessages.get(forumMessages.size()-1).Time.toLocalDate();
    	startDate = forumMessages.get(0).Time.toLocalDate();
    	
    	startTime = System.currentTimeMillis();	//
    	
		usersData = AnalyzeForum(forumMessages, startDate, endDate, true);

		endTime   = System.currentTimeMillis();	//
		totalTime = endTime - startTime;	//
		System.out.println("Emotion recognition time: " + totalTime);
		List<Letter> LetterMessages = ParseLetter(forumMessages, startDate, endDate);
		LetterData = TranslateLetters(LetterMessages, startDate, endDate);
	}
	
	public static String getRandomDate (LocalDate startDate, LocalDate endDate) {
		long start = startDate.toEpochDay();
		long end = endDate.toEpochDay();
		long randomEpochDay = ThreadLocalRandom.current().longs(start, end).findAny().getAsLong();
		String Time = LocalDate.ofEpochDay(randomEpochDay).toString();
		return Time;
	}
	
	public static List<Letter> ParseLetter(List<Message> msg, LocalDate startDate, LocalDate endDate) throws Exception {
		List<Letter> messages = new ArrayList<Letter>();
		String Text, Subject;
		
		Map<String,String[]> emotions = new HashMap<String,String[]>();
		CSVReader emoReader = new CSVReader(new FileReader("Media/emotionsLetter.csv"));
		String [] nextLine;
		while ((nextLine = emoReader.readNext()) != null) {
			String id = nextLine[0];
			String neg = nextLine[1];
			String neu = nextLine[2];
			String pos = nextLine[3];
			String all = nextLine[4];
			emotions.put(id, new String[]{neg, neu, pos, all});
		}
		emoReader.close();
		
		// подключение к базе
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("bragi_alt");
		MongoCollection<Document> collection = database.getCollection("messages");
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			Document nextCursor;
			while (cursor.hasNext()) {
		    	nextCursor = cursor.next();
		    	Text = nextCursor.getString("bodyText");
		    	Subject = nextCursor.getString("subject");
		    	String id = nextCursor.getString("messageId");
		    					    			    	
		    	if(!Text.isEmpty() && !Text.equals(" ") && !Subject.contains("Re:") && !Subject.contains("Fwd:") && !Subject.contains("Fw:") && !Text.contains("Re:") && !Text.contains(">>>")) {
		    		Random Random = new Random();
		    		int randomNumber = (int)Math.floor(msg.size() * Random.nextDouble());
		    		String user = msg.get(randomNumber).UserName;
		    		
		    		String[] emo = new String[]{"0.0", "0.0", "0.0", "0.0"};
			        if (emotions.containsKey(id)) {
			        	emo = emotions.get(id);
			        }
			        
		    		messages.add(new Letter(id, user, Text, getRandomDate(startDate, endDate), emo));
		    	}
		    	else 
		    		continue;
			}
		} finally {
			cursor.close();
		}
		Collections.sort(messages);  
	    return messages;
	}
	
	public static Map<String,double[]> TranslateLetters(List<Letter> msgLetter, LocalDate startDate, LocalDate endDate) throws Exception {
		Map<String,double[]> statistic = new HashMap<String,double[]>();
		
        for (int i = 0; i < msgLetter.size(); i++) {
			LocalDate msgTime = msgLetter.get(i).Time.toLocalDate();
			if(!(msgTime.isAfter(startDate) || msgTime.isEqual(startDate)) || !(msgTime.isBefore(endDate) || msgTime.isEqual(endDate)))
				continue;
			statistic.put(msgLetter.get(i).UserName, msgLetter.get(i).Emotions);
        }
        return statistic;
	}
		
	public static Map<LocalDate, double[]> LetterStatisticByDays(List<Letter> messages, LocalDate minDate, LocalDate maxDate, String user) throws FileNotFoundException {
		Map<LocalDate,double[]> statistic = new HashMap<LocalDate,double[]>();
		
    	for (Letter msg : messages) {
    		LocalDate curDate = msg.Time.toLocalDate();
    		if (curDate.isAfter(maxDate))
    			break;
    		else if (curDate.isBefore(minDate)) 
    			continue;
    		if (!user.equals(msg.UserName) && !user.isEmpty()) 
    			continue;
    		
    		if (statistic.containsKey(curDate)) {
        		double[] allEmo = statistic.get(curDate);
        		allEmo[0] += msg.Emotions[0];
        		allEmo[1] += msg.Emotions[1];
        		allEmo[2] += msg.Emotions[2];
        		allEmo[3] += msg.Emotions[0] + msg.Emotions[1] + msg.Emotions[2];
        		statistic.replace(curDate, allEmo);
        	} else {
        		double[] allEmo = new double[4];
        		allEmo[0] = msg.Emotions[0];
        		allEmo[1] = msg.Emotions[1];
        		allEmo[2] = msg.Emotions[2];
        		allEmo[3] = msg.Emotions[0] + msg.Emotions[1] + msg.Emotions[2];
        		statistic.put(curDate, allEmo);
        	}
        		
    	}
    	for(LocalDate curDate : statistic.keySet()) {
        	double[] allEmo = statistic.get(curDate);
        	if (allEmo[3] == 0)
        		allEmo[3] = 1;
    		allEmo[0] /= allEmo[3];
    		allEmo[1] /= allEmo[3];
    		allEmo[2] /= allEmo[3];
    		statistic.replace(curDate, allEmo); 
    	}
    	return statistic;
	}
	
	public static Map<String,double[]> AnalyzeForum(List<Message> msg, LocalDate startDate, LocalDate endDate, boolean translate) throws Exception {
		Map<String,double[]> statistic = new HashMap<String,double[]>();
        for (int i = 0; i < msg.size(); i++) {
						
			LocalDate msgTime = msg.get(i).Time.toLocalDate();
			if(!(msgTime.isAfter(startDate) || msgTime.isEqual(startDate)) || !(msgTime.isBefore(endDate) || msgTime.isEqual(endDate)))
				continue;
			statistic.put(msg.get(i).UserName, msg.get(i).Emotions);
        }
        return statistic;
	}
	
	public static Map<LocalDate, double[]> ForumStatisticByDays(List<Message> messages, LocalDate minDate, LocalDate maxDate, String user) throws FileNotFoundException {
		Map<LocalDate,double[]> statistic = new HashMap<LocalDate,double[]>();
		
    	for (Message msg : messages) {
    		LocalDate curDate = msg.Time.toLocalDate();
    		if (curDate.isAfter(maxDate))
    			break;
    		else if (curDate.isBefore(minDate)) 
    			continue;
    		if (!user.equals(msg.UserName) && !user.isEmpty()) 
    			continue;
    		
    		if (statistic.containsKey(curDate)) {
        		double[] allEmo = statistic.get(curDate);
        		allEmo[0] += msg.Emotions[0];
        		allEmo[1] += msg.Emotions[1];
        		allEmo[2] += msg.Emotions[2];
        		allEmo[3] += msg.Emotions[0] + msg.Emotions[1] + msg.Emotions[2];
        		statistic.replace(curDate, allEmo);
        	} else {
        		double[] allEmo = new double[4];
        		allEmo[0] = msg.Emotions[0];
        		allEmo[1] = msg.Emotions[1];
        		allEmo[2] = msg.Emotions[2];
        		allEmo[3] = msg.Emotions[0] + msg.Emotions[1] + msg.Emotions[2];
        		statistic.put(curDate, allEmo);
        	}
    	}
    	for(LocalDate curDate : statistic.keySet()) {
        	double[] allEmo = statistic.get(curDate);
        	if (allEmo[3] == 0)
    			allEmo[3] = 1;
    		allEmo[0] /= allEmo[3];
    		allEmo[1] /= allEmo[3];
    		allEmo[2] /= allEmo[3];
    		statistic.replace(curDate, allEmo); 
    	}
    	return statistic;
	}
	
	public static Map<LocalDate, Integer> ForumActivityStatistic(List<Message> messages, LocalDate minDate, LocalDate maxDate, String user) throws FileNotFoundException {
		Map<LocalDate,Integer> statistic = new HashMap<LocalDate,Integer>();
		
    	for (Message msg : messages) {
    		LocalDate curDate = msg.Time.toLocalDate();
    		if (curDate.isAfter(maxDate))
    			break;
    		else if (curDate.isBefore(minDate)) 
    			continue;
    		if (!user.equals(msg.UserName) && !user.isEmpty()) 
    			continue;
    		
    		if (statistic.containsKey(curDate)) {
        		int mesCount = statistic.get(curDate);
        		mesCount ++;
        		statistic.replace(curDate, mesCount);
        	} else 
        		statistic.put(curDate, 1);
    	}
    	return statistic;
	}
	
	public static List<Message> ParseForum() throws Exception {
		List<Message> messages = new ArrayList<Message>();
		Map<String,String[]> emotions = new HashMap<String,String[]>();
		CSVReader emoReader = new CSVReader(new FileReader("Media/emotionsForum.csv"));
		String [] nextLine;
		while ((nextLine = emoReader.readNext()) != null) {
			String id = nextLine[0];
			String neg = nextLine[1];
			String neu = nextLine[2];
			String pos = nextLine[3];
			String all = nextLine[4];
			emotions.put(id, new String[]{neg, neu, pos, all});
		}
		emoReader.close();
		
		CSVReader reader = new CSVReader(new FileReader("Media/forum-data/forum_post.csv"));
		nextLine = reader.readNext();
	    while ((nextLine = reader.readNext()) != null) {
	    	String id = nextLine[0];
	    	String date = nextLine[1];
	        String text = nextLine[3];
	        String user = nextLine[4];
	        String topic = nextLine[5];
	        
	        String[] emo = {"0.0", "0.0", "0.0", "0.0"};
	        if (emotions.containsKey(id)) {
	        	emo = emotions.get(id);
	        }
	        
	        messages.add(new Message(topic, 
	        		user.replaceAll("\"", ""), text.replaceAll("\"", ""), 
					date.replaceAll("\"", ""), id, emo));
	    }
	    Collections.sort(messages);
	    reader.close();	    
	    return messages;
	}
	
	public static Map<String,String[]> ParseForumTopic() throws Exception {
		Map<String,String[]> topic = new HashMap<String,String[]>();
		String [] nextLine;
		CSVReader reader = new CSVReader(new FileReader("Media/forum-data/forum_topic.csv"));
		nextLine = reader.readNext();
	    while ((nextLine = reader.readNext()) != null) {
	    	String id = nextLine[0];
	    	String name = nextLine[2];
	    	String forimId = nextLine[4];
	    	topic.put(id, new String[]{name, forimId});
	    }
	    reader.close();	    
	    return topic;
	}
	
	public static Map<String,String[]> ParseForumForum() throws Exception {
		Map<String,String[]> forum = new HashMap<String,String[]>();
		String [] nextLine;
		CSVReader reader = new CSVReader(new FileReader("Media/forum-data/forum_forum.csv"));
		nextLine = reader.readNext();
	    while ((nextLine = reader.readNext()) != null) {
	    	String id = nextLine[0];
	    	String name = nextLine[2];
	        String level = nextLine[7];
	        String parent = "";
	        if (!level.equals("0"))
	        	parent = nextLine[8];
	        forum.put(id, new String[]{name, level, parent});
	    }
	    reader.close();	    
	    return forum;
	}
	
	public static Map<String,Integer> TopForum(int level, List<Message> messages, Map<String,String[]> topics, Map<String,String[]> forums, LocalDate minDate, LocalDate maxDate, String user) {
		Map<String,Integer> statistic = new TreeMap<String,Integer>();
		
    	for (Message msg : messages) {
    		LocalDate curDate = msg.Time.toLocalDate();
    		if (curDate.isAfter(maxDate))
    			break;
    		else if (curDate.isBefore(minDate)) 
    			continue;
    		if (!user.equals(msg.UserName) && !user.isEmpty()) 
    			continue;
    		
    		String keyName = "";

    		if (!topics.containsKey(msg.Topic))
    			continue;
    		if (level == -1) { //This is theme
    			keyName = topics.get(msg.Topic)[0];
    		} else {
    			String[] curForum = forums.get(topics.get(msg.Topic)[1]);
    			int lvl = Integer.valueOf(curForum[1]);
    			
    			keyName = curForum[0];
    			while (lvl != level && lvl > 0) {
    				lvl = Integer.valueOf(forums.get(curForum[2])[1]);
    				if (!forums.containsKey(curForum[1]))
    					break;
    				curForum = forums.get(curForum[1]);
        			keyName = curForum[0];
    			}
    		}
    			
    		if (statistic.containsKey(keyName)) {
        		int mesCount = statistic.get(keyName);
        		mesCount ++;
        		statistic.replace(keyName, mesCount);
        	} else {
        		statistic.put(keyName, 1);
        	}
    	}
    	return statistic;
	}
	
	public static String[] GetLevels(Map<String,String[]> forums) {
		String[] levels;
		int max = 0;
    	for (String key : forums.keySet()) {
    		int level = Integer.valueOf(forums.get(key)[1]);
    		if (level > max)
    			max = level;
    	}
    	levels = new String[max + 2];
    	levels[0] = "Подфорум";
    	levels[max + 1] = "Тема";
    	for (int i = 1; i < max + 1; i ++) {
    		levels[i] = "Раздел " + i;
    	}
    	return levels;
	}
}
