package com.bibliotec.controller;

import java.time.LocalDateTime;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import com.bibliotec.dto.MensajeChat;

@Controller
public class ChatController {

    @MessageMapping("/chat.enviar")
    @SendTo("/topic/chat")
    public MensajeChat enviar(MensajeChat mensaje) {
        return new MensajeChat(
            mensaje.usuario(),
            mensaje.mensaje(),
            LocalDateTime.now().toString()
        );
    }
}
