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

import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.dto.FormDataValidationDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorListDto;
import com.epam.digital.data.platform.integration.formprovider.exception.FormValidationException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.epam.digital.data.platform.starter.validation.service.FormValidationService;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

  private static final String TRACE_ID_KEY = "X-B3-TraceId";

  private final FormValidationClient client;
  private final FormValidationErrorMapper errorMapper;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    var formDataValidationDto = FormDataValidationDto.builder().data(formDataDto.getData()).build();
    try {
      client.validateFormData(formId, formDataValidationDto);
      return FormValidationResponseDto.builder().isValid(true).build();
    } catch (FormValidationException ex) {
      var errorsInvalidFileType = new HashSet<>(ex.getErrors().getErrors());
      return createFailedValidationResponse(errorsInvalidFileType);
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
}
