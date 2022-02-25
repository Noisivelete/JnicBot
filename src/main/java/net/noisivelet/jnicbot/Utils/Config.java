/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    private ArrayList<Message> jnicparaMessages;
    private ArrayList<Message> jnicparaFrasesMessages;

    public Config(Guild guild, GuildChannel updates, ArrayList<Message> jnicparaMessages, ArrayList<Message> jnicparaFrasesMessages) throws IllegalStateException {
        assertCorrectChannelType(updates);
        this.guild = guild;
        this.updates = updates;
        this.jnicparaMessages = jnicparaMessages;
        this.jnicparaFrasesMessages = jnicparaFrasesMessages;
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
    
    public void setJnicparaMessages(ArrayList<Message> palabras, ArrayList<Message> frases) throws SQLException{
        try{
            String idChannel=null;
            if(!palabras.isEmpty()){
                idChannel=palabras.get(0).getChannel().getId();
            }
            Database.queryDML("UPDATE servers SET message_id_channel=? WHERE server_id=?", idChannel, guild.getId());
            jnicparaMessages=palabras;
            jnicparaFrasesMessages=frases;
            updateMessagesInDatabase();
        } catch(SQLException ex){
            throw ex;
        }
    }
    
    public GuildChannel getUpdatesChannel(){
        return updates;
    }
    
    public Message getJnicparaMessage(int id){
        return jnicparaMessages.get(id);
    }
    
    public Message getJnicparaFrasesMessage(int id){
        return jnicparaFrasesMessages.get(id);
    }
    
    public Message getLastJnicparaMessage(){
        return jnicparaMessages.get(jnicparaMessages.size()-1);
    }
    
    public Message getLastJnicparaFrasesMessage(){
        return jnicparaFrasesMessages.get(jnicparaFrasesMessages.size()-1);
    }
    
    public ArrayList<Message> getJnicparaMessages(){
        return jnicparaMessages;
    }
    
    public ArrayList<Message> getJnicparaFrasesMessages(){
        return jnicparaFrasesMessages;
    }
    
    public void addJnicparaMessage(Message nuevo) throws SQLException{
        jnicparaMessages.add(nuevo);
        Database.queryInsert("INSERT INTO servers_mensajes VALUES(?,?,?,?)", guild.getId(), Entrada.Tipo.palabras.name(), (jnicparaMessages.size()-1)+"", jnicparaMessages.get(0).getId());
        
    }
    
    public void addToDatabase() throws SQLException{
        Database.queryInsert("INSERT INTO servers VALUES(?,?,?)", guild.getId(), updates==null?null:updates.getId(), jnicparaMessages==null?null:jnicparaMessages.get(0).getChannel().getId());
        if(jnicparaMessages == null)
            return;
        updateMessagesInDatabase();
    }
    
    private void updateMessagesInDatabase() throws SQLException{
        Database.queryDML("DELETE FROM servers_mensajes WHERE server_id=?", guild.getId());
        for(int i=0;i<jnicparaMessages.size();i++){
            Database.queryInsert("INSERT INTO servers_mensajes VALUES(?,?,?,?)", guild.getId(), Entrada.Tipo.palabras.name(), i+"", jnicparaMessages.get(i).getId());
        }
        for(int i=0;i<jnicparaFrasesMessages.size();i++){
            Database.queryInsert("INSERT INTO servers_mensajes VALUES(?,?,?,?)", guild.getId(), Entrada.Tipo.frases.name(), i+"", jnicparaFrasesMessages.get(i).getId());
        }
    }
    
    public void removeFromDatabase() throws SQLException{
        Database.queryDML("DELETE FROM servers WHERE server_id=?", guild.getId());
    }
}
