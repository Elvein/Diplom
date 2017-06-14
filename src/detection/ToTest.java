package detection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class ToTest {

	public static void main (String []args) throws IOException, Exception {
		File folder = new File("D:/NSTU/Diplom/aclImdb_v1.tar/aclImdb_v1/aclImdb/train/neg");
		processFilesFromFolder(folder);
	}
	
	static public void processFilesFromFolder(File folder) throws FileNotFoundException, IOException
	{
	    File[] folderEntries = folder.listFiles(); 
	    int i = 0;
	    for (File entry : folderEntries)
	    {
	    	if (i >= 500)
	    		break;
	        if (entry.isDirectory())
	        {
	            processFilesFromFolder(entry);
	            continue;
	        }
	      //  System.out.println(entry);
	        BufferedReader in = new BufferedReader(new FileReader(entry));
			String line = "";
			while ((line = in.readLine()) != null) {
			
				if (line == null) 
					break;
		    	float [] allEmo = {0,0,0,0};
		    	int[] messageEmo;
		        messageEmo = detection.StanfordEmotion(line);
				allEmo[0] += messageEmo[0];
	            allEmo[1] += messageEmo[1];
	            allEmo[2] += messageEmo[2];
	            allEmo[3] += messageEmo[3];
				//System.out.println("Neg: " + allEmo[0] + ", neu: " + allEmo[1] + ", pos: " + allEmo[2]); 
				String maxN = allEmo[0] > allEmo[1] && allEmo[0] > allEmo[2] ? "Негатив" : allEmo[2] > allEmo[1] ? "Positive" : "Нейтрально";
				//System.out.println("Max: " + maxN); 
				write("neg_tr", maxN);
			}
			i++;
	    }
	}
	
	public static void write(String fileName, String text) {
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
	        	out.write("\n");
	        	out.write(text);
	        } finally {
	            //После чего мы должны закрыть файл
	            //Иначе файл не запишется
	            out.close();
	        }
	    } catch(IOException e) {
	        throw new RuntimeException(e);
	    }
	}
}
