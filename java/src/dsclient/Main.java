package dsclient;

import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

class Main {
    public static void main(String[] args)
    throws IOException, ParserConfigurationException, SAXException
    {
        Set<String> argSet = Set.of(args);

        JobScheduler scheduler = new JobScheduler("127.0.0.1", 50000, new AllToLargest());
        scheduler.newlines = argSet.contains("-n");
        scheduler.run();
    }
}
