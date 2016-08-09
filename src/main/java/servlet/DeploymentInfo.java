package servlet;

import org.omg.Messaging.SyncScopeHelper;

public class DeploymentInfo {
	public String packageXML;
	public String sourceUsername;
	public String sourcePassword;
	public String sourceType;
	public String targetUsername;
	public String targetPassword;
	public String targetType;	
	public Boolean isValidateOnly;
	public String testOptions;
	public String testClassesToBeExecuted;
	
	@Override
	public String toString() {
		String str = "packageXML-->"+packageXML+"\n";
		str += "sourceUsername-->"+sourceUsername+"\n";
		str += "sourcePassword-->"+sourcePassword+"\n";
		str += "sourceType-->"+sourceType+"\n";
		str += "targetUsername-->"+targetUsername+"\n";
		str += "targetPassword-->"+targetPassword+"\n";
		str += "targetType-->"+targetType+"\n";	
		str +="isValidateOnly-->"+isValidateOnly;
		str +="testOptions-->"+testOptions;
		
		return str;
	}
}
