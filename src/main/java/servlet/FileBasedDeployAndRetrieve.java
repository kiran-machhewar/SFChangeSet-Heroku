package servlet;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.sforce.soap.metadata.*;

/**
 * Sample that logs in and shows a menu of retrieve and deploy metadata options.
 */
public class FileBasedDeployAndRetrieve extends Thread {

    private MetadataConnection sourceOrgConnection;
    
    private DeploymentInfo deploymentInfo;
    
    private byte[] zipFileData;
    
    private MetadataConnection targetOrgConnection;

    private static final String ZIP_FILE = "components.zip";

    // manifest file that controls which components get retrieved
    private static final String MANIFEST_FILE = "package.xml";
    
    private String packageXMLAsString;

    private static final double API_VERSION = 29.0;

    // one second in milliseconds
    private static final long ONE_SECOND = 1000;

    // maximum number of attempts to deploy the zip file
    private static final int MAX_NUM_POLL_REQUESTS = 50;

    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    

    public static void main(String[] args) throws Exception {
        FileBasedDeployAndRetrieve sample = new FileBasedDeployAndRetrieve();
        sample.run();
    }

    public FileBasedDeployAndRetrieve() {
    }

    public static class DeploymentInProgressException extends Exception{}
    public void run() {
    	if(!DeploymentStatus.getIsDeploymentInProgress()){
    		try{
    			DeploymentStatus.setIsDeploymentInProgress(true);
    			DeploymentStatus.setMessage("Fetching metadata for "+(this.deploymentInfo.isValidateOnly?"validation.":"deployment."));
    			retrieveZip();
    			DeploymentStatus.setMessage("Metadata from source org is fetching.");
    			DeploymentStatus.setMessage((this.deploymentInfo.isValidateOnly?"Validation":"Deployment")+" is in progress.");    			
    			deployZip();            		    			
    		}catch(Exception ex){
    			DeploymentStatus.setMessage(ex.getMessage());
    			ex.printStackTrace();
    		}finally{
    			DeploymentStatus.setIsDeploymentInProgress(false);    			
    		}
    	}
    }


    private void deployZip() throws Exception {
        byte zipBytes[] = readZipFile();
        DeployOptions deployOptions = new DeployOptions();
        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        deployOptions.setCheckOnly(deploymentInfo.isValidateOnly);  
        if(deploymentInfo.testOptions!=null && deploymentInfo.testOptions.equalsIgnoreCase("Run Specified Tests")){
        	deployOptions.setTestLevel(TestLevel.RunSpecifiedTests);
        	if(deploymentInfo.testClassesToBeExecuted != null &&
        			!deploymentInfo.testClassesToBeExecuted.equalsIgnoreCase("null") ){
        			String[] testClasses = deploymentInfo.testClassesToBeExecuted.split(";");        			
	        		deployOptions.setRunTests(testClasses);        		
        	}
        }else if(deploymentInfo.testOptions!=null && deploymentInfo.testOptions.equalsIgnoreCase("Run Local Tests")){
        	deployOptions.setTestLevel(TestLevel.RunLocalTests);
        }else if(deploymentInfo.testOptions!=null && deploymentInfo.testOptions.equalsIgnoreCase("Run All Tests")){
        	deployOptions.setTestLevel(TestLevel.RunAllTestsInOrg);
        }else if(deploymentInfo.testOptions!=null && deploymentInfo.testOptions.equalsIgnoreCase("Default")){
        	deployOptions.setTestLevel(TestLevel.NoTestRun);
        }
        
        AsyncResult asyncResult = getTargetOrgConnection().deploy(zipBytes, deployOptions);
        DeployResult result = waitForDeployCompletion(asyncResult.getId());
        if (!result.isSuccess()) {
            printErrors(result, "Final list of failures:\n");
            DeploymentStatus.setMessage((this.deploymentInfo.isValidateOnly?"Validation":"Deployment")+" is failed. Please check target org for detailed errors.");
            throw new Exception("The files were not successfully deployed");
        }
        System.out.println("The file " + ZIP_FILE + " was successfully deployed\n");
        DeploymentStatus.setMessage((this.deploymentInfo.isValidateOnly?"Validation":"Deployment")+" is successful.");
    }

    /*
    * Read the zip file contents into a byte array.
    */
    private byte[] readZipFile() {
    	return zipFileData;
    }

    /*
    * Print out any errors, if any, related to the deploy.
    * @param result - DeployResult
    */
    private void printErrors(DeployResult result, String messageHeader) {
        DeployDetails details = result.getDetails();
        StringBuilder stringBuilder = new StringBuilder();
        if (details != null) {
            DeployMessage[] componentFailures = details.getComponentFailures();
            for (DeployMessage failure : componentFailures) {
                String loc = "(" + failure.getLineNumber() + ", " + failure.getColumnNumber();
                if (loc.length() == 0 && !failure.getFileName().equals(failure.getFullName()))
                {
                    loc = "(" + failure.getFullName() + ")";
                }
                stringBuilder.append(failure.getFileName() + loc + ":" 
                    + failure.getProblem()).append('\n');
            }
            RunTestsResult rtr = details.getRunTestResult();
            if (rtr.getFailures() != null) {
                for (RunTestFailure failure : rtr.getFailures()) {
                    String n = (failure.getNamespace() == null ? "" :
                        (failure.getNamespace() + ".")) + failure.getName();
                    stringBuilder.append("Test failure, method: " + n + "." +
                            failure.getMethodName() + " -- " + failure.getMessage() + 
                            " stack " + failure.getStackTrace() + "\n\n");
                }
            }
            if (rtr.getCodeCoverageWarnings() != null) {
                for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
                    stringBuilder.append("Code coverage issue");
                    if (ccw.getName() != null) {
                        String n = (ccw.getNamespace() == null ? "" :
                        (ccw.getNamespace() + ".")) + ccw.getName();
                        stringBuilder.append(", class: " + n);
                    }
                    stringBuilder.append(" -- " + ccw.getMessage() + "\n");
                }
            }
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.insert(0, messageHeader);            
            System.out.println(stringBuilder.toString());
        }
    }
    

    private void retrieveZip() throws Exception {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        // The version in package.xml overrides the version in RetrieveRequest
        retrieveRequest.setApiVersion(API_VERSION);
        setUnpackaged(retrieveRequest);

        AsyncResult asyncResult = getSourceOrgConnection().retrieve(retrieveRequest);
        
        RetrieveResult result = waitForRetrieveCompletion(asyncResult);

        if (result.getStatus() == RetrieveStatus.Failed) {
            throw new Exception(result.getErrorStatusCode() + " msg: " +
                    result.getErrorMessage());
        } else if (result.getStatus() == RetrieveStatus.Succeeded) {  
	        // Print out any warning messages
	        StringBuilder stringBuilder = new StringBuilder();
	        if (result.getMessages() != null) {
	            for (RetrieveMessage rm : result.getMessages()) {
	                stringBuilder.append(rm.getFileName() + " - " + rm.getProblem() + "\n");
	            }
	        }
	        if (stringBuilder.length() > 0) {
	            System.out.println("Retrieve warnings:\n" + stringBuilder);
	        }
	
	        System.out.println("Writing results to zip file");
	        zipFileData = result.getZipFile();
	        ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipFileData));
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	ZipOutputStream zos = new ZipOutputStream(baos);
	        for (ZipEntry zipEntry;(zipEntry = zin.getNextEntry()) != null; )
	        {
	            Scanner sc = new Scanner(zin);
	            String data = "";
	            Boolean firstLine = true;
	            while (sc.hasNextLine())
	            {
	            	String newLine = sc.nextLine();
	            	data += ((firstLine)?"":"\n")+newLine;
	            	firstLine = false;
	            }
	            if(zipEntry.getName().endsWith("profile")){
		            data = data.replaceAll("(?s)<userPermissions[^>]*>.*?</userPermissions>","");
		            data = data.replaceAll("(?s)<loginIpRanges[^>]*>.*?</loginIpRanges>","");	
	            }
	            ZipEntry tempZipEntry = new ZipEntry(zipEntry.getName());
	            zos.putNextEntry(tempZipEntry);
	            zos.write(data.getBytes());
	            zos.closeEntry();
	        }
	        baos.close();
	        zos.close();
	        zipFileData = baos.toByteArray();
	        zin.close();
        }
    }

    private DeployResult waitForDeployCompletion(String asyncResultId) throws Exception {
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        DeployResult deployResult;
        boolean fetchDetails;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception(
                    "Request timed out. If this is a large set of metadata components, " +
                    "ensure that MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            // Fetch in-progress details once for every 3 polls
            fetchDetails = (poll % 3 == 0);

            deployResult = getTargetOrgConnection().checkDeployStatus(asyncResultId, fetchDetails);
            System.out.println("Status is: " + deployResult.getStatus());
            if (!deployResult.isDone() && fetchDetails) {
                printErrors(deployResult, "Failures for deployment in progress:\n");
            }
        }
        while (!deployResult.isDone());

        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
            throw new Exception(deployResult.getErrorStatusCode() + " msg: " +
                    deployResult.getErrorMessage());
        }
        
        if (!fetchDetails) {
            // Get the final result with details if we didn't do it in the last attempt.
            deployResult = getTargetOrgConnection().checkDeployStatus(asyncResultId, true);
        }
        
        return deployResult;
    }

    private RetrieveResult waitForRetrieveCompletion(AsyncResult asyncResult) throws Exception {
    	// Wait for the retrieve to complete
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        String asyncResultId = asyncResult.getId();
        RetrieveResult result = null;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // Double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out.  If this is a large set " +
                "of metadata components, check that the time allowed " +
                "by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            result = getSourceOrgConnection().checkRetrieveStatus(
                    asyncResultId,true);
            System.out.println("Retrieve Status: " + result.getStatus());
        } while (!result.isDone());         

        return result;
    }

    private void setUnpackaged(RetrieveRequest request) throws Exception {
        com.sforce.soap.metadata.Package p = parsePackageManifest(null);
        request.setUnpackaged(p);
    }

    private com.sforce.soap.metadata.Package parsePackageManifest(File file)
            throws ParserConfigurationException, IOException, SAXException {
        com.sforce.soap.metadata.Package packageManifest = null;
        List<PackageTypeMembers> listPackageTypes = new ArrayList<PackageTypeMembers>();
        DocumentBuilder db =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        /*InputStream inputStream = new FileInputStream(file)*/;
        String packageXML = getPackageXMLAsString();
        System.out.println(packageXML);
        InputStream stream = new ByteArrayInputStream(packageXML.getBytes(StandardCharsets.UTF_8));
        Element d = db.parse(stream).getDocumentElement();
        for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element) {
                Element ce = (Element) c;
                NodeList nodeList = ce.getElementsByTagName("name");
                if (nodeList.getLength() == 0) {
                    continue;
                }
                String name = nodeList.item(0).getTextContent();
                NodeList m = ce.getElementsByTagName("members");
                List<String> members = new ArrayList<String>();
                for (int i = 0; i < m.getLength(); i++) {
                    Node mm = m.item(i);
                    members.add(mm.getTextContent());
                }
                PackageTypeMembers packageTypes = new PackageTypeMembers();
                packageTypes.setName(name);
                packageTypes.setMembers(members.toArray(new String[members.size()]));
                listPackageTypes.add(packageTypes);
            }
        }
        packageManifest = new com.sforce.soap.metadata.Package();
        PackageTypeMembers[] packageTypesArray =
                new PackageTypeMembers[listPackageTypes.size()];
        packageManifest.setTypes(listPackageTypes.toArray(packageTypesArray));
        packageManifest.setVersion(API_VERSION + "");
        return packageManifest;
    }

	public MetadataConnection getTargetOrgConnection() {
		return targetOrgConnection;
	}

	public void setTargetOrgConnection(MetadataConnection targetOrgConnection) {
		this.targetOrgConnection = targetOrgConnection;
	}

	public MetadataConnection getSourceOrgConnection() {
		return sourceOrgConnection;
	}

	public void setSourceOrgConnection(MetadataConnection sourceOrgConnection) {
		this.sourceOrgConnection = sourceOrgConnection;
	}

	public String getPackageXMLAsString() {
		return packageXMLAsString;
	}

	public void setPackageXMLAsString(String packageXMLAsString) {
		this.packageXMLAsString = packageXMLAsString;
	}

	public DeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

	public void setDeploymentInfo(DeploymentInfo deploymentInfo) {
		this.deploymentInfo = deploymentInfo;
	}
	
}