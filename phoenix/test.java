import java.sql.*;
import java.util.*;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.phoenix.jdbc.PhoenixDriver;




public class test {
  public static void main(String args[]) throws Exception {
    Connection conn;
    Properties prop = new Properties();
   // Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
    URLClassLoader loader = new URLClassLoader(new URL[] {new URL("file:///tmp/phoenix-4.1.0-bin/hadoop2/phoenix-4.1.0-client-hadoop2.jar")}, null);
    PhoenixDriver driver = new PhoenixDriver();


	conn = driver.connect("jdbc:phoenix:10.0.2.15:2181:/hbase-unsecure",new Properties()); 
    

//conn =  DriverManager.getConnection("jdbc:phoenix:localhost:2181:/hbase-unsecure");
    System.out.println("got connection");
    ResultSet rst = conn.createStatement().executeQuery("select * from drivers");
    while (rst.next()) {
      System.out.println(rst.getString(1) + " " + rst.getString(2));
    }
    conn.commit();
  }
}
