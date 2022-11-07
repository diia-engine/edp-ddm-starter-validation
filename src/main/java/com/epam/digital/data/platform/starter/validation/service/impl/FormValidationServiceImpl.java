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
import com.epam.digital.data.platform.integration.formprovider.exception.SubmissionValidationException;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;
import com.epam.digital.data.platform.starter.validation.service.FormValidationService;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

  private final FormValidationClient client;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    var formDataValidationDto = FormDataValidationDto.builder().data(formDataDto.getData()).build();
    try {
      client.validateFormData(formId, formDataValidationDto);
      return FormValidationResponseDto.builder().isValid(true).build();
    } catch (SubmissionValidationException ex) {
      return FormValidationResponseDto.builder().isValid(false).error(ex.getErrors()).build();
    }
  }
}
