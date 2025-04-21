package cn.com.edtechhub.workmultilingualai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Configuration
class ChatClientConfig {

    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatClient) {
        return ChatClient.builder(ollamaChatClient).build();
    }
}

@RestController
public class ChatController {

    private final ChatClient chatClient;

    @Autowired
    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/ai/generate")
    public Map<String, String> generate(@RequestParam(value = "message", defaultValue = "你叫什么") String message) {
        String result = chatClient.prompt()
                .user(message)
                .call()
                .content();

        if (result != null) {
            return Map.of("generation", result);
        }
        return Map.of();
    }

}

//package cn.com.edtechhub.workmultilingualai;
//
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.ai.ollama.OllamaChatModel;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Flux;
//
//import java.util.Map;
//
//@RestController
//public class ChatController {
//
//    private final OllamaChatModel chatModel;
//
//    @Autowired
//    public ChatController(OllamaChatModel chatModel) {
//        this.chatModel = chatModel;
//    }
//
//    @GetMapping("/ai/generate")
//    public Map<String,String> generate(@RequestParam(value = "message", defaultValue = "你叫什么") String message) {
//        return Map.of("generation", this.chatModel.call(message));
//    }
//
//    @GetMapping("/ai/generateStream")
//    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
//        Prompt prompt = new Prompt(new UserMessage(message));
//        return this.chatModel.stream(prompt);
//    }
//
//}
