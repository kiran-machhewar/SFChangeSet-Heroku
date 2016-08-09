package servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(
        name = "CheckDynoServlet", 
        urlPatterns = {"/CheckDyno"}
    )
public class CheckDyno extends HttpServlet {
	public static Integer counter =0;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
    	FileBasedDeployAndRetrieve fbd = new FileBasedDeployAndRetrieve();
    	Boolean success  = false;
    	try {
			fbd.run();
			success = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ServletOutputStream out = resp.getOutputStream();
        int mb = 1024*1024;
        counter++;
        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();
         
        String log = "##### Heap utilization statistics [MB] #####\n";
         
        //Print used memory
        log+="\nUsed Memory:"
            + (runtime.totalMemory() - runtime.freeMemory()) / mb;
 
        //Print free memory
        log+="\nFree Memory:"
            + runtime.freeMemory() / mb;
         
        //Print total available memory
        log+="\nTotal Memory:" + runtime.totalMemory() / mb;
 
        //Print Maximum available memory
        log+="\nMax Memory:" + runtime.maxMemory() / mb;
        log+="Counter-->"+counter;
        out.write(log.getBytes());
        out.flush();
        out.close();
    }
    
}
