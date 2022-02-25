/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Listeners;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.noisivelet.jnicbot.JnicBot;
import net.noisivelet.jnicbot.Tasks.MessagePublishTask;
import net.noisivelet.jnicbot.Utils.Config;
import net.noisivelet.jnicbot.Utils.Database;
import net.noisivelet.jnicbot.Utils.EmbededMessages;
import net.noisivelet.jnicbot.Utils.Entrada;
import net.noisivelet.jnicbot.Utils.Entrada.Tipo;
import net.noisivelet.jnicbot.Utils.Utils;

/**
 *
 * @author Francis
 */
public class SlashCommandEventListener extends ListenerAdapter{
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getUser().getIdLong()==451748776789409813L){
            event.reply("<:blobvomiting:624289306625966101>").submit();
            return;
        }
        switch(event.getName()){
            case "jnicpara" -> {
                if(event.getUser().getIdLong() != Database.ID_CREATOR)
                    return;
                OptionMapping palabra=event.getOption("palabra");
                if(palabra == null){
                    event.reply(EmbededMessages.errorEmbed("Parámetros incorrectos", "Es necesaria una palabra para añadir.")).setEphemeral(true).queue();
                    return;
                }
                
                Long userid=null;
                OptionMapping om_uid=event.getOption("id-usuario");
                if(om_uid != null){
                    try{
                        userid=om_uid.getAsLong();
                    }catch(IllegalStateException | NumberFormatException ex){
                        event.reply(EmbededMessages.errorEmbed("Parámetros incorrectos", "La ID de usuario no es correcta.")).setEphemeral(true).queue();
                        return;
                    }
                }
                Entrada nueva_palabra=new Entrada(palabra.getAsString(), System.currentTimeMillis(), userid);
                try {
                    nueva_palabra.addToDatabase(Entrada.Tipo.palabras);
                } catch (SQLException ex) {
                    System.err.println("No se puede añadir la palabra a la base de datos. Error: ");
                    ex.printStackTrace();
                    event.reply(EmbededMessages.errorEmbed("Error de conexión a la base de datos", "Ha ocurrido un error conectando a la base de datos. No se ha podido añadir la palabra.")).setEphemeral(true).queue();
                }
                Database.añadir(nueva_palabra, Tipo.palabras, true);
                
                event.reply(EmbededMessages.successEmbed("Jnicpara actualizado", "Palabra añadida al jnicpara.")).setEphemeral(true).queue();
            }
            
            case "jnicpara-frase" -> {
                if(event.getUser().getIdLong() != Database.ID_CREATOR)
                    return;
                OptionMapping palabra=event.getOption("frase");
                if(palabra == null){
                    event.reply(EmbededMessages.errorEmbed("Parámetros incorrectos", "Es necesaria una frase para añadir.")).setEphemeral(true).queue();
                    return;
                }
                
                Long userid;
                OptionMapping om_uid=event.getOption("id-usuario");
                if(om_uid == null){
                    event.reply(EmbededMessages.errorEmbed("Parámetros incorrectos", "En las frases, es necesario especificar una id de usuario.")).setEphemeral(true).queue();
                    return;
                }
                try{
                    userid=om_uid.getAsLong();
                }catch(IllegalStateException | NumberFormatException ex){
                    event.reply(EmbededMessages.errorEmbed("Parámetros incorrectos", "La ID de usuario no es correcta.")).setEphemeral(true).queue();
                    return;
                }
                Entrada nueva_frase=new Entrada(palabra.getAsString(), System.currentTimeMillis(), userid);
                try {
                    nueva_frase.addToDatabase(Entrada.Tipo.frases);
                } catch (SQLException ex) {
                    System.err.println("No se puede añadir la frase a la base de datos. Error: ");
                    ex.printStackTrace();
                    event.reply(EmbededMessages.errorEmbed("Error de conexión a la base de datos", "Ha ocurrido un error conectando a la base de datos. No se ha podido añadir la frase.")).setEphemeral(true).queue();
                }
                Database.añadir(nueva_frase, Tipo.frases, true);
                
                event.reply(EmbededMessages.successEmbed("Jnicpara actualizado", "Frase añadida al jnicpara.")).setEphemeral(true).queue();
            }
            
            case "publicar_jnicpara" -> {
                if(event.getChannelType().equals(ChannelType.PRIVATE)){
                    event.reply(EmbededMessages.errorEmbed("Acción imposible", "No puedo publicar el jnicpara en un canal privado. Necesito que pongas el comando en un servidor de Discord.")).submit();
                    return;
                }
                if(!event.getMember().hasPermission(Permission.MANAGE_SERVER)){
                    event.reply(EmbededMessages.errorEmbed("Permisos insuficientes", "Necesitas el permiso `Gestionar Servidor` para poder cambiar dónde se publica el jnicpara en este servidor.")).setEphemeral(true).queue();
                    return;
                }
                GuildMessageChannel channel=(GuildMessageChannel)event.getChannel();
                CompletableFuture<InteractionHook> hook=event.deferReply(true).submit();
                JnicBot.EXECUTOR.submit(new MessagePublishTask(hook, channel));
            }
            
            case "frase-random" -> {
                event.deferReply().submit();
                try {
                    event.getHook().editOriginal(Utils.jnicparaFraseGenRandom(), "jnicpara.png").submit();
                } catch (IOException ex) {
                    event.getHook().editOriginal(EmbededMessages.errorEmbed("Error del sistema de ficheros", "No se ha podido generar la imagen. Si el problema persiste, contacta con Noisivelet.")).submit();
                    ex.printStackTrace();
                }
            }
            
            case "canal-updates" -> {
                event.deferReply(true).submit();
                OptionMapping canal=event.getOption("canal");
                GuildChannel channel;
                if(canal == null){
                    channel=event.getGuildChannel();
                } else {
                    channel=canal.getAsGuildChannel();
                }
                
                Config cfg=Database.config.get(event.getGuild());
                try {
                    cfg.setUpdatesChannel(channel);
                    event.getHook().sendMessage(EmbededMessages.successEmbed("Completado", "Configuración del bot cambiada con éxito.\n\n**Nuevo canal de updates del bot:** "+channel.getAsMention())).submit();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    event.getHook().sendMessage(EmbededMessages.errorEmbed("Fallo de conexión a la base de datos", "No se ha podido obtener acceso a la base de datos. No se ha podido actualizar la configuración del bot.")).submit();
                } catch (IllegalStateException ex){
                    event.getHook().sendMessage(EmbededMessages.errorEmbed("Parámetros incorrectos", ex.getMessage())).submit();
                }
            
            }
        }
    }
}
