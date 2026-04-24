package com.example.cae.common.config;

import com.example.cae.common.constant.QueryValidationConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
	private static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern(QueryValidationConstants.STANDARD_DATE_TIME_PATTERN);

	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(STANDARD_DATE_TIME_FORMATTER));
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(STANDARD_DATE_TIME_FORMATTER));
		return Jackson2ObjectMapperBuilder.json()
				.modules(javaTimeModule)
				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.simpleDateFormat(QueryValidationConstants.STANDARD_DATE_TIME_PATTERN)
				.build();
	}
}
