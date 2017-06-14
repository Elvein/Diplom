package detection;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.bson.Document;

import com.cybozu.labs.langdetect.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.opencsv.*;

import synesketch.emotion.EmotionalState;

import com.google.gson.*;
import edu.stanford.nlp.simple.*;


public class detection {	
	public static void init() throws LangDetectException, IOException {
		init("profiles");
	}
	
	public static void main(String[] args) throws Exception {
		long startTime,endTime,totalTime;
		init();
		List<Message> forumMessages = ParseForum();
		Map<String,float[]> usersData;
		Map<String,float[]> LetterData;
		
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
		LetterData = TranslateLetters(LetterMessages, forumMessages, startDate, endDate, true);
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
		String Text, Subject, id;
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
		    	id = nextCursor.getString("messageId");
		    					    			    	
		    	if(!Text.isEmpty() && !Text.equals(" ") && !Subject.contains("Re:") && !Subject.contains("Fwd:") && !Subject.contains("Fw:") && !Text.contains("Re:") && !Text.contains(">>>")) {
		    		Random Random = new Random();
		    		int randomNumber = (int)Math.floor(msg.size() * Random.nextDouble());
		    		String user = msg.get(randomNumber).UserName;
		    		messages.add(new Letter(id, user, Text, getRandomDate(startDate, endDate)));
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
	
	public static Map<String,float[]> TranslateLetters(List<Letter> msgLetter, List<Message> msgForum, LocalDate startDate, LocalDate endDate, boolean translate) throws Exception {
		ArrayList<Language> languages;
		Map<String,float[]> statistic = new HashMap<String,float[]>();
		String text, translation = null;
		CSVWriter writer = new CSVWriter(new FileWriter("emotionsLetter.csv"));
        // feed in your array (or convert your data to an array)
		String symbols = "[^\\p{L}\\p{Z}]";//"[\\<\\>\\{\\}/\\*\\^“\\[\\]_░#]"; 
		
        for (int i = 0; i < msgLetter.size(); i++) {
        	System.out.println(msgLetter.size());
        	int[] messageEmo = {0, 0, 0, 0};
        	float[] allEmo = new float[4];
		
			text = msgLetter.get(i).MessageText;
			LocalDate msgTime = msgLetter.get(i).Time.toLocalDate();
			if(!(msgTime.isAfter(startDate) || msgTime.isEqual(startDate)) || !(msgTime.isBefore(endDate) || msgTime.isEqual(endDate)))
				continue;
			String[] splited_text = text.split("[.?!\\n。？\\t\\r]");
			
			for (String translateText : splited_text) {
				
				translateText = translateText.replaceAll(symbols, "");
				if (translateText.length() < 3) {
					continue;
				}
				try {
					// для определения языка
	    			languages = detectLangs(translateText);
					if (translate && (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.8)) {
						translation = sendMessage(translateText); // выбираем само сообщение
						translation = ParseAndPrintTranslate(translation);  // получаем перевод письма
						
						msgLetter.get(i).MessageText = translation;
	    				messageEmo = StanfordEmotion(translation);
	    			} else {
	    				messageEmo = StanfordEmotion(translateText);
	    			}
					allEmo[0] += messageEmo[0];
            		allEmo[1] += messageEmo[1];
            		allEmo[2] += messageEmo[2];
            		allEmo[3] += messageEmo[3];
				} catch (LangDetectException e) {
	    			System.out.println(e.toString());
	    		}
			}
				
			String []line = new String[5];
			line[0] = String.valueOf(msgLetter.get(i).id);
			line[1] = String.valueOf(allEmo[0]);
			line[2] = String.valueOf(allEmo[1]);
			line[3] = String.valueOf(allEmo[2]);
			line[4] = String.valueOf(allEmo[3]);
	        writer.writeNext(line);
        }
        writer.close();
        return statistic;
	}
		
	public static Map<String,float[]> AnalyzeForum(List<Message> msg, LocalDate startDate, LocalDate endDate, boolean translate) throws Exception {
		ArrayList<Language> languages;
		Map<String,float[]> statistic = new HashMap<String,float[]>();
		 CSVWriter writer = new CSVWriter(new FileWriter("emotionsForum.csv"));
	        // feed in your array (or convert your data to an array)
	    String symbols = "[^\\p{L}\\p{Z}]";//"[\\<\\>\\{\\}/\\*\\^“\\[\\]_░#]"; 
		String text, translation = null;
        for (int i = 0; i < msg.size(); i++) {
        	System.out.print("Iter: " + String.valueOf(i) + "\r");
        	int[] messageEmo = {0, 0, 0, 0};
        	float[] allEmo = new float[4];
			text = msg.get(i).MessageText.replaceAll("[░\\t]", "");
			
			LocalDate msgTime = msg.get(i).Time.toLocalDate();
			if(!(msgTime.isAfter(startDate) || msgTime.isEqual(startDate)) || !(msgTime.isBefore(endDate) || msgTime.isEqual(endDate)))
				continue;
			
			String[] splited_text = text.split("[.?!\\n。？]");
			
			for (String translateText : splited_text) {
				translateText = translateText.replaceAll(symbols, "");
				// для определения языка
				if (translateText.length() < 3) {
					continue;
				}
				try {
					languages = detectLangs(translateText);
					if (translate && (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.8)) {
						translation = sendMessage(translateText); // выбираем само сообщение
						translation = ParseAndPrintTranslate(translation);  // получаем перевод письма
				        msg.get(i).MessageText = translation;

						messageEmo = StanfordEmotion(translation);
					} else {
						messageEmo = StanfordEmotion(translateText);
					}
					allEmo[0] += messageEmo[0];
					allEmo[1] += messageEmo[1];
					allEmo[2] += messageEmo[2];
					allEmo[3] += messageEmo[3];
				} catch (LangDetectException e) {
					System.out.println(e);
				}
			}
			String []line = new String[5];
			line[0] = String.valueOf(msg.get(i).idMsg);
			line[1] = String.valueOf(allEmo[0]);
			line[2] = String.valueOf(allEmo[1]);
			line[3] = String.valueOf(allEmo[2]);
			line[4] = String.valueOf(allEmo[3]);
	        writer.writeNext(line);
        }
        writer.close();
        return statistic;
	}
	
	public static Map<LocalDate, float[]> ForumStatisticByDays(List<Message> messages, LocalDate minDate, LocalDate maxDate, String user) throws FileNotFoundException {
		Map<LocalDate,float[]> statistic = new HashMap<LocalDate,float[]>();
		String text;
        int[] messageEmo;
		
    	for (Message msg : messages) {
    		LocalDate curDate = msg.Time.toLocalDate();
    		if (curDate.isAfter(maxDate))
    			break;
    		else if (curDate.isBefore(minDate)) 
    			continue;
    		if (!user.equals(msg.UserName) && !user.isEmpty()) 
    			continue;
    		
    		text = msg.MessageText;
			messageEmo = StanfordEmotion(text);
			
			if (statistic.containsKey(curDate)) {
        		float[] allEmo = statistic.get(curDate);
        		allEmo[0] += messageEmo[0];
        		allEmo[1] += messageEmo[1];
        		allEmo[2] += messageEmo[2];
        		allEmo[3] += messageEmo[3];
        		statistic.replace(curDate, allEmo);
        	} else {
        		float[] allEmo = new float[4];
        		allEmo[0] = messageEmo[0];
        		allEmo[1] = messageEmo[1];
        		allEmo[2] = messageEmo[2];
        		allEmo[3] = messageEmo[3];
        		statistic.put(curDate, allEmo);
        	}
    	}
    	for(LocalDate curDate : statistic.keySet()) {
        	float[] allEmo = statistic.get(curDate);
    		allEmo[0] /= allEmo[3];
    		allEmo[1] /= allEmo[3];
    		allEmo[2] /= allEmo[3];
    		statistic.replace(curDate, allEmo); 
    	}
    	return statistic;
	}
	
	//получение письма от Яндекса
	public static String sendMessage(String msg) throws IOException {
		//подключение к базе яндекса
		URL url = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate");
		
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(10000);
        urlConnection.setConnectTimeout(15000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", "trnsl.1.1.20170206T155147Z.45d94d7b0d17f17a.463c81c05e43638dce9817f8c333120970707b10")); //my key
       // params.add(new BasicNameValuePair("key", "trnsl.1.1.20170206T144056Z.fc57db4943803739.f92dfb5b300712e7271fc5a7da0d0787060b50c8")); //not my key
        params.add(new BasicNameValuePair("text", msg)); //текст для перевода
        params.add(new BasicNameValuePair("lang", "en")); //язык перевода
        
        OutputStream os = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush(); //синхронизирует буффер
        writer.close();
        os.close();
 
        urlConnection.connect();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return in.readLine();
	}
	
	//формулировка запроса с учётом параметром для яндекса 
	private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (NameValuePair pair : params)
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");
	        result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }
	    return result.toString();
	}
	
	//все еще яндекс
	public static String ParseAndPrintTranslate(String text) throws Exception {
		JsonParser parser = new JsonParser(); 
		JsonObject mainObject = parser.parse(text).getAsJsonObject();
		JsonArray pItem = mainObject.getAsJsonArray("text");
		/*for (JsonElement user : pItem) {
		    //System.out.println(user.toString());
		    SentSense.AnalyzeText(user.toString(), output);
		    StanfordEmotion(text, outputS, i);
		}*/
		return pItem.get(0).toString();
	}
	
	public static List<Message> ParseForum() throws Exception {
		//String csvFile = "forum.csv";
		List<Message> messages = new ArrayList<Message>();
		CSVReader reader = new CSVReader(new FileReader("Media/forum-data/forum_post.csv"));
	     String [] nextLine = reader.readNext();
	     while ((nextLine = reader.readNext()) != null) {
	    	String id = nextLine[0];
	    	String date = nextLine[1];
	        String text = nextLine[3];
	        String user = nextLine[4];
	        messages.add(new Message("", 
	        		user.replaceAll("\"", ""), text.replaceAll("\"", ""), 
					date.replaceAll("\"", ""), id));
	        //System.out.println(text);
	     }
	    Collections.sort(messages);
	    //System.out.println(messages);
	    return messages;
	}
	
	public static void init(String profileDirectory) throws LangDetectException, IOException {
        //грузим профайл для определения языков
		DetectorFactory.loadProfile(profileDirectory);
    }
	
    public static ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }
    
    public static int[] StanfordEmotion(String text) throws FileNotFoundException {
    	double neg = 0, neut = 0, pos = 0;
    	int[] res = new int[4];
    	SentimentClass emotion;
    	
	    for (Sentence sent : getTextSentences(text)) {
	    	emotion = sent.sentiment();
	    	if (emotion.isPositive())
	    		pos++;
	    	else 
	    		if (emotion.isNeutral())
	    			neut++;
	    		else 
	    			neg++;
	    }	   
	    
	    res[0] = (int)neg;
	    res[1] = (int)neut;
	    res[2] = (int)pos;
	    res[3] = res[0] + res[1] + res[2]; //количество предложений
	    return res;
    }
    
    public static List<Sentence> getTextSentences(String text) {
    	edu.stanford.nlp.simple.Document doc = new edu.stanford.nlp.simple.Document(text);
    	return doc.sentences();
    }
}
