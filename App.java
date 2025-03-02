package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class App extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    SendMessage message = new SendMessage();
    Map<Long, DevState> devState = new HashMap<>();
    Map<Long, BotState> userState = new HashMap<>();
    Map<String, String> devMessage = new HashMap<>();
    Map<String, String> firstMessage = new HashMap<>();

    AIResponse obj = new AIResponse();

    @Override
    public String getBotUsername() {
//      bot username -> from telegram bot father
        return "JobMessageCoverBot";
    }

    @Override
    public String getBotToken() {
//        bot token -> from telegram bot father
        String botToken = "your_bot_token";
        return botToken;
    }

    private enum DevState {
        AWAITING_COMPANY_NAME,
        AWAITING_JOB_DEC
    }

    private enum BotState {
        AWAITING_COMPANY_NAME,
        AWAITING_SKILLS,
        AWAITING_PROJECTS,
        AWAITING_JOB_DEC
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long userId = update.getMessage().getFrom().getId();

            String receivedMessage = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String userName = update.getMessage().getFrom().getUserName();

            // developer chat id ........
            int devChatId = 123456777;
            if (userId == devChatId) {

                if (receivedMessage.contains("/start")) {
                    String messageF = "welcome " + userName;
                    sendMessage(messageF, chatId);
                    sendMessage("I am a J_bot here to help generate cover letters", chatId);
                    sendMessage("enter company name where you want to apply", chatId);
                } else {

                    DevState currentState = devState.getOrDefault(userId, DevState.AWAITING_COMPANY_NAME);

                    if (currentState == DevState.AWAITING_COMPANY_NAME) {
                        devMessage.put("companyName", receivedMessage);
                        devState.put(userId, DevState.AWAITING_JOB_DEC);
                        sendMessage("share job description", chatId);


                    } else if (currentState == DevState.AWAITING_JOB_DEC) {
                        devMessage.put("jobD", receivedMessage);

                        devState.put(userId, DevState.AWAITING_COMPANY_NAME);

                        sendMessage("great, generating cover latter", chatId);

                        String aiRes = obj.geminiResponse(devMessage.get("companyName"), devMessage.get("jobD"));
//                        System.out.println("--------------------------" + aiRes);
                        sendMessage(aiRes, chatId);
                    }

                }
            } else {

                if (receivedMessage.contains("/start")) {
                    String messageF = "welcome " + userName;
                    sendDev(userName);
                    sendMessage(messageF, chatId);
                    sendMessage("I am a J_bot here to help generate cover letters", chatId);
                    sendMessage("enter company name where you want to apply", chatId);
                } else {

                    BotState currentState = userState.getOrDefault(userId, BotState.AWAITING_COMPANY_NAME);

                    if (currentState == BotState.AWAITING_COMPANY_NAME) {
                        firstMessage.put("companyName", receivedMessage);
                        userState.put(userId, BotState.AWAITING_SKILLS);
                        sendMessage("share your skills", chatId);
                        sendDev(receivedMessage);

                    }
                    if (currentState == BotState.AWAITING_SKILLS) {
                        firstMessage.put("skills", receivedMessage);
                        userState.put(userId, BotState.AWAITING_PROJECTS);
                        sendMessage("share your projects", chatId);


                    }
                    if (currentState == BotState.AWAITING_PROJECTS) {
                        firstMessage.put("projects", receivedMessage);
                        userState.put(userId, BotState.AWAITING_JOB_DEC);
                        sendMessage("share job description", chatId);


                    } else if (currentState == BotState.AWAITING_JOB_DEC) {
                        firstMessage.put("jobD", receivedMessage);

                        userState.put(userId, BotState.AWAITING_COMPANY_NAME);

                        sendMessage("great, generating cover latter", chatId);

                        String aiRes = obj.geminiResponseUser(firstMessage.get("companyName"), firstMessage.get("jobD"), firstMessage.get("projects"), firstMessage.get("skills"));
                        sendMessage(aiRes, chatId);
                    }
                }
            }
        }
    }

    public void sendMessage(String mess, String chatId) {
        message.setChatId(chatId);
        message.setText(mess);
        try {
            execute(message);
        } catch (Exception e) {
            log.error("e: ", e);
        }
    }

    public static void sendDev(String message_info) {
        String tt = "this is from job " + message_info;
        String devChatId = "1431210384";
        String userBot = "bot71....user bot token";
        String devURL = "https://api.telegram.org/"+userBot+"/sendMessage";
        try {
            Connection.Response response = Jsoup.connect(devURL).ignoreContentType(true).data("chat_id", devChatId).data("text", tt).method(Connection.Method.POST).execute();
        } catch (Exception a) {
            System.out.println("tare" + a);
        }
    }


    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new App());
        } catch (TelegramApiException e) {
            log.error("e: ", e);
        }
    }
}

class AIResponse{
//    private static final Logger log = LoggerFactory.getLogger(AIResponse.class);
    String api = "AIzaSyA...........gemini api key";
    String endURL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-002:generateContent?key=" + api;

//    this is for developer    //

    public String geminiResponse(String company, String jobD) {

        String prom = "\n" +
                "Write a job application message based on the job description, my skills, and relevant projects" +
                "\n" +
                "My Skills\n" +

                "Technical Skills: Java, Python, Mysql \n" +
                "Native platform development: Flutter, Dart \n" +
                "Cloud: AWS , Firebase \n" +
                "Browser Automation - Microsoft Playwright" +
                "Cyber Security: Burp suite, Linux, Web Scraping \n" +
                "Soft Skills: Leadership, Teamwork, Problem Solving\n" +
                "\n" +

                "My Projects\n" +
                "\n" +
                "Image Compress App\n" +
                "• Built an application using Flutter and Dart to ensure cross-platform compatibility and performance.\n" +
                "• Compresses images to a specific size without altering the original file.\n" +
                "• Enables users to save compressed images directly to their device.\n" +
                "• Designed to run seamlessly across Android, iOS, desktop, and web platforms.\n" +
                "• Simple and user-friendly, users only need to select an image and enter the desired size for compression.\n" +
                "\n" +
                "\n" +
                "InstagramReelBot\n" +
                "• Developed a Telegram bot using Java and the Telegram API to download Instagram reels and posts without watermarks.\n" +
                "• Users can easily download content by sharing the URL of the post via the Telegram app.\n" +
                "• Downloads are saved directly into the Telegram chat, offering a quick and convenient solution.\n" +
                "• Focused on providing a fast and user-friendly experience for content retrieval.\n" +
                "\n" +
                "\n" +
                "Face Recognition Bot\n" +
                "• Designed and built an AI-powered Face Recognition Bot using Python, integrated with AWS Rekognition services.\n" +
                "• Achieved accurate face identification and estimated age groups of people in photos.\n" +
                "• Analyzed facial expressions to understand emotional states.\n" +
                "• Training process requiring only one photo per person.\n" +
                "• Used technologies: Python3, AWS S3 Buckets, AWS Lambda Functions, AWS Rekognition API, and Telegram Bot API.\n" +
                "\n" +

                "Company name - " + company +
                "\n" +
                "Job Description " + jobD +
                "\n" +
                "give answer in 800 characters and 3 paragraphs and use simple Indian english words";

        String aiOut = "";

        String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prom + "\"}]}]}";

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body());
            JsonNode node1 = node.at("/candidates/0");
            JsonNode node2 = node1.at("/content/parts/0");
            aiOut = node2.at("/text").asText();

        } catch (Exception e) {
            return "something went wrong "+ e;
        }

        return aiOut;
    }

//    for users     //

    public String geminiResponseUser(String company, String jobD, String project, String skills) {
        String aiOut = " ";
        String prom = "\n" +
                "Generate a job application message based on the job description, my skills, and relevant projects. Exclude any email formatting, such as greetings or closings" +
                "\n" +
                "My Skills\n" +
                skills
                +
                "\n" +
                "My Projects\n" +
                "\n" +
                project
                +
                "Company name - " + company +
                "\n" +
                "Job Description " + jobD +
                "\n" +
                "write cover latter in 1000 characters and 3 paragraphs and use simple Indian english words";

        String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prom + "\"}]}]}";

        try {

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response.body());
            JsonNode node1 = node.at("/candidates/0");
            JsonNode node2 = node1.at("/content/parts/0");
            aiOut = node2.at("/text").asText();
//            System.out.println("output........" + aiOut);

        } catch (Exception e) {
            return "something went wrong "+e;
        }
        return aiOut;
    }
}