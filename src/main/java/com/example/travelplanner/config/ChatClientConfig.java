package com.example.travelplanner.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Builds the application {@link ChatClient} with the travel-planner system
 * prompt applied to every request. The underlying {@code ChatClient.Builder}
 * and OpenAI chat model are auto-configured by Spring AI from
 * {@code spring.ai.openai.*} properties.
 */
@Configuration
public class ChatClientConfig {

	@Bean
	public ChatClient chatClient(ChatClient.Builder builder,
								  @Value("classpath:prompts/system-prompt.st") Resource systemPrompt) {
		return builder
				.defaultSystem(systemPrompt)
				.build();
	}
}
