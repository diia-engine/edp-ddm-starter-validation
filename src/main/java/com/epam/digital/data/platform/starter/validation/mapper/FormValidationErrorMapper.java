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

package com.epam.digital.data.platform.starter.validation.mapper;

import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorListDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * The interface represents a mapper for errors. The methods are implemented using the MapStruct.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormValidationErrorMapper {

  /**
   * Method for converting validation form errors.
   *
   * @param formErrorListDto list of {@link FormErrorListDto} entities.
   * @return list of {@link ErrorsListDto} entities.
   */
  ErrorsListDto toErrorListDto(FormErrorListDto formErrorListDto);
}
