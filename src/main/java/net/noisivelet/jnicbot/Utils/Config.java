/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import java.sql.SQLException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;

/**
 *
 * @author Francis
 */
public class Config {
    public final Guild guild;
    private GuildChannel updates;
    private Message jnicparaMessage;
    private Message jnicparaFrasesMessage;

    public Config(Guild guild, GuildChannel updates, Message jnicparaMessage, Message jnicparaFrasesMessage) throws IllegalStateException {
        assertCorrectChannelType(updates);
        this.guild = guild;
        this.updates = updates;
        this.jnicparaMessage = jnicparaMessage;
        this.jnicparaFrasesMessage = jnicparaFrasesMessage;
    }
    
    private void assertCorrectChannelType(GuildChannel channel) throws IllegalStateException{
        if (channel == null) return;
        switch(channel.getType()){
            case CATEGORY, PRIVATE, STAGE, STORE, UNKNOWN, VOICE -> 
                throw new IllegalStateException("El tipo de canal del canal de updates del jnicpara es inválido. Solo pueden usarse canales de texto.");
        }
    }
    
    public void setUpdatesChannel(GuildChannel newChannel) throws SQLException, IllegalStateException{
        assertCorrectChannelType(newChannel);
        try{
            Database.queryDML("UPDATE servers SET publish_channel_id=? WHERE server_id=?", newChannel==null?null:newChannel.getId(), guild.getId());
        } catch(SQLException ex){
            throw ex;
        }
        updates=newChannel;
        
    }
    
    public void setJnicparaMessages(Message palabras, Message frases) throws SQLException{
        try{
            Database.queryDML("UPDATE servers SET message_id=?, message_id_channel=?, message_id_frases=? WHERE server_id=?", palabras.getId(), palabras.getChannel().getId(), frases.getId(), guild.getId());
        } catch(SQLException ex){
            throw ex;
        }
        jnicparaMessage=palabras;
        jnicparaFrasesMessage=frases;
    }
    
    public GuildChannel getUpdatesChannel(){
        return updates;
    }
    
    public Message getJnicparaMessage(){
        return jnicparaMessage;
    }
    
    public Message getJnicparaFrasesMessage(){
        return jnicparaFrasesMessage;
    }
    
    public void addToDatabase() throws SQLException{
        Database.queryInsert("INSERT INTO servers VALUES(?,?,?,?,?)", guild.getId(), updates==null?null:updates.getId(), jnicparaMessage==null?null:jnicparaMessage.getId(), jnicparaMessage==null?null:jnicparaMessage.getChannel().getId(), jnicparaFrasesMessage==null?null:jnicparaFrasesMessage.getId());
    }
    
    public void removeFromDatabase() throws SQLException{
        Database.queryDML("DELETE FROM servers WHERE server_id=?", guild.getId());
    }
}
