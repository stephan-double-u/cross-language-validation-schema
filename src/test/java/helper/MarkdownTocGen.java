package helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates TOC for/from README.md (github flavor)
 */
public class MarkdownTocGen {
    public static void main(String[] args) throws IOException {
        Path mdPath = Paths.get("README.MD");
        BufferedReader mdReader = Files.newBufferedReader(mdPath);
        mdReader.lines()
                .filter(line -> line.startsWith("#"))
                .forEach(heading -> buildLink(heading));
    }

    private static String spaces = "                                                                                  ";
    private static void buildLink(String heading) {
        int level = heading.split(" ")[0].length() - 1;
        String text = heading.substring(level + 2);
        String link = text.toLowerCase().replaceAll("[;_]", "").replace(" ", "-");
        String indent = level == 0 ? "" : spaces.substring(0, level * 2);
        System.out.println(indent + "- [" + text + "](#" + link + ")");
    }
}
