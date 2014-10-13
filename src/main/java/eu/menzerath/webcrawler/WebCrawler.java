package eu.menzerath.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebCrawler {
    private final String ROOT_URL;
    private List<String> internalLinks = new ArrayList<>();
    private List<String> externalLinks = new ArrayList<>();

    public WebCrawler(String url) {
        ROOT_URL = url;
    }

    /**
     * Start crawling and output results on the console and in a xml-file
     */
    public void start() {
        System.out.print("CRAWLING");

        Thread pt = new PointerThread();
        pt.start();
        crawlPage(ROOT_URL); // <-- Start!
        pt.interrupt();

        // Print results
        System.out.println("\n\nINTERNAL LINKS:");
        int i = 1;
        for (String url : internalLinks) {
            System.out.println("[" + i + "] " + url);
            i++;
        }

        System.out.println("\nEXTERNAL LINKS:");
        i = 1;
        for (String url : externalLinks) {
            System.out.println("[" + i + "] " + url);
            i++;
        }

        // Create XML-file
        XMLOutput.createXmlOutput(internalLinks, externalLinks, new File("output.xml"));
    }

    /**
     * Recursive page-crawler
     * @param url Url to crawl
     */
    private void crawlPage(String url) {
        // http://website.com & http://website.com/ are the same page!
        if (internalLinks.contains(url) || internalLinks.contains(url + "/") || internalLinks.contains(url.substring(0, url.length() - 1))) return;
        internalLinks.add(url);

        // Open page at url
        Document doc;
        try {
            doc = Jsoup.connect(url).userAgent(Main.APPLICATION_NAME + "/" + Main.APPLICATION_VERSION + " (" + Main.APPLICATION_URL + ")").get();
        } catch (IOException e) {
            System.out.println("Unable to read Page at [" + url + "]: " + e.getMessage());
            return;
        }

        // Get every link from that page
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            if (linkUrl.startsWith(ROOT_URL)) { // Found an internal link
                if (!linkUrl.matches(Main.noPageRegEx)) { // This link does not end with a "#"

                    // Is there a file?
                    boolean isFile = false;
                    for (String regex : Main.fileRegEx) {
                        if (linkUrl.matches(regex)) {
                            isFile = true;
                            internalLinks.add(linkUrl); // It is a file --> add to list
                        }
                    }

                    // No file --> crawl this page
                    if (!isFile) crawlPage(linkUrl);
                }
            } else { // Found an external link
                // http://website.com & http://website.com/ are the same page!
                if (externalLinks.contains(linkUrl) || externalLinks.contains(linkUrl + "/") || externalLinks.contains(linkUrl.substring(0, linkUrl.length() - 1))) return;
                externalLinks.add(linkUrl);
            }
        }
    }

    /**
     * Thread, which will add every 3 seconds another "." to indicate process
     */
    private class PointerThread extends Thread {
        @Override
        public void run() {
            while (!this.isInterrupted()) {
                System.out.print(".");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                    this.interrupt();
                }
            }
        }
    }
}