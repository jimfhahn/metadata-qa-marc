package de.gwdg.metadataqa.marc;

import de.gwdg.metadataqa.marc.definition.*;
import de.gwdg.metadataqa.marc.definition.general.Linkage;
import de.gwdg.metadataqa.marc.definition.general.parser.ParserException;
import de.gwdg.metadataqa.marc.definition.general.parser.SubfieldContentParser;
import de.gwdg.metadataqa.marc.definition.general.validator.SubfieldValidator;
import de.gwdg.metadataqa.marc.model.validation.ValidationError;
import de.gwdg.metadataqa.marc.model.validation.ValidationErrorType;
import de.gwdg.metadataqa.marc.utils.keygenerator.DataFieldKeyGenerator;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

public class MarcSubfield implements Validatable, Serializable {

  private static final Logger logger = Logger.getLogger(MarcSubfield.class.getCanonicalName());

  private MarcRecord record;
  private DataField field;
  private SubfieldDefinition definition;
  private String code;
  private String value;
  private String codeForIndex = null;
  private List<ValidationError> validationErrors = null;
  private Linkage linkage;
  private String referencePath;

  public MarcSubfield(SubfieldDefinition definition, String code, String value) {
    this.definition = definition;
    this.code = code;
    this.value = value;
  }

  public String getCode() {
    return code;
  }

  public String getValue() {
    return value;
  }

  public DataField getField() {
    return field;
  }

  public void setField(DataField field) {
    this.field = field;
  }

  public Linkage getLinkage() {
    return linkage;
  }

  public void setLinkage(Linkage linkage) {
    this.linkage = linkage;
  }

  public String getReferencePath() {
    return referencePath;
  }

  public void setReferencePath(String referencePath) {
    this.referencePath = referencePath;
  }

  public String getLabel() {
    String label = code;
    if (definition != null && definition.getLabel() != null)
      label = definition.getLabel();
    return label;
  }

  public String resolve() {
    if (definition == null)
      return value;

    return definition.resolve(value);
  }

  public SubfieldDefinition getDefinition() {
    return definition;
  }

  public void setDefinition(SubfieldDefinition definition) {
    this.definition = definition;
  }

  public MarcRecord getRecord() {
    return record;
  }

  public void setRecord(MarcRecord record) {
    this.record = record;
  }

  public String getCodeForIndex() {
    if (codeForIndex == null) {
      codeForIndex = "_" + code;
      if (definition != null && definition.getCodeForIndex() != null) {
        codeForIndex = definition.getCodeForIndex();
      }
    }
    return codeForIndex;
  }

  public Map<String, String> parseContent() {
    if (definition.hasContentParser())
      try {
        return definition.getContentParser().parse(value);
      } catch (ParserException e) {
        String msg = String.format(
          "Error in record: '%s' %s$%s: '%s'. Error message: '%s'",
          record.getId(), field.getTag(), definition.getCode(), value, e.getMessage()
        );
        logger.severe(msg);
        // e.printStackTrace();
      }
    return null;
  }

  public Map<String, List<String>> getKeyValuePairs(DataFieldKeyGenerator keyGenerator) {
    Map<String, List<String>> pairs = new HashMap<>();
    String prefix = keyGenerator.forSubfield(this);

    pairs.put(prefix, Arrays.asList(resolve()));
    if (getDefinition() != null) {
      getKeyValuePairsForPositionalSubfields(pairs, prefix);
      getKeyValuePairsFromContentParser(keyGenerator, pairs);
    }

    return pairs;
  }

  private void getKeyValuePairsFromContentParser(DataFieldKeyGenerator keyGenerator, Map<String, List<String>> pairs) {
    if (getDefinition().hasContentParser()) {
      Map<String, String> extra = parseContent();
      if (extra != null) {
        for (String key : extra.keySet()) {
          pairs.put(
            keyGenerator.forSubfield(this, key),
            Arrays.asList(extra.get(key))
          );
        }
      }
    }
  }

  private void getKeyValuePairsForPositionalSubfields(Map<String, List<String>> pairs, String prefix) {
    if (getDefinition().hasPositions()) {
      Map<String, String> extra = getDefinition().resolvePositional(getValue());
      for (String key : extra.keySet()) {
        pairs.put(prefix + "_" + key, Arrays.asList(extra.get(key)));
      }
    }
  }


  @Override
  public boolean validate(MarcVersion marcVersion) {
    boolean isValid = true;
    validationErrors = new ArrayList<>();

    if (definition == null) {
      validationErrors.add(new ValidationError(record.getId(), field.getDefinition().getTag(),
        ValidationErrorType.SUBFIELD_UNDEFINED, code, field.getDefinition().getDescriptionUrl()));
      isValid = false;
    } else {
      if (code == null) {
        validationErrors.add(new ValidationError(record.getId(), field.getDefinition().getTag(),
          ValidationErrorType.SUBFIELD_NULL_CODE, code, field.getDefinition().getDescriptionUrl()));
        isValid = false;
      } else {
        if (definition.hasValidator()) {
          if (!validateWithValidator())
            isValid = false;
        } else if (definition.hasContentParser()) {
          if (!validateWithParser())
            isValid = false;
        } else if (definition.getCodes() != null && definition.getCode(value) == null) {
          String message = value;
          if (referencePath != null) {
            message += String.format(" (the field is embedded in %s)", referencePath);
          }
          String path = (referencePath == null ? definition.getPath() : referencePath + "->" + definition.getPath());
          validationErrors.add(
            new ValidationError(
              record.getId(),
              path,
              ValidationErrorType.SUBFIELD_INVALID_VALUE,
              message,
              definition.getParent().getDescriptionUrl()
            )
          );
          isValid = false;
        }
      }
    }

    return isValid;
  }

  private boolean validateWithValidator() {
    boolean isValid = true;
    SubfieldValidator validator = definition.getValidator();
    ValidatorResponse response = validator.isValid(this);
    if (!response.isValid()) {
      validationErrors.addAll(response.getValidationErrors());
      isValid = false;
    }
    return isValid;
  }

  private boolean validateWithParser() {
    boolean isValid = true;
    SubfieldContentParser parser = definition.getContentParser();
    try {
      parser.parse(getValue());
    } catch (ParserException e) {
      validationErrors.add(
        new ValidationError(
          record.getId(),
          definition.getPath(),
          ValidationErrorType.SUBFIELD_UNPARSABLE_CONTENT,
          e.getMessage(),
          definition.getParent().getDescriptionUrl()
      ));
      isValid = false;
    }
    return isValid;
  }

  @Override
  public List<ValidationError> getValidationErrors() {
    return validationErrors;
  }

  @Override
  public String toString() {
    return "MarcSubfield{" +
            "code='" + code + '\'' +
            ", value='" + value + '\'' +
            '}';
  }
}
