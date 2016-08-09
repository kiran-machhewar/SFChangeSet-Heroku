package servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.util.JSONPObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sforce.ws.ConnectionException;


@WebServlet(
        name = "DeployCodeServlet", 
        urlPatterns = {"/DeployCode"}
    )
public class DeployCode extends HttpServlet {
	public static Integer counter =0;
	
	public static FileBasedDeployAndRetrieve fileBasedDeployAndRetrieve ;
	
	
	public static FileBasedDeployAndRetrieve getFileBasedDeployAndRetriev(){
		return new FileBasedDeployAndRetrieve();		
	}
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
    	
        ServletOutputStream out = resp.getOutputStream();         
        String log = "{\"IsDeploymentInProgress\":\""+DeploymentStatus.getIsDeploymentInProgress()+"\",\"Message\":\""+DeploymentStatus.getMessage()+"\"}";
        out.write(log.getBytes());
        out.flush();
        out.close();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       ServletOutputStream out = resp.getOutputStream();
       StringBuffer jb = new StringBuffer();
       String line = null;
       try {
         BufferedReader reader = req.getReader();
         while ((line = reader.readLine()) != null)
           jb.append(line);
       } catch (Exception e) { /*report an error*/ }       
       FileBasedDeployAndRetrieve fileBasedDeployAndRetrieve = getFileBasedDeployAndRetriev();
       
       String status = "";
       String       message = "To check status please check deployment status in target org.";
       if(DeploymentStatus.getIsDeploymentInProgress()){
    	   status = "AnotherDeploymentIsInProgress";
       }else{
    	   status = "DeploymentStarted";
	       
	       try {
	    	    DeploymentStatus.setMessage("Fetching metada for deployment..");
	    	    DeploymentInfo deploymentInfo = getDeploymentInfo(jb.toString());    	
	    	    fileBasedDeployAndRetrieve.setDeploymentInfo(deploymentInfo);
	       		fileBasedDeployAndRetrieve.setPackageXMLAsString(StringEscapeUtils.unescapeXml(deploymentInfo.packageXML));
	       		fileBasedDeployAndRetrieve.setSourceOrgConnection(MetadataLoginUtil.loginToSourceOrg(deploymentInfo.sourceUsername, deploymentInfo.sourcePassword, deploymentInfo.sourceType));
				fileBasedDeployAndRetrieve.setTargetOrgConnection(MetadataLoginUtil.loginToSourceOrg(deploymentInfo.targetUsername, deploymentInfo.targetPassword, deploymentInfo.targetType));
				fileBasedDeployAndRetrieve.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
       }
	   String jsonResponse = "{\"Status\":\""+status+"\",\"Message\":\""+message+"\"}";
       out.write(jsonResponse.getBytes());
    }
    
    public static void main(String[] args) throws Exception {
    	
    	BufferedReader br = null;

    	String jsonData ="{  \"testClassesToBeExecuted\" : null,\"isValidate\" : false , \"targetOrg\" : {    \"attributes\" : {      \"type\" : \"Salesforce_Org__c\",      \"url\" : \"/services/data/v35.0/sobjects/Salesforce_Org__c/a0528000003ay1XAAQ\"    },    \"Id\" : \"a0528000003ay1XAAQ\",    \"Username__c\" : \"kiran@sflab.com\",    \"Password__c\" : \"ironman@12345\",    \"Type__c\" : \"Production\"  },  \"sourceOrg\" : {    \"attributes\" : {      \"type\" : \"Salesforce_Org__c\",      \"url\" : \"/services/data/v35.0/sobjects/Salesforce_Org__c/a0528000003ay1cAAA\"    },    \"Id\" : \"a0528000003ay1cAAA\",    \"Username__c\" : \"kiran@group.com\",    \"Password__c\" : \"newuser@123\",    \"Type__c\" : \"Production\"  },  \"packageXML\" : \"&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;Package xmlns=&quot;http://soap.sforce.com/2006/04/metadata&quot;&gt;&lt;types&gt;&lt;members&gt;SFChange&lt;/members&gt;&lt;name&gt;ApexClass&lt;/name&gt;&lt;/types&gt;&lt;version&gt;35.0&lt;/version&gt;&lt;/Package&gt;\"}";
		DeploymentInfo deploymentInfo = getDeploymentInfo(jsonData);
		FileBasedDeployAndRetrieve fileBasedDeployAndRetrieve  = new FileBasedDeployAndRetrieve();
		fileBasedDeployAndRetrieve.setDeploymentInfo(deploymentInfo);
		fileBasedDeployAndRetrieve.setSourceOrgConnection(MetadataLoginUtil.loginToSourceOrg(deploymentInfo.sourceUsername, deploymentInfo.sourcePassword, deploymentInfo.sourceType));
		fileBasedDeployAndRetrieve.setTargetOrgConnection(MetadataLoginUtil.loginToSourceOrg(deploymentInfo.targetUsername, deploymentInfo.targetPassword, deploymentInfo.targetType));
		fileBasedDeployAndRetrieve.setPackageXMLAsString(StringEscapeUtils.unescapeXml(deploymentInfo.packageXML));
		fileBasedDeployAndRetrieve.run();
	}
    
    public static DeploymentInfo getDeploymentInfo(String jsonData) throws org.json.simple.parser.ParseException{
    	JSONParser parser = new JSONParser();
		Object obj;
		obj = parser.parse(jsonData);
		System.out.println(obj);
		JSONObject jobj = (JSONObject)obj;
		String packageXML = (String) jobj.get("packageXML");
		DeploymentInfo deploymentInfo = new DeploymentInfo();
		//Getting sourceOrg details
		JSONObject sourceOrg = (JSONObject)jobj.get("sourceOrg");
		deploymentInfo.sourceUsername = (String) sourceOrg.get("Username__c");
		deploymentInfo.sourcePassword = (String) sourceOrg.get("Password__c");
		deploymentInfo.sourceType = (String)sourceOrg.get("Type__c");

		JSONObject targetOrg = (JSONObject)jobj.get("targetOrg"); 
		deploymentInfo.targetUsername = (String) targetOrg.get("Username__c");
		deploymentInfo.targetPassword = (String) targetOrg.get("Password__c");
		deploymentInfo.targetType = (String)targetOrg.get("Type__c");
		deploymentInfo.isValidateOnly = Boolean.valueOf(""+jobj.get("isValidate"));
		deploymentInfo.testOptions = (String)jobj.get("testOptions");
		deploymentInfo.testClassesToBeExecuted = (String)jobj.get("testClassesToBeExecuted");
		deploymentInfo.packageXML = packageXML;
		
		System.out.println("DEPLOYMENT INFO");
		System.out.println(deploymentInfo);
		return deploymentInfo;
    	
    }
}
