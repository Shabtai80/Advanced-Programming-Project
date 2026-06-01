import server.HTTPServer;
import server.MyHTTPServer;
import servlets.ConfLoader;
import servlets.GraphRefresh;
import servlets.HtmlLoader;
import servlets.TopicDisplayer;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting server...");

        HTTPServer server = new MyHTTPServer(8080, 5);

        server.addServlet("GET", "/app/", new HtmlLoader("html_files"));
        server.addServlet("GET", "/publish", new TopicDisplayer());
        server.addServlet("GET", "/refresh", new GraphRefresh());
        server.addServlet("POST", "/upload", new ConfLoader());

        server.start();

        System.out.println("Server started on http://localhost:8080/app/index.html");
        System.out.println("Topic test: http://localhost:8080/publish?topic=A&message=5");
        System.out.println("Press Enter to stop the server.");

        System.in.read();

        server.close();
        System.out.println("done");
    }
}
