import java.io.*;
import java.sql.*;
import java.util.Random;
import java.lang.Math;   
import java.math.BigInteger;
import org.json.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet("/customers/*")   // Configure the request URL for this servlet (Tomcat 7/Servlet 3.0 upwards)
public class customers extends HttpServlet {

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
         //GET /customers fetches all customers
         if(pathInfo == null || pathInfo.equals("/"))
         {   
            String sqlStr = "select CID, Name, Email, Salary from customers";   
            ResultSet rset = stmt.executeQuery(sqlStr);  // Send the query to the server

            String rst="CID, Name, Email, Salary \n";
            JSONArray jsonArray = new JSONArray();
            while(rset.next()) {
               rst += rset.getInt("CID")
                  + ", " + rset.getString("Name")
                  + ", " + rset.getString("Email")
                  + ", " + rset.getInt("Salary") +" \n";
               jsonArray = CDL.toJSONArray(rst);
            }
            out.print(jsonArray);
            out.flush();
            return;
         }

         //GET /customers/{CID} fetched customer with ID CID
         String[] split = pathInfo.split("/");

         int cid = Integer.parseInt(split[1]);
         String sqlStr = "select CID, Name, Email, Salary from customers where CID="+cid; 
         ResultSet rset = stmt.executeQuery(sqlStr);
         String rst="CID, Name, Email, Salary \n";
         int count=0;
         JSONArray jsonArray = new JSONArray();
         while(rset.next()) {
            rst += rset.getInt("CID")
                  + ", " + rset.getString("Name")
                  + ", " + rset.getString("Email")
                  + ", " + rset.getInt("Salary") +" \n";
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

   //DELETE /customer?cid={cid} delete customer with CID
   @Override
   public void doDelete(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException{
      String param = request.getParameter("cid");
      Integer cid = (param==null)?null : Integer.valueOf((param.trim()));
      response.setContentType("application/json");
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
         String sqlStr = "select count(CID) from customers where CID="+cid; 
         ResultSet rset = stmt.executeQuery(sqlStr);
         if(rset.next())
         if(rset.getBoolean("count(CID)"))
         {  sqlStr = "delete from customers where cid = "+cid;   
            String msg = "Deleted "+cid;
            JSONArray jsonArray = CDL.rowToJSONArray(new JSONTokener(msg));        
            out.print(jsonArray);
            return;
         }
         else
         {
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

   //POST /customers?name={name}&address={address}&phone={phone}&email={email}&salary={salary} creates a new customer
   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException{
      response.setContentType("application/json");
      // Get a output writer to write the response message into the network socket
      PrintWriter out = response.getWriter();

      Integer cid, salary;
      BigInteger phone;
      String name, address, email;

      //cid = request.getParameter("cid");
      salary = Integer.valueOf(request.getParameter("salary"));
      phone =  new BigInteger(request.getParameter("phone"));
      name = request.getParameter("name");
      address = request.getParameter("address");
      email = request.getParameter("email");

       if (salary == null || phone==null ||name==null ||
            address==null || email==null)
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
         String sqlStr = "INSERT INTO customers(name, address, phone, email, salary) VALUES('"+ name +"', '"+ address +"', "+ phone +", '"+ email +"', "+ salary +" )";   
         stmt.executeUpdate(sqlStr);
         sqlStr = "Select CID from customers where Email='"+email+"'";
         ResultSet rset = stmt.executeQuery(sqlStr);
         rset.next();
         cid = rset.getInt("CID");
         String msg = "Inserted "+cid;
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