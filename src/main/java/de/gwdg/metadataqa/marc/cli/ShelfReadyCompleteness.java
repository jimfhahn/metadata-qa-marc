package de.gwdg.metadataqa.marc.cli;

import de.gwdg.metadataqa.marc.MarcRecord;
import de.gwdg.metadataqa.marc.analysis.ShelfReadyAnalysis;
import de.gwdg.metadataqa.marc.analysis.ShelfReadyFieldsBooks;
import de.gwdg.metadataqa.marc.cli.parameters.CommonParameters;
import de.gwdg.metadataqa.marc.cli.parameters.ShelfReadyCompletenessParameters;
import de.gwdg.metadataqa.marc.cli.processor.MarcFileProcessor;
import de.gwdg.metadataqa.marc.cli.utils.RecordIterator;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.marc4j.marc.Record;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static de.gwdg.metadataqa.marc.Utils.createRow;
import static de.gwdg.metadataqa.marc.Utils.quote;

/**
 * usage:
 * java -cp target/metadata-qa-marc-0.1-SNAPSHOT-jar-with-dependencies.jar \
 * de.gwdg.metadataqa.marc.cli.ShelfReadyCompleteness [MARC21 file]
 *
 * @author Péter Király <peter.kiraly at gwdg.de>
 */
public class ShelfReadyCompleteness implements MarcFileProcessor, Serializable {

  private static final Logger logger = Logger.getLogger(
    ShelfReadyCompleteness.class.getCanonicalName()
  );
  private final Options options;
  private final boolean readyToProcess;
  private ShelfReadyCompletenessParameters parameters;
  private File output = null;

  public ShelfReadyCompleteness(String[] args) throws ParseException {
    parameters = new ShelfReadyCompletenessParameters(args);
    options = parameters.getOptions();
    readyToProcess = true;
  }

  public static void main(String[] args) throws ParseException {
    MarcFileProcessor processor = null;
    try {
      processor = new ShelfReadyCompleteness(args);
    } catch (ParseException e) {
      System.err.println("ERROR. " + e.getLocalizedMessage());
      processor.printHelp(processor.getParameters().getOptions());
      System.exit(0);
    }

    if (processor.getParameters().getArgs().length < 1) {
      System.err.println("Please provide a MARC file name!");
      System.exit(0);
    }
    if (processor.getParameters().doHelp()) {
      processor.printHelp(processor.getParameters().getOptions());
      System.exit(0);
    }

    RecordIterator iterator = new RecordIterator(processor);
    iterator.start();
  }

  @Override
  public CommonParameters getParameters() {
    return parameters;
  }

  @Override
  public void beforeIteration() {
    logger.info(parameters.formatParameters());
    printFields();

    output = new File(parameters.getOutputDir(), parameters.getFileName());
    if (output.exists())
      output.delete();

    print(createRow(createHeaders()));
  }

  @NotNull
  private List<String> createHeaders() {
    List<String> headers = new ArrayList<>();
    headers.add("id");
    headers.addAll(ShelfReadyAnalysis.getHeaders());
    headers.add("total");
    return headers;
  }

  @Override
  public void fileOpened(Path path) {
  }

  @Override
  public void processRecord(Record marc4jRecord, int recordNumber) {

  }

  @Override
  public void processRecord(MarcRecord marcRecord, int recordNumber) {
    List<Double> scores = ShelfReadyAnalysis.getScores(marcRecord);
    String id = parameters.getTrimId()
              ? marcRecord.getId().trim()
              : marcRecord.getId();

    List<String> scoresToString = new ArrayList<>();
    for (Double score : scores)
      scoresToString.add(String.format("%.2f", score));

    String message = String.format(
      "\"%s\",%s%n",
      id, StringUtils.join(scoresToString, ",")
    );
    print(message);
  }

  @Override
  public void fileProcessed() {

  }

  @Override
  public void afterIteration(int numberOfprocessedRecords) {

  }

  @Override
  public boolean readyToProcess() {
    return readyToProcess;
  }

  public void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    String message = String.format(
      "java -cp metadata-qa-marc.jar %s [options] [file]",
      this.getClass().getCanonicalName()
    );
    formatter.printHelp(message, options);
  }

  private void print(String message) {
    try {
      FileUtils.writeStringToFile(output, message, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void printFields() {
    Path path = Paths.get(parameters.getOutputDir(), "shelf-ready-completeness-fields.csv");
    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      writer.write(createRow("name", "label", "marcpath", "score"));
      for (ShelfReadyFieldsBooks field : ShelfReadyFieldsBooks.values()) {
        try {
          writer.write(createRow(field.name(), quote(field.getLabel()), quote(field.getMarcPath()), field.getScore()));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}