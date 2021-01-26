package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;  
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataScraping {
	//Input variables that need to be provided by the user
	private static String state = "  karnataka  ";
	private static String tinNumber = "----------";
	
	
	@RequestMapping("/TIN")
	public static String TinDetails() throws Exception {
		
		String baseUrl = "http://www.tinsearch.in/"+state.strip().replace(" ", "-").toLowerCase()+".html";	
		
		//Initializing the WebClient of the HtmlUnit
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
		Iterator<HtmlElement> it;
		HtmlPage page2 = null;
        
		try {
			//Getting the starter page of the baseUrl
            HtmlPage page = client.getPage(baseUrl);
            HtmlForm form = page.getHtmlElementById("defaultForm");
            System.out.println(baseUrl);
            
            //Checking if baseUrl contain delhi state, if it matches then capture the captcha from img tag
            //then read text from the image using the GoogleVisioApi and set the captcha to the particular form
            if(state.strip().toLowerCase().equals("delhi")) 
            {
	            String img_tag = new String();
	            DomNodeList<HtmlElement> ele = page.getDocumentElement().getElementsByTagName("img");      
	         	img_tag = (ele.get(0)).asXml().toString();
	        	System.out.println(img_tag);
	        	
	        	String captcha_img = "src=\"(.*?)\"\\/>";
				Pattern captcha_img_pattern = Pattern.compile(captcha_img, Pattern.MULTILINE);
				Matcher captcha_img_matcher = captcha_img_pattern.matcher(img_tag);
				if(captcha_img_matcher.find()) {
					for (int i = 1; i <= captcha_img_matcher.groupCount(); i++) {
						captcha_img = captcha_img_matcher.group(i);
					}
				}
				System.out.println(captcha_img.split(",")[1]);
				
				byte[] imgBytes = Base64.decodeBase64(captcha_img.split(",")[1]);  
				File imgFile = new File("src\\main\\resources\\captcha.png");  
		        BufferedImage br_img = ImageIO.read(new ByteArrayInputStream(imgBytes));  
		        ImageIO.write(br_img, "png", imgFile);  			
				
		        //Calling the user defined GoogleVisionApi function for reading the captcha image
		        GoogleVisionApi obj = new GoogleVisionApi();
	        	String capText = obj.detectText(imgFile.toString());
	        	
	        	HtmlTextInput captchaInput = page.getElementByName("code");
	    		captchaInput.setValueAttribute(capText);
	            
	        }
        	
            //setting the tinNumber to the particular attribute
    		HtmlTextInput tinInput = page.getElementByName("tin");
    		tinInput.setValueAttribute(tinNumber);
            
    		//After setting all the attributes click the submit button to get to the details page
    		HtmlButton submit = form.getOneHtmlElementByAttribute("button", "type", "submit");
    		page2 = submit.click();
    		
    		//Storing the tin details into the json structure  
            it = page2.getDocumentElement().getHtmlElementsByTagName("table").iterator();
            final String secret = it.next().asText();
                
            String[] result = secret.split("\n");
            HashMap<Object,Object> dict = new HashMap<>();
            for(int i=0; i<result.length; i++) {
            	dict.put(result[i].split("\t")[0],result[i].split("\t")[1]);    	
            }
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            jsonString = mapper.writeValueAsString(dict);
	    
            System.out.println(secret);
            
	    	return jsonString+"<br><br>Below data is in the HashMap format:<br><br>"+dict;
    		
        }
        
        catch (JsonProcessingException e) {
			e.printStackTrace();
		}
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NoSuchElementException e) {
            System.out.println("Dealers not found");
        	return page2.asXml();
        }
        
		return null;
    }

}
