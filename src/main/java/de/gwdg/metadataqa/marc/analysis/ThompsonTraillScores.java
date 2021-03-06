package de.gwdg.metadataqa.marc.analysis;

import de.gwdg.metadataqa.marc.Utils;

import java.util.*;

public class ThompsonTraillScores {
  private Map<ThompsonTraillFields, Integer> scores;

  public ThompsonTraillScores() {
    scores = new LinkedHashMap<>();
    for (ThompsonTraillFields field : ThompsonTraillFields.values()) {
      if (!field.equals(ThompsonTraillFields.ID)) {
        scores.put(field, 0);
      }
    }
  }

  public void count(ThompsonTraillFields key) {
    Utils.count(key, scores);
  }

  public void set(ThompsonTraillFields key, int value) {
    scores.put(key, value);
  }

  public void calculateTotal() {
    int total = 0;
    for (Map.Entry<ThompsonTraillFields, Integer> entry : scores.entrySet()) {
      ThompsonTraillFields field = entry.getKey();
      if (!field.equals(ThompsonTraillFields.TOTAL)) {
        if (field.isClassification())
          total += Math.min(entry.getValue(), 10);
        else
          total += entry.getValue();
      }
    }
    set(ThompsonTraillFields.TOTAL, total);
  }

  public List<Integer> asList() {
    List<Integer> list = new ArrayList<>();
    for (Map.Entry<ThompsonTraillFields, Integer> entry : scores.entrySet()) {
      ThompsonTraillFields field = entry.getKey();
      if (field.isClassification())
        list.add(Math.min(entry.getValue(), 10));
      else
        list.add(entry.getValue());
    }
    return list;
  }
}
