# ddm-starter-validation

### Overview

* Project with configuration for form data validations.

### Usage

1. Specify dependency in your service:

```xml

<dependencies>
  ...
  <dependency>
    <groupId>com.epam.digital.data.platform</groupId>
    <artifactId>ddm-starter-validation</artifactId>
    <version>...</version>
  </dependency>
  ...
</dependencies>
```

2. Auto-configuration should be activated through the `@SpringBootApplication`
  or `@EnableAutoConfiguration` annotation in main class.
3. Inject`com.epam.digital.data.platform.starter.validation.service.FormValidationService` for form data validation.

### Test execution

* Tests could be run via maven command:
  * `mvn verify` OR using appropriate functions of your IDE.
  
### License

The ddm-starter-validation is Open Source software released under
the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).