package server;


import config.Configuration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;


public class Server {
    public static final int PORT = Integer.parseInt(Configuration.get("server.port"));


    public static void main(String[] args) throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(new QueuedThreadPool(32, 16));

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setPort(PORT);
        connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setRequestHeaderSize(1024 * 1024 * 10);
        server.addConnector(connector);

        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath("/");
        ctx.setResourceBase("./web");

        // support JSP in embedded mode
        ctx.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/[^/]*jstl.*\\.jar$");
        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        // wrap with GZIP handler
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setHandler(ctx);

        server.setHandler(gzipHandler);
        server.start();
        server.join();
    }

}
