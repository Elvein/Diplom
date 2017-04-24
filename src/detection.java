import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.bson.Document;
import org.bson.json.*;

import com.cybozu.labs.langdetect.*;
import com.mongodb.*;
import com.mongodb.client.*;

import synesketch.emotion.EmotionalState;
import synesketch.emotion.Empathyscope;

import com.google.gson.*;

import edu.stanford.nlp.simple.*;

public class detection {
	private static PrintWriter output, outputS;
	private static List<EmotionalState> sents = new ArrayList<EmotionalState>();
	private static String outputFileName = "test/outputBaseline/resultsSurpriseBaseline.txt";
	private static String outputStanf = "test/outputBaseline/emotionOfStanf.txt";
	
	private static String fileName = "test/inputBaseline/surprisedfun.txt";
	
	public static void main(String[] args) throws Exception {
		init("profiles");
		File outputFile = new File(outputFileName);
		output = new PrintWriter(new FileOutputStream(outputFile));
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		

    	File outputStan = new File(outputStanf);
		outputS = new PrintWriter(new FileOutputStream(outputStan));
		
		// подключение к базе
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("bragi_alt");
		MongoCollection<Document> collection = database.getCollection("messages");
		
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			int i = 0;
			Document nextCursor;
			String translation;
			String Text, Subject;
			ArrayList<Language> languages;
			while (cursor.hasNext() && i < 1000) {
		    	nextCursor = cursor.next();
		    	Text = nextCursor.getString("bodyText");
		    	Subject = nextCursor.getString("subject");
		    	
		    	if(!Text.isEmpty() && !Text.equals(" ") && !Subject.contains("Re:") && !Subject.contains("Fwd:") && !Text.contains("Re:"))
		    	{
		    		//для перевода текста с файла
			        /*String line = "";
			        do {
						line = in.readLine();
						if (line == null) break;
						try {
							// для определения языка
			    			languages = detectLangs(line);
			    			System.out.println(languages);
			    			
			    			if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.95) {
			    				translation = sendMessage(line); // выбираем само сообщение
						        ParseAndPrintTranslate(translation);  // получаем перевод письма
			    			}
			    			else {
			    				System.out.println(line); //что за текст печатаем
			    			    SentSense.AnalyzeText(line, output); //вывод первой библиотеки
			    			    StanfordEmotion(line, outputS);
			    			}
			    		} catch (LangDetectException e) {
			    			System.out.println("Can't detect language");
			    		}*/
		    		
		    			//для базы
						try {
							// для определения языка
			    			languages = detectLangs(Text);
			    			
			    			if (!languages.get(0).lang.equals("en") || languages.get(0).prob < 0.95) {
			    				translation = sendMessage(Text); // выбираем само сообщение
						        ParseAndPrintTranslate(translation);  // получаем перевод письма
			    			}
			    			else {
			    			//	System.out.println(Text); //что за текст печатаем
			    			    SentSense.AnalyzeText(Text, output); //вывод первой библиотеки
			    			    StanfordEmotion(Text, outputS);
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

    	output.close();
    	outputS.close();
	}
	
	/*public static void write(String fileName, String text) {
	    //Определяем файл
	    File file = new File(fileName);
	 
	    try {
	        //проверяем, что если файл не существует то создаем его
	        if(!file.exists()){
	            file.createNewFile();
	        }
	 
	        //PrintWriter обеспечит возможности дозаписи в файл
	        FileWriter out = new FileWriter(file.getAbsoluteFile(), true);
	 
	        try {
	            //Записываем текст у файл
	            out.write(text);
	        } finally {
	            //После чего мы должны закрыть файл
	            //Иначе файл не запишется
	            out.close();
	        }
	    } catch(IOException e) {
	        throw new RuntimeException(e);
	    }
	}*/
	
	
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
	
	public static void ParseAndPrintTranslate(String text) throws Exception {
		JsonParser parser = new JsonParser(); 
		JsonObject mainObject = parser.parse(text).getAsJsonObject();
		JsonArray pItem = mainObject.getAsJsonArray("text");
		for (JsonElement user : pItem) {
		    //System.out.println(user.toString());
		    SentSense.AnalyzeText(user.toString(), output);
		    StanfordEmotion(text, outputS);
		    
		    //write("output.txt", user.toString());
		}
	}
	
	public static void init(String profileDirectory) throws LangDetectException, IOException {
        //грузим профайл для определения языков
		DetectorFactory.loadProfile(profileDirectory);
    }
	
	//определяем язык -- не используется
    public static String detect(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }
	
    public static ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }
    
    public static void StanfordEmotion(String text, PrintWriter output) throws FileNotFoundException {
    	double neg = 0, neut = 0, pos = 0;
    	int sum = 0;
    	SentimentClass emotion;
    	
    	edu.stanford.nlp.simple.Document doc = new edu.stanford.nlp.simple.Document(text);
	    for (Sentence sent : doc.sentences()) {
	    	sum ++;
	    	emotion = sent.sentiment();
	    	if (emotion.isPositive())
	    		pos++;
	    	else 
	    		if (emotion.isNeutral())
	    			neut++;
	    		else 
	    			if (emotion.isNegative())
	    				neg++;
	    			else
	    				System.out.println(emotion); //эмоции со второй библиотеки
	    }
	    pos/=sum; neut/=sum; neg/=sum;
	    output.println(pos  + " " + neut + " " + neg); //эмоции со второй библиотеки
	    output.flush();
    }
}
