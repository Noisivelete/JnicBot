/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Tasks;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.noisivelet.jnicbot.Utils.Config;
import net.noisivelet.jnicbot.Utils.Database;
import net.noisivelet.jnicbot.Utils.EmbededMessages;

/**
 *
 * @author Francis
 */
public class MessagePublishTask implements Runnable{
    private final CompletableFuture<Message> sent_msg, sent_msg_frases;
    private final CompletableFuture<InteractionHook> hook;

    public MessagePublishTask(CompletableFuture<Message> sent_msg, CompletableFuture<Message> sent_msg_frases, CompletableFuture<InteractionHook> hook) {
        this.sent_msg = sent_msg;
        this.sent_msg_frases = sent_msg_frases;
        this.hook = hook;
    }

    @Override
    public void run() {
        try {
            InteractionHook hook=this.hook.get();
            Message msg, msg_frases;
            try{
                msg=sent_msg.get();
                msg_frases=sent_msg_frases.get();
            } catch(ExecutionException ex){
                ex.printStackTrace();
                return;
            }
            Config serverConfig=Database.config.get(hook.getInteraction().getGuild());
            Message jnicparaAntiguo=serverConfig.getJnicparaMessage();
            Message jnicparaFrasesAntiguo=serverConfig.getJnicparaFrasesMessage();
            serverConfig.setJnicparaMessages(msg, msg_frases);
            if(jnicparaAntiguo != null){
                jnicparaAntiguo.delete().submit();
                jnicparaFrasesAntiguo.delete().submit();
            }
            hook.sendMessage(EmbededMessages.successEmbed("Mensaje actualizado", "El mensaje anterior (si había alguno) ya no se actualizará automáticamente; lo hará el nuevo mensaje creado.")).submit();
        } catch (InterruptedException | ExecutionException | SQLException ex) {
            ex.printStackTrace();
        }
    }
}
