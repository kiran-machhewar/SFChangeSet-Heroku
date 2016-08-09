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
        name = "MyServlet", 
        urlPatterns = {"/hello"}
    )
public class HelloServlet extends HttpServlet {

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
        BufferedWriter fw = new BufferedWriter(new FileWriter(new File("test.txt")));
        fw.write("Now I am able to write file.");
        BufferedReader br = new BufferedReader(new FileReader(new File("test.txt")));
        out.write((""+br.readLine()).getBytes());
        out.write(("hello heroku--> Deployment = "+success).getBytes());
        out.flush();
        out.close();
    }
    
}
