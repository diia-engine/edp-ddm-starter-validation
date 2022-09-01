/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.starter.validation.service.impl;

import com.epam.digital.data.platform.integration.formprovider.client.FormManagementProviderClient;
import com.epam.digital.data.platform.integration.formprovider.dto.ComponentsDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorListDto;
import com.epam.digital.data.platform.integration.formprovider.exception.BadRequestException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.validation.exception.CopyFormDataException;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;
import com.epam.digital.data.platform.starter.validation.dto.enums.FileType;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.epam.digital.data.platform.starter.validation.service.FormValidationService;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

  private static final String TRACE_ID_KEY = "X-B3-TraceId";
  private static final String DAY_TYPE = "day";
  private static final String DATE_FORMAT = "%s/%s/%s";
  private static final String DATE_SEPARATOR = "-";
  private static final int YEAR_INDEX = 0;
  private static final int MONTH_INDEX = 1;
  private static final int DAY_INDEX = 2;
  private static final String DATE_FORMAT_REGEX = "(.+-.+-.+)";
  private static final String FILE_VALUE_ID = "id";
  private static final String FILE_VALUE_CHECKSUM = "checksum";

  private final FormManagementProviderClient client;
  private final FormValidationErrorMapper errorMapper;
  private final ObjectMapper objectMapper;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    var form = client.getForm(formId);
    var dayComponents = getDayTypeComponents(form.getComponents());
    var dataCopy = getCopyFormData(formDataDto);
    changeDateFormat(dataCopy, dayComponents);
    var errorsInvalidFileType = getErrorsInvalidFileType(form, formDataDto);
    try {
      client.validateFormData(formId, FormDataDto.builder().data(dataCopy).build());

      if (errorsInvalidFileType.isEmpty()) {
        return FormValidationResponseDto.builder().isValid(true).error(null).build();
      } else {
        return createFailedValidationResponse(errorsInvalidFileType);
      }
    } catch (BadRequestException ex) {
      errorsInvalidFileType.addAll(ex.getErrors().getErrors());
      return createFailedValidationResponse(errorsInvalidFileType);
    }
  }

  private LinkedHashMap<String, Object> getCopyFormData(FormDataDto formDataDto) {
    try {
      var stringFormData = objectMapper.writeValueAsString(formDataDto.getData());
      return objectMapper.readValue(stringFormData, new TypeReference<>() {
      });
    } catch (JsonProcessingException ex) {
      throw new CopyFormDataException("Error while copying form data");
    }
  }

  /**
   * Method to change date format of all day components to FormIO format
   *
   * @param formData      user form data
   * @param dayComponents day components of the user form
   */
  @SuppressWarnings("unchecked")
  private void changeDateFormat(Map<String, Object> formData, Map<String, Boolean> dayComponents) {
    if (Objects.isNull(formData)) {
      return;
    }
    for (var dataEntry : formData.entrySet()) {
      var key = dataEntry.getKey();
      var value = dataEntry.getValue();
      if (value instanceof String && dayComponents.containsKey(key)) {
        formData.put(key, convertDateToFormIOFormat((String) value, dayComponents.get(key)));
      } else if (value instanceof List) {
        var list = (List<Object>) value;
        for (Object nestedObj : list) {
          if (nestedObj instanceof Map) {
            var nestedMap = (Map<String, Object>) nestedObj;
            changeDateFormat(nestedMap, dayComponents);
          }
        }
      }
    }
  }

  private FormValidationResponseDto createFailedValidationResponse(Set<FormErrorDetailDto> err) {
    var errorsListDto = errorMapper.toErrorListDto(new FormErrorListDto(List.copyOf(err)));
    var error = ValidationErrorDto.builder()
        .traceId(MDC.get(TRACE_ID_KEY))
        .code("FORM_VALIDATION_ERROR")
        .message("Form validation error")
        .details(errorsListDto)
        .build();
    return FormValidationResponseDto.builder().isValid(false).error(error).build();
  }

  /**
   * Return a map that contain key of day component and boolean value describing the format of the
   * date construction
   *
   * @param formComponents all form components
   * @return map of day components
   */
  private Map<String, Boolean> getDayTypeComponents(List<ComponentsDto> formComponents) {
    var dayComponents = new HashMap<String, Boolean>();
    for (var component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (var nestedComponent : nestedComponents) {
          if (DAY_TYPE.equals(nestedComponent.getType())) {
            dayComponents.put(nestedComponent.getKey(), nestedComponent.getDayFirst());
          }
        }
      }
      if (DAY_TYPE.equals(component.getType())) {
        dayComponents.put(component.getKey(), component.getDayFirst());
      }
    }
    return dayComponents;
  }

  /**
   * Convert date to FormIO format
   * <p>
   * Input date format : 'yyyy-dd-MM', output date format : if dayFirst equal true 'dd/MM/yyyy',
   * else 'MM/dd/yyyy'
   *
   * @param date     string date to convert
   * @param dayFirst defines the place of the day in the date
   * @return converted string date
   */
  private String convertDateToFormIOFormat(String date, boolean dayFirst) {
    if (Objects.nonNull(date) && date.matches(DATE_FORMAT_REGEX)) {
      var dateArray = date.split(DATE_SEPARATOR);
      return dayFirst ? createDate(DAY_INDEX, MONTH_INDEX, dateArray)
          : createDate(MONTH_INDEX, DAY_INDEX, dateArray);
    }
    return date;
  }

  private String createDate(int firstIndex, int secondIndex, String[] dateArr) {
    return String
        .format(DATE_FORMAT, dateArr[firstIndex], dateArr[secondIndex], dateArr[YEAR_INDEX]);
  }

  /**
   * Check form components and user form data and get a list of errors that describe invalid files
   *
   * @param form     representation of user form
   * @param formData user form data
   * @return errors describing invalid file components
   */
  private Set<FormErrorDetailDto> getErrorsInvalidFileType(FormDto form, FormDataDto formData) {
    var errors = new HashSet<FormErrorDetailDto>();
    var fileKeysWithErrorMessages = getFileKeysWithCustomErrorMessages(form.getComponents());
    var invalidFileComponents = getInvalidFileComponents(formData.getData(),
        fileKeysWithErrorMessages.keySet());

    invalidFileComponents.entrySet().stream()
        .filter(fileComponent -> Objects.nonNull(fileComponent.getValue()))
        .forEach(fileComponent -> {
          var key = fileComponent.getKey();
          var stringValue = fileComponent.getValue().toString();
          var formErrorDetailDto = new FormErrorDetailDto(fileKeysWithErrorMessages.get(key), key,
              stringValue);
          errors.add(formErrorDetailDto);
        });
    return errors;
  }

  /**
   * Check form data and get invalid file components
   *
   * @param formData data from user form
   * @return map of invalid file components
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getInvalidFileComponents(Map<String, Object> formData,
      Set<String> fileKeys) {
    var invalidFileComponents = new HashMap<String, Object>();
    if (Objects.isNull(formData)) {
      return invalidFileComponents;
    }
    for (var dataEntry : formData.entrySet()) {
      var key = dataEntry.getKey();
      var value = dataEntry.getValue();
      if (fileKeys.contains(key) && !isValidFileValues(value)) {
        invalidFileComponents.put(key, value);
      } else if (value instanceof List) {
        var list = (List<Object>) value;
        for (Object nestedObj : list) {
          if (nestedObj instanceof Map) {
            var nestedMap = (Map<String, Object>) nestedObj;
            invalidFileComponents.putAll(getInvalidFileComponents(nestedMap, fileKeys));
          }
        }
      }
    }
    return invalidFileComponents;
  }

  /**
   * Checks if the list of file values is in a valid format
   * <p>
   * The values must be in the format: [ { "id":"string", "checksum":"string" }, ... ]
   *
   * @param fileValues list of file values
   * @return true if the file values is in a valid format
   */
  @SuppressWarnings("unchecked")
  private boolean isValidFileValues(Object fileValues) {
    if (fileValues instanceof List) {
      var fileList = (List<Object>) fileValues;
      for (Object value : fileList) {
        if (!isValidFileValue(value)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Checks if the file value is in a valid format
   * <p>
   * The value must be in the format: { "id":"string", "checksum":"string" }
   *
   * @param fileValue file value
   * @return true if the file value is in a valid format
   */
  @SuppressWarnings("unchecked")
  private boolean isValidFileValue(Object fileValue) {
    if (fileValue instanceof Map) {
      var fileList = (Map<String, String>) fileValue;
      var id = fileList.get(FILE_VALUE_ID);
      var checksum = fileList.get(FILE_VALUE_CHECKSUM);
      return isNotBlankString(id) && isNotBlankString(checksum);
    } else {
      return false;
    }
  }

  boolean isNotBlankString(String string) {
    return string != null && !string.isBlank();
  }

  /**
   * Retrieving file keys with custom error messages form user form
   *
   * @param formComponents form components
   * @return map of file keys with custom error messages
   */
  private Map<String, String> getFileKeysWithCustomErrorMessages(
      List<ComponentsDto> formComponents) {
    var fileKeys = new HashMap<String, String>();
    for (var component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (var nestedComponent : nestedComponents) {
          var validateComponent = nestedComponent.getValidate();
          if (FileType.isValidFileType(nestedComponent.getType()) && Objects
              .nonNull(validateComponent)) {
            fileKeys.put(nestedComponent.getKey(), validateComponent.getCustomMessage());
          }
        }
      }
      var validateComponent = component.getValidate();
      if (FileType.isValidFileType(component.getType()) && Objects.nonNull(validateComponent)) {
        fileKeys.put(component.getKey(), validateComponent.getCustomMessage());
      }
    }
    return fileKeys;
  }
}
