package de.gwdg.metadataqa.marc.cli;

import de.gwdg.metadataqa.marc.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.gwdg.metadataqa.marc.Utils.createRow;
import static de.gwdg.metadataqa.marc.Utils.createRowWithSep;

public class PairGenerator {
  private static final Logger logger = Logger.getLogger(NetworkAnalysis.class.getCanonicalName());

  private String outputDir;
  private final int groupLimit;
  private boolean asBase36 = false;

  private BufferedWriter pairWriter;
  private BufferedWriter nodeWriter;

  public PairGenerator(String outputDir, int groupLimit, boolean asBase36) {
    this.asBase36 = asBase36;
    this.outputDir = outputDir;
    this.groupLimit = groupLimit;
  }

  protected void generatePairs() {
    logger.info("pairIds");

    createPairs("");
    Path inputPath = Paths.get(outputDir,"network-by-concepts-tags.csv");
    try (Stream<String> stream = Files.lines(Paths.get(inputPath.toString()))) {
      stream.forEach(
        line -> {
          String[] parts = line.split(",");
          if (!parts[0].equals("tag")) {
            logger.info(parts[0]);
            createPairs(parts[0]);
          }
        }
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void createPairs(String tag) {
    if (!tag.equals(""))
      tag = "-" + tag;

    initializePairWriter("network-pairs" + tag + ".csv");
    initializeNodeWriter("network-nodes" + tag + ".csv");
    processConcepts( "network-by-concepts" + tag + ".csv");
    try {
      pairWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      nodeWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void initializePairWriter(String fileName) {
    Path outputPath = Paths.get(outputDir, fileName);
    try {
      pairWriter = Files.newBufferedWriter(outputPath);
      if (asBase36)
        pairWriter.write(createRow("id1", "id2"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void initializeNodeWriter(String fileName) {
    Path nodePath = Paths.get(outputDir, fileName);
    try {
      nodeWriter = Files.newBufferedWriter(nodePath);
      if (asBase36)
        nodeWriter.write(createRow("id1", "id2"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void processConcepts(String fileName) {
    Map<Object, Boolean> nodeTrack = new HashMap<>();
    AtomicInteger lineNr = new AtomicInteger();

    Path inputPath = Paths.get(outputDir, fileName);
    try (Stream<String> stream = Files.lines(Paths.get(inputPath.toString()))) {
      stream.forEach(
        line -> {
          lineNr.getAndIncrement();
          if (lineNr.get() % 100 == 0)
            logger.info("" + lineNr.get());
          String[] parts = line.split(",");
          String[] ids = parts[2].split(";");
          if (ids.length > 1) {
            if (ids.length > groupLimit) {
              logger.info(String.format(
                "%d is greater than %d. The array will will truncated.",
                ids.length, groupLimit));
              ids = Arrays.copyOfRange(ids, 0, groupLimit);
            }

            Object[] encoded = asBase36 ? stringToBase36(ids) : stringToInteger(ids);
            List<String> pairs = makePairs(encoded);
            try {
              for (String pair : pairs) {
                pairWriter.write(pair);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }

            for (Object id : encoded) {
              try {
                if (!nodeTrack.containsKey(id)) {
                  nodeWriter.write(createRow(id, id));
                  nodeTrack.put(id, true);
                }
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }
        }
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      pairWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @NotNull
  private Object[] stringToBase36(String[] ids) {
    Object[] encoded;
    encoded = new String[ids.length];
    for (int i = 0; i < ids.length; i++) {
      encoded[i] = Utils.base36_encode(ids[i]);
    }
    return encoded;
  }

  @NotNull
  private Object[] stringToInteger(String[] ids) {
    Object[] encoded = new Integer[ids.length];
    for (int i = 0; i < ids.length; i++) {
      encoded[i] = Utils.parseId(ids[i]);
    }
    return encoded;
  }

  private List<String> makePairs(Object[] elements) {
    List<String> pairs = new ArrayList<>(elements.length);
    int len = elements.length;
    for (int i = 0; i < (len - 1); i++) {
      for (int j = (i + 1); j < len; j++) {
        if (asBase36) {
          String a = (String) elements[i];
          String b = (String) elements[j];
          if (a.equals(b))
            continue;
          pairs.add(createRow(elements[i], elements[j]));
        } else {
          int a = (int) elements[i];
          int b = (int) elements[j];
          if (a == b)
            continue;
          pairs.add(createRowWithSep(' ', a, b));

        }
      }
    }
    return pairs;
  }
}
