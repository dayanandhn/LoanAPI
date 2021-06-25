import java.io.*;
import java.sql.*;
import java.util.Random;
import java.lang.Math;   
import java.math.BigInteger;
import org.json.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet("/loans/*")   // Configure the request URL for this servlet (Tomcat 7/Servlet 3.0 upwards)
public class loanServlet extends HttpServlet {

   // The doGet() runs once per HTTP GET request to this servlet.
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
            //GET /loans/ gets all the loans from the table
         if(pathInfo == null || pathInfo.equals("/"))
         {   
            String sqlStr = "select * from loans";   
            ResultSet rset = stmt.executeQuery(sqlStr);  // Send the query to the server

            String rst="LoanId, CustomerId, Amount, Tenure, Desc, EMI, Interest, start-date \n";
            JSONArray jsonArray = new JSONArray();
            while(rset.next()) {
               rst += rset.getInt("LID")
                    + ", " + rset.getInt("C_ID")
                    + ", " + rset.getInt("Amount")
                    + ", " + rset.getInt("Tenure")+" months"
                    + ", " + rset.getString("Desc")
                    + ", " + rset.getFloat("EMI")
                    + ", " + rset.getInt("Interest")
                    + ", " + rset.getDate("start-date")
                    +" \n";
               jsonArray = CDL.toJSONArray(rst);
            }
            out.print(jsonArray);
            out.flush();
            return;
         }
            //GET loans/{loanid} form table
         String[] split = pathInfo.split("/");

         int lid = Integer.parseInt(split[1]);
         String sqlStr = "select * from loans where LID="+lid; 
         ResultSet rset = stmt.executeQuery(sqlStr);
         String rst="LoanId, CustomerId, Amount, Tenure, Desc, EMI, Interest, start-date \n";
         int count=0;
         JSONArray jsonArray = new JSONArray();
         while(rset.next()) {
            rst += rset.getInt("LID")
                    + ", " + rset.getInt("C_ID")
                    + ", " + rset.getInt("Amount")
                    + ", " + rset.getInt("Tenure")+" months"
                    + ", " + rset.getString("Desc")
                    + ", " + rset.getFloat("EMI")
                    + ", " + rset.getInt("Interest")
                    + ", " + rset.getDate("start-date")
                    +" \n";
            jsonArray = CDL.toJSONArray(rst);
            count++;
         }
         if(count>0)
         {
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

    
    //DELETE /loans?lid={lid} deletes loan from table
   @Override
   public void doDelete(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException{
      String param = request.getParameter("lid");
      Integer lid = (param==null)?null : Integer.valueOf((param.trim()));
      response.setContentType("application/json");
      // Get a output writer to write the response message into the network socket
      PrintWriter out = response.getWriter();

      if(lid == null)
         throw new RuntimeException(Integer.toString(HttpServletResponse.SC_BAD_REQUEST));
      
      try (
         // Step 1: Allocate a database 'Connection' object
         Connection conn = DriverManager.getConnection(
               "jdbc:mysql://localhost:3306/loanms?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
               "root", "");   // For MySQL
               // The format is: "jdbc:mysql://hostname:port/databaseName", "username", "password"

         // Step 2: Allocate a 'Statement' object in the Connection
         Statement stmt = conn.createStatement();
      ) {
         String sqlStr = "select count(LID) from loans where LID="+lid; 
         ResultSet rset = stmt.executeQuery(sqlStr);
         if(rset.next())
         if(rset.getBoolean("count(LID)"))
         {sqlStr = "delete from loans where lid = "+lid;   
         stmt.executeUpdate(sqlStr);
         String msg = "Deleted "+lid;
         JSONArray jsonArray = CDL.rowToJSONArray(new JSONTokener(msg));        
         out.print(jsonArray);
         return;
         }else{
             response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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