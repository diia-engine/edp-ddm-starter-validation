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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.exception.SubmissionValidationException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorDetailDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormValidationServiceImplTest {

  @Mock
  private FormValidationClient client;
  @InjectMocks
  private FormValidationServiceImpl formValidationService;

  @Test
  public void testFormDataValidationWithInvalidData() {
    var message = "ValidationError";
    var field = "name";
    var value = "123";
    var errorDetailDto = new ErrorDetailDto(message, field, value);
    var errorListDto = new ErrorsListDto(List.of(errorDetailDto));
    var validationErrorDto = new ValidationErrorDto("traceId", "VALIDATION_ERROR", message,
        errorListDto);
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    doThrow(new SubmissionValidationException(validationErrorDto)).when(client)
        .validateFormData(eq(formId), any());

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError()).isNotNull();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getMessage())
        .isEqualTo(message);
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo(field);
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getValue())
        .isEqualTo(value);
  }

  @Test
  public void testFormDataValidationWithInvalidData2() {
    var formErrorDetailDto = new ErrorDetailDto("ValidationError2", "name", "321");
    var formErrorDetailDto2 = new ErrorDetailDto("ValidationError3", "fileName", "543");
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();

    var errorListDto = new ErrorsListDto(List.of(formErrorDetailDto, formErrorDetailDto2));
    var validationErrorDto = new ValidationErrorDto("traceId", "FORM_VALIDATION_ERROR", "message",
        errorListDto);
    doThrow(new SubmissionValidationException(validationErrorDto)).when(client)
        .validateFormData(eq(formId), any());

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo("name");
  }

  @Test
  public void happyPath() {
    var formValidationResponseDto = formValidationService.validateForm("formId",
        FormDataDto.builder().data(new LinkedHashMap<>()).build());

    assertThat(formValidationResponseDto.isValid()).isTrue();
  }
}
