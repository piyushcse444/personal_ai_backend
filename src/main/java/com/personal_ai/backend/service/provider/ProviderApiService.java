package com.personal_ai.backend.service.provider;

import com.personal_ai.backend.dto.ProviderApiResponse;
import com.personal_ai.backend.model.Message;
import com.personal_ai.backend.model.Model;
import com.personal_ai.backend.model.ProviderConfig;

import java.util.List;

public interface ProviderApiService {

	/**
	 * Call the provider's chat API and return the assistant's response.
	 *
	 * @param messages the conversation history
	 * @param model the model to use
	 * @param config the provider config with credentials and endpoint
	 * @param thinking whether to enable thinking/reasoning mode (for reasoning-capable models)
	 * @return the API response with content and token counts
	 */
	ProviderApiResponse chat(List<Message> messages, Model model, ProviderConfig config, boolean thinking);
}
