package detection;
import java.io.*;

import java.net.*;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.bson.Document;
import org.bson.json.*;

import com.cybozu.labs.langdetect.*;
import com.mongodb.*;
import com.mongodb.client.*;

import synesketch.emotion.EmotionalState;

import com.google.gson.*;
import edu.stanford.nlp.simple.*;


public class detection {
	private static PrintWriter output, outputS, outputLetter;
	private static List<EmotionalState> sents = new ArrayList<EmotionalState>();
	private static String outputFileName = "test/outputBaseline/resultsSurpriseBaseline.txt";
	private static String outputStanf = "test/outputBaseline/emotionOfStanf.txt";
	private static String outLetter = "test/outputBaseline/Letters.txt";
	
	private static String fileName = "test/inputBaseline/surprisedfun.txt";
	
	public static void init() throws LangDetectException, IOException {
		init("profiles");
	}
	public static void main(String[] args) throws Exception {
		List<Message> forumMessages = ParseForum();
		//AnalyzeForum(forumMessages, new Date("24-10-2011"));
		
		//Для писем с базы
		//TranslateLetters();

		BufferedReader in = new BufferedReader(new FileReader(fileName));
						/*
						try {
							// для определения языка
			    			languages = detectLangs(Text);
			    			
			    			if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.96) {
			    				translation = sendMessage(Text); // выбираем само сообщение
						        ParseAndPrintTranslate(translation, i);  // получаем перевод письма
			    			}
			    			else {
			    				
			    				outputLetter.println("\n" + i + "\n" + Text); //эмоции со второй библиотеки
			    				outputLetter.flush();
			    			    SentSense.AnalyzeText(Text, output); //вывод первой библиотеки
			    			    StanfordEmotion(Text, outputS, i);
			    			}
			    		} catch (LangDetectException e) {
			    			System.out.println("Can't detect language");
			    		}*/
		    //	} while (true);
			        
	}
		
	public static void TranslateLetters() throws Exception {
		File outputFile = new File(outputFileName);
		output = new PrintWriter(new FileOutputStream(outputFile));

    	File outputLet = new File(outLetter);
    	outputLetter = new PrintWriter(new FileOutputStream(outLetter));
		
		File outputStan= new File(outputStanf);
		outputS = new PrintWriter(new FileOutputStream(outputStan));
		
		ArrayList<Language> languages;
		//работа с базой
		int i = 0;
		String translation;
		String Text, Subject;
		// подключение к базе
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("bragi_alt");
		MongoCollection<Document> collection = database.getCollection("messages");
				
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			Document nextCursor;
			while (cursor.hasNext() && i < 1000) {
		    	nextCursor = cursor.next();
		    	Text = nextCursor.getString("bodyText");
		    	Subject = nextCursor.getString("subject");
		    					    			    	
		    	if(!Text.isEmpty() && !Text.equals(" ") && !Subject.contains("Re:") && !Subject.contains("Fwd:") && !Text.contains("Re:") && !Text.contains(">>>")) {
		    		//для перевода текста с файла
		    		try {
		    			// для определения языка
				    	languages = detectLangs(Text);
				    			
				    	if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.97) {
				    		outputLetter.println(Text); //вывод письма в файл
					    	outputLetter.println("\n------------------------------------------------------\n"); 
					    	outputLetter.flush();
					    	
				    		translation = sendMessage(Text); // выбираем само сообщение
				    		translation = ParseAndPrintTranslate(translation);  // получаем перевод письма
						    outputLetter.println(translation); //вывод письма в файл
						    outputLetter.println("\n------------------------------------------------------\n"); 
					    	outputLetter.flush();
					    	
					    	int[] allEmo  = StanfordEmotion(Text);
			    	    	output.println(i + "\n" + allEmo[0]  + " " + allEmo[1] + " " + allEmo[2]); //вывод эмоции со второй библиотеки
			    	 	    output.flush();
				    	}
				    	else {
				    			//System.out.println(Text); //что за текст печатаем
				    	    	SentSense.AnalyzeText(Text, output); //вывод первой библиотеки
				    	    	int[] allEmo  = StanfordEmotion(Text);
				    	    	output.println(i + "\n" + allEmo[0]  + " " + allEmo[1] + " " + allEmo[2]); //вывод эмоции со второй библиотеки
				    	 	    output.flush();
				    	}
				    } catch (LangDetectException e) {
				    	System.out.println("Can't detect language");
				    }
		    	}
					i = i+1;
			}
		} finally {
			cursor.close();
		}
		outputLetter.close();
		outputS.close();
	}
		
	public static Map<String,float[]> AnalyzeForum(List<Message> msg, LocalDate startDate, LocalDate endDate, boolean translate) throws Exception {
		ArrayList<Language> languages;
		Map<String,float[]> statistic = new HashMap<String,float[]>();
		
		String text, translation;
        for (int i = 0; i < msg.size(); i++) {
        	int[] messageEmo;
			try {
				text = msg.get(i).MessageText;
				LocalDate msgTime = msg.get(i).Time.toLocalDate();
				if(!(msgTime.isAfter(startDate) || msgTime.isEqual(startDate)) || !(msgTime.isBefore(endDate) || msgTime.isEqual(endDate)))
					continue;
				// для определения языка
    			languages = detectLangs(text);
				if (translate && (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.98)) {
    				translation = sendMessage(text); // выбираем само сообщение
			        translation = ParseAndPrintTranslate(translation);  // получаем перевод письма
			        msg.get(i).MessageText = translation;
    				messageEmo = StanfordEmotion(translation);
    			} else {
    				messageEmo = StanfordEmotion(text);
    			}
    			
    			if (statistic.containsKey(msg.get(i).UserName)) {
            		float[] allEmo = statistic.get(msg.get(i).UserName);
            		allEmo[0] += messageEmo[0];
            		allEmo[1] += messageEmo[1];
            		allEmo[2] += messageEmo[2];
            		allEmo[3] += messageEmo[3];
            		statistic.replace(msg.get(i).UserName, allEmo);
            	} else {
            		float[] allEmo = new float[4];
            		allEmo[0] = messageEmo[0];
            		allEmo[1] = messageEmo[1];
            		allEmo[2] = messageEmo[2];
            		allEmo[3] = messageEmo[3];
            		statistic.put(msg.get(i).UserName, allEmo);
            	}
    		} catch (LangDetectException e) {
    			System.out.println("Can't detect language");
    			System.out.println(e.toString());
    		}
        }
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
        params.add(new BasicNameValuePair("key", "trnsl.1.1.20170206T155147Z.45d94d7b0d17f17a.463c81c05e43638dce9817f8c333120970707b10"));
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
		String csvFile = "forum.csv";
	    String line = "";
	    String cvsSplitBy = ";";
		
	    List<Message> messages = new ArrayList<Message>();
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile),"UTF-8"))) {

	    	JsonParser parser = new JsonParser();
			JsonObject mainObject = parser.parse(br).getAsJsonObject();
			JsonArray sections = mainObject.getAsJsonArray("sections");
			for (JsonElement section : sections) {
				JsonObject sectionObj = section.getAsJsonObject();
				JsonArray topics = sectionObj.getAsJsonArray("topics");
				for (JsonElement topic : topics) {
					JsonObject topicObj = topic.getAsJsonObject();
					JsonArray topicMessages = topicObj.getAsJsonArray("messages");
					for (JsonElement message : topicMessages)
					{
						JsonObject messageObj = message.getAsJsonObject();
						messages.add(new Message(sectionObj.get("name").toString().replaceAll("\"", ""), topicObj.get("topic").toString().replaceAll("\"", ""), 
								messageObj.get("user").toString().replaceAll("\"", ""), messageObj.get("message").toString().replaceAll("\"", ""), 
								messageObj.get("date").toString().replaceAll("\"", "")));
					}
				}
			}
	    	/*while ((line = br.readLine()) != null) {
	    		// use comma as separator
	            String[] message = line.split(cvsSplitBy);
	            System.out.println(messages);
	            //, Integer.parseInt(message[2].replaceAll("\\D+", ""))
	            messages.add(new Message(message[0], message[1], message[2], message[3], message[4]));
            }*/
            System.out.println(messages);
        } catch (IOException e) {
        	e.printStackTrace();
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
    	int sum = 0; //общее число предложений
    	int[] res = new int[4];
    	SentimentClass emotion;
    	
	    for (Sentence sent : getTextSentences(text)) {
	    	sum ++;
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
	    res[3] = res[0] + res[1] + res[2];
	    return res;
    }
    
    public static List<Sentence> getTextSentences(String text) {
    	edu.stanford.nlp.simple.Document doc = new edu.stanford.nlp.simple.Document(text);
    	return doc.sentences();
    }
}
