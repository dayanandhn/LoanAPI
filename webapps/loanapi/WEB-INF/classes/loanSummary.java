import java.io.*;
import java.sql.*;
import java.util.Random;
import java.lang.Math;   
import java.math.BigInteger;
import org.json.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet("/loan/summary/*")   // Configure the request URL for this servlet (Tomcat 7/Servlet 3.0 upwards)
public class loanSummary extends HttpServlet {

   // The doGet() runs once per HTTP GET request to this servlet.
   //GET /loan/summary/{loanid} fetched the loan details along with the EMI payment made 
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

         String[] split = pathInfo.split("/");

         int lid = Integer.parseInt(split[1]);
         String sqlStr = "select LID, C_ID, Amount, Tenure, EMI, Interest, Paid from loans where LID="+lid; 
         ResultSet rset = stmt.executeQuery(sqlStr);
         String rst="LID, CID, Amount, Tenure, EMI, Interest, Paid \n";
         int count=0;
         JSONArray jsonArray = new JSONArray();
         while(rset.next()) {
            rst += rset.getInt("LID")
                  + ", " + rset.getInt("C_ID")
                  + ", " + rset.getInt("Amount")
                  + ", " + rset.getInt("Tenure")
                  + " months, " + rset.getInt("EMI") 
                  + ", " + rset.getInt("Interest")
                  + ", " + rset.getInt("Paid")
                  +" \n";
            jsonArray = CDL.toJSONArray(rst);
            count++;
         }
         if(count==1)
         {
             JSONArray jsonArray1 = new JSONArray();
            sqlStr = "select * from lpayments where L_ID LIKE "+lid; 
            rset = stmt.executeQuery(sqlStr);

            String rstr = "PaymentID, LoanID, Amount, Date \n";
            while(rset.next()) {
            rstr += rset.getInt("PID")
                  + ", " + rset.getInt("L_ID")
                  + ", " + rset.getInt("P_amount")
                  + ", " + rset.getDate("P_date")
                  +" \n";
            jsonArray1 = CDL.toJSONArray(rstr);
         }
            jsonArray.putAll(jsonArray1);
            out.print(jsonArray);
            out.flush();
            return;
         }
         else{
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
         }

      } catch(Exception ex) {
         out.println("<p>Error: " + ex.getMessage() + "</p>");
         out.println("<p>Check Tomcat console for details.</p>");
         ex.printStackTrace();
      }  // Step 5: Close conn and stmt - Done automatically by try-with-resources (JDK 7)
 
      out.close();
   }


}