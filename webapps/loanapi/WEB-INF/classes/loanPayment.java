import java.io.*;
import java.sql.*;
import java.util.Date;
import org.json.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet("/payment/*")   // Configure the request URL for this servlet (Tomcat 7/Servlet 3.0 upwards)
public class loanPayment extends HttpServlet {

    //POST /payment?lid={loanid}&amount={amount} updates the loan details and gets a payment id
    @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
               throws ServletException, IOException {

      String pathInfo = request.getPathInfo();

      // Set the MIME type for the response message
      response.setContentType("application/json");
      // Get a output writer to write the response message into the network socket
      PrintWriter out = response.getWriter();
      Integer pid, lid, amount;
      long millis=System.currentTimeMillis();  
      java.sql.Date date=new java.sql.Date(millis);

      lid = Integer.valueOf(request.getParameter("lid"));
      amount = Integer.valueOf(request.getParameter("amount"));

       if(lid==null || amount==null)
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
            String sqlStr = "INSERT INTO lpayments(L_ID, P_amount, P_date) VALUES("+lid+", "+amount+", '"+date+"')";
            stmt.executeUpdate(sqlStr);
            sqlStr = "Select MAX(PID) from lpayments where L_ID="+lid;
            ResultSet rset = stmt.executeQuery(sqlStr);
            rset.next();
            pid = rset.getInt("MAX(PID)");
            Integer paid;
            paid = 0;
            
            sqlStr = "Select Amount, Paid from loans where LID="+lid;
            rset = stmt.executeQuery(sqlStr);
            rset.next();
            
            paid = rset.getInt("Paid");
            paid += amount;
            sqlStr = "UPDATE loans SET Paid="+paid+" WHERE LID="+lid;
            stmt.executeUpdate(sqlStr);

            String msg = "Payment ID:"+pid;
            JSONArray jsonArray = CDL.rowToJSONArray(new JSONTokener(msg));        
            out.print(jsonArray);
            return;

      } catch(Exception ex) {
         out.println("<p>Error: " + ex.getMessage() + "</p>");
         out.println("<p>Check Loan ID <br> Check Tomcat console for details.</p>");
         ex.printStackTrace();
      }  // Step 5: Close conn and stmt - Done automatically by try-with-resources (JDK 7)
 
      out.close();
   }

}