package servlet;

public class DeploymentStatus {
	private static Boolean IsDeploymentInProgress = false;

	private static String message = "";
	
	
	
	public static Boolean getIsDeploymentInProgress() {
		return IsDeploymentInProgress;
	}

	public static void setIsDeploymentInProgress(Boolean isDeploymentInProgress) {
		IsDeploymentInProgress = isDeploymentInProgress;
	}
	
	public static void setMessage(String messageLog){
		message = messageLog;
	}

	public static String getMessage() {
		return message;
	}

	public static void getMessage(String deploymentLog) {
		DeploymentStatus.message = deploymentLog;
	}
	
	public static void clearLog(){
		message = "";
	}
	
}
