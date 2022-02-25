/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Tasks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.noisivelet.jnicbot.Utils.Config;
import net.noisivelet.jnicbot.Utils.Database;
import net.noisivelet.jnicbot.Utils.EmbededMessages;

/**
 *
 * @author Francis
 */
public class MessagePublishTask implements Runnable{
    private final GuildMessageChannel channel;
    private final CompletableFuture<InteractionHook> hook;
    private Guild g;

    /**
     * Configura una nueva tarea de publicación del diccionario. Esta tarea depende de un InteractionHook, y mandará feedback sobre las acciones que se realicen.
     * @param hook Un CompletableFuture del InteractionHook que se usará en esta tarea.
     * @param channel Canal donde se publicará el diccionario.
     */
    public MessagePublishTask(CompletableFuture<InteractionHook> hook, GuildMessageChannel channel) {
        this.hook = hook;
        this.channel = channel;
    }
    
    /**
     * Configura una tarea de publicación del diccionario que no depende de ningún InteractionHook. Debido a esto, no se enviará ningún mensaje de feedback.
     * @param channel Canal donde publicar los mensajes del diccionario.
     * @param g Guild donde se producirá esta publicación.
     */
    public MessagePublishTask(GuildMessageChannel channel, Guild g){
        hook=null;
        this.channel=channel;
        this.g=g;
    }

    @Override
    public void run() {
        Database.publicarJnicpara(hook, channel, g);
    }
}
