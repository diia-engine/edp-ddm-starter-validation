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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.formprovider.client.FormManagementProviderClient;
import com.epam.digital.data.platform.integration.formprovider.dto.ComponentsDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorListDto;
import com.epam.digital.data.platform.integration.formprovider.dto.NestedComponentDto;
import com.epam.digital.data.platform.integration.formprovider.dto.ValidateComponentDto;
import com.epam.digital.data.platform.integration.formprovider.exception.BadRequestException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorDetailDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
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
  private FormManagementProviderClient client;
  @Mock
  private FormValidationErrorMapper errorMapper;
  @Mock
  private ObjectMapper objectMapper;
  @InjectMocks
  private FormValidationServiceImpl formValidationService;

  @Test
  @SuppressWarnings("unchecked")
  public void testFormDataValidationWithValidData() throws Exception {
    var mockValidFile = new ArrayList<>();
    var validFile = new HashMap<>();
    validFile.put("id", "id");
    validFile.put("checksum", "checksum");
    mockValidFile.add(validFile);
    var expectedData = new LinkedHashMap<String, Object>();
    var expectedNestedData = new LinkedHashMap<String, Object>();
    expectedNestedData.put("specializationDate", "12/31/2021");
    expectedNestedData.put("file1", mockValidFile);
    expectedData.put("specializationEndDate", "01/01/2021");
    expectedData.put("specializationEndDate2", "20210101");
    expectedData.put("file2", mockValidFile);
    expectedData.put("list", List.of(expectedNestedData));
    var expectedFormDataDto = FormDataDto.builder().data(expectedData).build();
    var formId = "testFormId";
    var data = new LinkedHashMap<String, Object>();
    var nestedData = new LinkedHashMap<String, Object>();
    nestedData.put("specializationDate", "2021-12-31");
    nestedData.put("file1", mockValidFile);
    data.put("specializationEndDate", "2021-01-01");
    data.put("specializationEndDate2", "20210101");
    data.put("file2", mockValidFile);
    data.put("list", List.of(nestedData));
    var formDataDto = FormDataDto.builder().data(data).build();
    var nestedComponentsDto = new ArrayList<NestedComponentDto>();
    var nestedComponent = new NestedComponentDto("specializationDate", "day", false,
        new ValidateComponentDto(), null, null);
    var nestedComponent2 = new NestedComponentDto("file1", "file", false,
        new ValidateComponentDto(), null, null);
    nestedComponentsDto.add(nestedComponent);
    nestedComponentsDto.add(nestedComponent2);
    var componentsDto = new ArrayList<ComponentsDto>();
    var component = new ComponentsDto("specializationEndDate", "day", true, nestedComponentsDto,
        null, null, new ValidateComponentDto());
    var component2 = new ComponentsDto("file2", "file", false, nestedComponentsDto,
        null, null, new ValidateComponentDto());
    var component3 = new ComponentsDto("specializationEndDate2", "day", true, null,
        null, null, new ValidateComponentDto());
    componentsDto.add(component);
    componentsDto.add(component2);
    componentsDto.add(component3);
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDto));
    when(objectMapper.writeValueAsString(formDataDto.getData())).thenReturn("{}");
    when(objectMapper.readValue(eq("{}"), (TypeReference<Object>) any()))
        .thenReturn(formDataDto.getData());
    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isTrue();
    assertThat(formValidationResponseDto.getError()).isNull();
    verify(client, times(1)).validateFormData(formId, expectedFormDataDto);
  }

  @Test
  public void testFormDataValidationWithInvalidData() {
    var message = "ValidationError";
    var field = "name";
    var value = "123";
    var formErrorDetailDto = new FormErrorDetailDto(message, field, value);
    var errorDetailDto = new ErrorDetailDto(message, field, value);
    var formErrorListDto = new FormErrorListDto(List.of(formErrorDetailDto));
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(eq(formId), any());
    when(client.getForm(formId)).thenReturn(new FormDto(new ArrayList<>()));
    when(errorMapper.toErrorListDto(formErrorListDto)).thenReturn(errorsListDto);

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
    var formErrorDetailDto = new FormErrorDetailDto("ValidationError2", "name", "321");
    var formErrorDetailDto2 = new FormErrorDetailDto("ValidationError3", "fileName", "543");
    var formErrorListDto = new FormErrorListDto(
        List.of(formErrorDetailDto, formErrorDetailDto2));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    var nestedComponent = new NestedComponentDto("fullName", "textfield", false,
        new ValidateComponentDto(), null, null);
    var nestedComponent2 = new NestedComponentDto("fileName", "file", false,
        new ValidateComponentDto(), null, null);
    var componentsDtos = List
        .of(new ComponentsDto("name", "textfield", false, null, null, null,
                new ValidateComponentDto()),
            new ComponentsDto("fileName", "file", false, List.of(nestedComponent, nestedComponent2),
                null, null, new ValidateComponentDto()));
    var errorDetailDto = new ErrorDetailDto("ValidationError", "name", "321");
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto));
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(eq(formId), any());
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));
    when(errorMapper.toErrorListDto(any())).thenReturn(errorsListDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo("name");
  }

  @Test
  public void testValidationFileTypeFormComponents() {
    var formId = "testFormId";
    var mockValidFile = new ArrayList<>();
    var validFile = new HashMap<>();
    validFile.put("id", "id");
    validFile.put("checksum", "checksum");
    mockValidFile.add(validFile);
    var mockInvalidFile = new ArrayList<>();
    var invalidFile = new HashMap<>();
    invalidFile.put("id", "id");
    mockInvalidFile.add(invalidFile);
    var data = new LinkedHashMap<String, Object>();
    var nestedData = new LinkedHashMap<String, Object>();
    nestedData.put("file1", mockInvalidFile);
    nestedData.put("file5", List.of("string"));
    data.put("file2", mockValidFile);
    data.put("file3", null);
    data.put("file4", mockValidFile);
    data.put("list", List.of(nestedData));
    var formDataDto = FormDataDto.builder().data(data).build();
    var nestedComponent = new NestedComponentDto("file1", "file", false,
        new ValidateComponentDto("Required field"), null, null);
    var nestedComponent2 = new NestedComponentDto("file5", "file", false,
        new ValidateComponentDto("Required field"), null, null);
    var componentsDtos = List
        .of(new ComponentsDto("file3", "file", false, null, null, null,
                new ValidateComponentDto("Required field")),
            new ComponentsDto("file4", "file", false, null, null, null,
                new ValidateComponentDto("Required field")),
            new ComponentsDto("file2", "file", false, List.of(nestedComponent, nestedComponent2),
                null, null, new ValidateComponentDto("Required field")));
    var errorDetailDto = new ErrorDetailDto("Required field", "file1", null);
    var errorDetailDto2 = new ErrorDetailDto("Required field", "file3", null);
    var errorDetailDto3 = new ErrorDetailDto("Required field", "file5", null);
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto, errorDetailDto2, errorDetailDto3));
    when(client.validateFormData(eq(formId), any())).thenReturn(formDataDto);
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));
    when(errorMapper.toErrorListDto(any())).thenReturn(errorsListDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo("file1");
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(1).getField())
        .isEqualTo("file3");
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(2).getField())
        .isEqualTo("file5");
  }
}
