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
    static double emi_calculator(double p, double r, double t)
    {
        double emi;
     
        r = r / (12 * 100); // one month interest
        t = t * 12; // one month period
        emi = (p * r * (double)Math.pow(1 + r, t))
                / (double)(Math.pow(1 + r, t) - 1);
     
        return (emi);
    }

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
                    + ", " + rset.getDouble("Amount")
                    + ", " + rset.getDouble("Tenure")+" months"
                    + ", " + rset.getString("Desc")
                    + ", " + rset.getDouble("EMI")
                    + ", " + rset.getDouble("Interest")
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
                    + ", " + rset.getDouble("Amount")
                    + ", " + rset.getDouble("Tenure")+" months"
                    + ", " + rset.getString("Desc")
                    + ", " + rset.getDouble("EMI")
                    + ", " + rset.getDouble("Interest")
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

   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException{
      response.setContentType("application/json");

      double amount, emi, interest, lid;
      int tenure;
      String param = request.getParameter("cid");
      Integer cid = (param==null)?null : Integer.valueOf((param.trim()));
      
      // Get a output writer to write the response message into the network socket
      PrintWriter out = response.getWriter();

      if(cid == null)
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
         String sql = "select LID from loans where C_ID="+cid; 
         ResultSet rset = stmt.executeQuery(sql);
         if(rset.next())
         {
            lid = rset.getInt("LID");
            if(lid>0)
            {
               String msg = "You Already Have loan "+lid;
               JSONArray jsonArray = CDL.rowToJSONArray(new JSONTokener(msg));        
               out.print(jsonArray);
               return;
            }
         }
         String desc = request.getParameter("desc");
         amount = Double.valueOf(request.getParameter("amount"));
         interest = Double.valueOf(request.getParameter("interest"));
         double interestp;
         interestp = interest;
         interestp/=(12*100);
         tenure = Integer.valueOf(request.getParameter("tenure"));
         emi= emi_calculator(amount, interestp, tenure);

         long millis=System.currentTimeMillis();  
         java.sql.Date date=new java.sql.Date(millis);
         sql = "INSERT INTO `loans` (`C_ID`, `Amount`, `Tenure`, `Desc`, `EMI`, `Interest`, `start-date`, `Paid`) VALUES ("+cid+", "+amount+", "+ tenure+", "+ desc+", "+ emi+", "+ interest+", '"+date+"', "+ 0+ ")";
         
         stmt.executeUpdate(sql);
         //String msg = "C_ID, Amount, Tenure, Desc, EMI, Interest, start-date, Paid \n";
         //msg= msg+cid+", "+amount+", "+ tenure+", "+ desc+", "+ emi+", "+ interest+", "+date+", "+ 0+"\n";

         sql = "SELECT * from loans where C_ID="+cid;
         rset = stmt.executeQuery(sql);
         String rst="LoanId, CustomerId, Amount, Tenure, Desc, EMI, Interest, start-date \n";
            JSONArray jsonArray = new JSONArray();
            while(rset.next()) {
               rst += rset.getInt("LID")
                    + ", " + rset.getInt("C_ID")
                    + ", " + rset.getInt("Amount")
                    + ", " + rset.getInt("Tenure")+" months"
                    + ", " + rset.getString("Desc")
                    + ", " + rset.getDouble("EMI")
                    + ", " + rset.getInt("Interest")
                    + ", " + rset.getDate("start-date")
                    +" \n";
               jsonArray = CDL.toJSONArray(rst);
            }      
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