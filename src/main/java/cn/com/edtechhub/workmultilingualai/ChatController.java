package cn.com.edtechhub.workmultilingualai;

import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
class ChatClientUtils {

    /**
     * 部署地址
     */
    static private final String ollamaBaseUrl = "http://127.0.0.1:11434";

    /**
     * 模型种类
     */
    static String models = "mistral"; // "gemma3:12b"; // "ollama3.2"; // "DeepSeek-R1";

    /**
     * 模型温度
     */
    static double temperature = 0.8;

    /**
     * 默认系统消息
     */
    static String defaultSystem = "你是一名{role}，所有的回答都需要带上“{text}”才能结束";

    /**
     * 创建客户对象
     */
    static public ChatClient getChatClient() {
        ChatModel chatModel = OllamaChatModel // 构建模型对象
                .builder()
                .ollamaApi(new OllamaApi(ollamaBaseUrl))
                .defaultOptions(
                        OllamaOptions
                                .builder()
                                .model(models)
                                .temperature(temperature)
                                .build())
                .build();

        return ChatClient
                .builder(chatModel) // 载入模型
                .defaultSystem(defaultSystem) // 设置默认系统消息(无论是否加上 system() 都会设置这个默认系统消息)
                .build(); // 构建客户端对象
    }
}

class DateTimeTools {  // 定义一个工具类
    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
    @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
    void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }
}

@RestController
public class ChatController {

    private final ChatClient chatClient = ChatClientUtils.getChatClient();

    @GetMapping("/ai/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你是谁") String message) {
        return chatClient.prompt() // 开始链式构造提示词
                .user(message) // 用户消息
                .call() // 向 AI 模型发送请求
                .content();
    }

    @GetMapping("/ai/chat_response")
    public ChatResponse chatResponse(@RequestParam(value = "message", defaultValue = "你知道米哈游么？") String message) {
        return chatClient.prompt() // 开始链式构造提示词
                .user(message) // 用户消息
                .call() // 向 AI 模型发送请求
                .chatResponse(); // 内部包含 token
    }

    public record ActorFilms(String actor, List<String> movies) {
    } // 只存数据的类, 存储作者和电影作品

    @GetMapping("/ai/entity")
    public ActorFilms entity(@RequestParam(value = "message", defaultValue = "成龙有哪些电影？") String message) {
        return chatClient.prompt() // 开始链式构造提示词
                .system("请你严格只用JSON格式回答，禁止输出任何解释、注释、额外内容，例如<think>标签、说明文字等。返回内容必须符合结构：{\"actor\": \"\", \"movies\": [\"\", \"\"]}，也不准使用 markdown") // 系统消息
                .user(message) // 用户消息
                .call() // 向 AI 模型发送请求
                .entity(ActorFilms.class); // Spring AI 结构化输出依赖模型自己是否支持严格 json, 当前的模型很难这么干...
    }

    @GetMapping("/ai/generate_flux")
    public void generateFlux(@RequestParam(value = "message", defaultValue = "你认识乔丹么？") String message) {
        Flux<String> result = chatClient.prompt() // 开始链式构造提示词
                .user(message) // 用户消息
                .stream() // 向 AI 模型发送请求
                .content();

        result.subscribe(line -> {
            System.out.println("收到一段响应：" + line); // 可以进一步考虑使用 WebSocket
        });
    }

    ChatMemory chatMemory = new InMemoryChatMemory(); // 创建一个内存聊天记录

    @GetMapping("/ai/generate_dog")
    public String generateCat(@RequestParam(value = "message", defaultValue = "你是谁") String message) {
        return chatClient.prompt() // 开始链式构造提示词
                .advisors(
                        new SimpleLoggerAdvisor(), // 添加日志记录
                        new MessageChatMemoryAdvisor(
                                chatMemory, // 聊天记忆
                                "001", // 设置会话 ID
                                10 // 上下文窗口大小(比如保留最近 10 条)
                        ) // 添加聊天记忆
                )
                .system(
                        sp -> sp
                                .param("role", "狗娘")
                                .param("text", "汪")
                ) // 填写系统消息模板
                .user(message) // 用户消息
                .call() // 向 AI 模型发送请求
                .content();
    }

    @GetMapping("/ai/show_memory")
    public List<Message> showMemory() {
        String conversationId = "001";
        return chatMemory.get(conversationId, Integer.MAX_VALUE); // 获取所有历史记录
    }

    @GetMapping("/ai/date_time")
    public String dateTime() {
        return chatClient.prompt() // 开始链式构造提示词
                .tools(new DateTimeTools()) // 添加工具类调用
                .system(
                        sp -> sp
                                .param("role", "魔法师")
                                .param("text", "呀")
                ) // 填写系统消息模板
                .user("你能把闹钟定在10分钟后吗？") // 用户消息
                .call() // 向 AI 模型发送请求
                .content()
                ;
    }

}
