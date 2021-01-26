package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.opencsv.CSVWriter;

@RestController
public class DSSanctons {
	
	@RequestMapping("/Sanc")
	public static String Sanctions() throws Exception {
		
		String baseUrl = "https://scsanctions.un.org/fop/fop?xml=htdocs/resources/xml/en/consolidated.xml&xslt=htdocs/resources/xsl/en/consolidated.xsl";	
		
		
		try {
			
			File file = new File("src//main//resources//yourFile.pdf");
			
			/*URL url = new URL(baseUrl);
			InputStream in = url.openStream();
			FileOutputStream fos = new FileOutputStream(file);

			System.out.println("reading from resource and writing to file...");
			int length = -1;
			byte[] buffer = new byte[1024];// buffer for portion of data from connection
			while ((length = in.read(buffer)) > -1) {
			    fos.write(buffer, 0, length);
			}
			fos.close();
			in.close();
			System.out.println("File downloaded");
			*/
			
			String parsedText;
			PDFParser parser = new PDFParser(new RandomAccessFile(file, "r"));
			parser.parse();
			COSDocument cosDoc = parser.getDocument();
			PDFTextStripper pdfStripper = new PDFTextStripper();
			PDDocument pdDoc = new PDDocument(cosDoc);
			parsedText = pdfStripper.getText(pdDoc);
			PrintWriter pw = new PrintWriter("src//main//resources//pdf.txt");
			pw.print(parsedText);
			pw.close();
			
			
			List<String> test = Arrays.asList(parsedText.split("\n"));
			String text = String.join(" ",test);
			
			text = text.replaceAll("[\\s]+Page\\s[\\d]{1,4}\\sof\\s[\\d]{1,4}[\\s]+", "");
			text = text.replaceAll("Res.\\sList[\\s]+", "");
			text = text.replaceAll("[\r\n]+", " ");
			
			System.out.println(text);
			
			String individuals = "";
            String entities = "";
            
            //After getting all the data in a string using regular expression to extract only individual names
			
            String individuals_reg = "(Name:.*?Title:)";
            Pattern individuals_pattern = Pattern.compile(individuals_reg, Pattern.MULTILINE);
            Matcher individuals_matcher = individuals_pattern.matcher(text);
            while(individuals_matcher.find()) {
            	individuals += individuals_matcher.group(1)+"<br>";
            }
            
            for(String individual : individuals.split("<br>")) {
            	text = text.replace(individual, "");
            }
            
            String entities_reg = "Name:(.*?)A.k.a.:";
            Pattern entities_pattern = Pattern.compile(entities_reg, Pattern.MULTILINE);
            Matcher entities_matcher = entities_pattern.matcher(text);
            while(entities_matcher.find()) {
            	entities += entities_matcher.group(1)+"<br>";
            }
            
            //cleaning data by removing the extra words from the data dump
            individuals = individuals.replaceAll("[Name]+\\s[(original]+\\s[script)]+[:\\s]+", "");
            individuals = individuals.replaceAll("[\\p{IsArabic}\\p{IsCyrillic}\\p{IsHan}]+", "");
            individuals = individuals.replaceAll("[\\d]{1}:", "");
            individuals = individuals.replaceAll("na", "");
            individuals = individuals.replaceAll("Name:", "");
            individuals = individuals.replaceAll("Title:", "");
            individuals = individuals.replaceAll(",", " ");
            
            entities = entities.replaceAll("[Name]+\\s[(original]+\\s[script)]+[:\\s]+", "");
            entities = entities.replaceAll("[\\p{IsArabic}\\p{IsCyrillic}\\p{IsHan}]+", "");
            entities = entities.replaceAll(",", " ");
            
            //Storing all the names in arrrayList 
            List<String[]> entitiesList = new ArrayList<>();
           // 	entitiesList.add(new String[] {entity});
            
            List<String[]> individualsList = new ArrayList<>();
            for(int i=0; i<individuals.split("<br>").length; i++) {
            	for(int j=0; j<entities.split("<br>").length; j++) {
                    if ((individuals.split("<br>")[i] != individuals.split("<br>")[i+1])||
                    (entities.split("<br>")[j] != entities.split("<br>")[j+1])) {
            		individualsList.add(new String[] {individuals.split("<br>")[i],entities.split("<br>")[j]});
                    
                    }
                    
            	}
            }
            
            
            
            //Creating the CSV file
            FileWriter filewriter = new FileWriter("src//main//resources//scsanctions.csv"); 
            CSVWriter csvwriter = new CSVWriter(filewriter); 
            
            //Storing the List of names in the CSV file
            
            String[] header = { "Individuals", "Entities" }; 
            csvwriter.writeNext(header); 
            
            csvwriter.writeAll(individualsList);
            
            for(String[] ent : entitiesList) {
            	csvwriter.writeNext(ent);
            }
            
            csvwriter.close();
            
            return individuals+"<br><br><b>Entities<br>"+entities;
           	
    		
        }
        
        catch (JsonProcessingException e) {
			e.printStackTrace();
		}
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NoSuchElementException e) {
        	e.printStackTrace();;
        }
        
		return null;
    }

}
