package server;

import servlets.Servlet;

public interface HTTPServer extends Runnable {
    public void addServlet(String httpCommand, String uri, Servlet s);
    public void removeServlet(String httpCommand, String uri);
    public void start();
    public void close();
}
