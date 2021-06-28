import java.io.*;
import java.sql.*;
import java.util.Random;
import java.lang.Math;   
import java.math.BigInteger;
import org.json.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet("/EMI/*")   // Configure the request URL for this servlet (Tomcat 7/Servlet 3.0 upwards)

public class getEMI extends HttpServlet {

   // The doGet() runs once per HTTP GET request to this servlet.
   //GET /loan/total fetches the number of loans along with highest loan amount and total amount of loan in the company 
   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
               throws ServletException, IOException {

      String pathInfo = request.getPathInfo();

      // Set the MIME type for the response message
      response.setContentType("application/json");
      // Get a output writer to write the response message into the network socket
      PrintWriter out = response.getWriter();
      
      try (
         // Step 1: Allocate a database 'Connection' object
         Connection conn = DriverManager.getConnection(
               "jdbc:mysql://localhost:3306/loanms?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
               "root", "");   // For MySQL
               // The format is: "jdbc:mysql://hostname:port/databaseName", "username", "password"

         // Step 2: Allocate a 'Statement' object in the Connection
         Statement stmt = conn.createStatement();
      ) {

          double amount, emi, interest;
          int tenure;
          amount = Double.valueOf(request.getParameter("amount"));
          interest = Double.valueOf(request.getParameter("interest"));
           interest/=(12*100);
          tenure = Integer.valueOf(request.getParameter("tenure"));
          emi= (amount*interest*Math.pow(1+interest,tenure))/(Math.pow(1+interest,tenure)-1);
        String msg = "EMI :"+emi;
         JSONArray jsonArray = CDL.rowToJSONArray(new JSONTokener(msg));        
         out.print(jsonArray);
          return;

      } catch(Exception ex) {
         out.println("<p>Error: " + ex.getMessage() + "</p>");
         out.println("<p>Check Tomcat console for details.</p>");
         ex.printStackTrace();
      }  // Step 5: Close conn and stmt - Done automatically by try-with-resources (JDK 7)
 
      out.close();
   }


}