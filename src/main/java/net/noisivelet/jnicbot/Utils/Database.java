/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Scanner;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.noisivelet.jnicbot.JnicBot;

/**
 *
 * @author Francis
 */
public class Database {
    public static ArrayList<Entrada> palabras=new ArrayList<>();
    public static ArrayList<Entrada> frases=new ArrayList<>();
    public static HashMap<Guild, Config> config=new HashMap<>();
    public static final MysqlDataSource DATASOURCE=getDataSource();
    public static final long ID_CREATOR=178860287292735489L;
    
    static public String getJnicpara(){
        String mensaje="";
        int año_actual=0;
        int contador=0;
        Calendar c=new GregorianCalendar();
        for(Entrada palabra : palabras){ //El arraylist está ordenado por orden de llegada
            c.setTimeInMillis(palabra.timestamp);
            int año=c.get(Calendar.YEAR);
            if(año > año_actual){
                if(año_actual != 0)
                    mensaje+="("+contador+")\n";
                mensaje+="```yaml\n"+año+"```\n";
                año_actual=año;
                contador=0;
            }
            contador++;
            mensaje+=palabra.string+" ";
        }
        
        return mensaje;
    }
    
    public static String getJnicparaFrases() {
        String mensaje="```Hermosas frases célebres```\n";
        Calendar c=new GregorianCalendar();
        
        for(Entrada frase : frases){ //El arraylist está ordenado por orden de llegada
            c.setTimeInMillis(frase.timestamp);
            mensaje+=frase.string+" - <@"+frase.id_creador+"> "+c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+"\n";
        }
        
        return mensaje;
    }
    
    static public void loadFromDB() throws SQLException{
        
        ArrayList<HashMap<String, String>> palabras=querySelect("SELECT * FROM palabras ORDER BY created_at ASC");
        ArrayList<HashMap<String, String>> frases=querySelect("SELECT * FROM frases ORDER BY created_at ASC");
        ArrayList<HashMap<String, String>> configs=querySelect("SELECT * FROM servers");
        
        palabras.forEach(hm -> {
            String string=hm.get("palabra");
            long timestamp=Long.parseLong(hm.get("created_at"));
            String creator_id=hm.get("creator_id");
            Long creador=null;
            if(creator_id!=null)
                creador=Long.parseLong(hm.get("creator_id"));
            Database.palabras.add(new Entrada(string, timestamp, creador));
        });
        
        frases.forEach(hm -> {
            String string=hm.get("frase");
            long timestamp=Long.parseLong(hm.get("created_at"));
            long creador=Long.parseLong(hm.get("creator_id"));
            Database.frases.add(new Entrada(string, timestamp, creador));
        });
        
        configs.forEach(hm -> {
            Guild guild=JnicBot.jda.getGuildById(hm.get("server_id"));
            String publish_channel_id=hm.get("publish_channel_id");
            GuildChannel publish=null;
            if(publish_channel_id != null)
                publish=guild.getGuildChannelById(ChannelType.TEXT, publish_channel_id);
            
            String message_id=hm.get("message_id");
            Message jnicpara_msg=null;
            Message jnicpara_frases_msg=null;
            if(message_id != null)
                try{    
                    jnicpara_msg=((TextChannel)guild.getGuildChannelById(hm.get("message_id_channel"))).retrieveMessageById(hm.get("message_id")).complete();
                    jnicpara_frases_msg=((TextChannel)guild.getGuildChannelById(hm.get("message_id_channel"))).retrieveMessageById(hm.get("message_id_frases")).complete();
                } catch(InsufficientPermissionException ex){}
            
            config.put(guild, new Config(guild, publish, jnicpara_msg, jnicpara_frases_msg));
        });
    }
    
    private static MysqlDataSource getDataSource(){
        MysqlDataSource ds=new MysqlDataSource();
        String[] params=new String[3];
        int i=0;
        File file = new File("sql.discordbot"); 
        Scanner sc; 
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException ex) {
            System.err.println("El archivo sql.discordbot no existe.");
            try {
                file.createNewFile();
            } catch (IOException ex1) {
                System.err.println("El archivo sql.discordbot no pudo crearse: ");
                ex1.printStackTrace();
            }
            return null;
        }
  
        while (sc.hasNextLine()) {
            params[i++]=sc.nextLine();
        }
        if(i!=3){
            System.err.println("El archivo SQL no tiene una configuración válida.");
            return null;
        }
        ds.setUser(params[0]);
        ds.setPassword(params[1]);
        ds.setUrl("jdbc:"+params[2]);
        return ds;
    }
    
    public static ArrayList<HashMap<String, String>> querySelect(String statement, String... params) throws SQLException{
        try(
                Connection cn=DATASOURCE.getConnection();
                PreparedStatement stmt=cn.prepareStatement(statement);
                ){
                    for(int i=1;i<=params.length;i++){
                        stmt.setString(i, params[i-1]);
                    }
                    ResultSet rs=stmt.executeQuery();
                    ResultSetMetaData rsmd=rs.getMetaData();
                    ArrayList<HashMap<String,String>> al=new ArrayList<>();
                    HashMap<String, String> hm;
                    while(rs.next()){
                        hm=new HashMap<>();
                        for(int i=1;i<=rsmd.getColumnCount();i++){
                            hm.put(rsmd.getColumnLabel(i), rs.getString(i));
                        }
                        al.add(hm);
                    }
                    return al;
        }
    }
    
    public static long queryInsert(String statement, String... params) throws SQLException{
        try(
                Connection cn=DATASOURCE.getConnection();
                PreparedStatement stmt=cn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
                )
        {
            for(int i=1;i<=params.length;i++){
                stmt.setString(i, params[i-1]);
            }
            int numRows=stmt.executeUpdate();
            if(numRows==0) throw new SQLException("Error: Ninguna fila afectada.");
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return (generatedKeys.getLong(1));
                }
                else {
                    return -1;
                }
            }
            
        }
    }
    
    public static int queryDML(String statement, String... params) throws SQLException{
        try (
                Connection cn=DATASOURCE.getConnection();
                PreparedStatement stmt=cn.prepareStatement(statement);
                
            )
        {
            for (int i=1;i<=params.length;i++) {
                stmt.setString(i, params[i-1]);
            }
            return stmt.executeUpdate();         
        }
    }

    public static void añadirPalabra(Entrada nueva_palabra) {
        Database.palabras.add(nueva_palabra);
        for(Guild guild:config.keySet()){
            Config cfg=config.get(guild);
            Message jnicpara_message=cfg.getJnicparaMessage();
            if(jnicpara_message!= null){
                try{
                    jnicpara_message.editMessage(getJnicpara()).submit();
                } catch(InsufficientPermissionException ex){}
                
            }
            
            GuildChannel gc=cfg.getUpdatesChannel();
            if(gc != null){
                try{
                    ((TextChannel)gc).sendMessage(EmbededMessages.updateEmbed(nueva_palabra, Entrada.Tipo.palabras)).submit();
                } catch(InsufficientPermissionException ex){}
            }
        }
    }
    
    public static void añadirFrase(Entrada nueva_frase) {
        Database.frases.add(nueva_frase);
        for(Guild guild:config.keySet()){
            Config cfg=config.get(guild);
            Message jnicpara_message=cfg.getJnicparaFrasesMessage();
            if(jnicpara_message!= null){
                try{
                    jnicpara_message.editMessage(getJnicparaFrases()).submit();
                } catch(InsufficientPermissionException ex){}
                
            }
            
            GuildChannel gc=cfg.getUpdatesChannel();
            if(gc != null){
                try{
                    ((TextChannel)gc).sendMessage(EmbededMessages.updateEmbed(nueva_frase, Entrada.Tipo.frases)).submit();
                } catch(InsufficientPermissionException ex){}
            }
        }
    }

    public static void start() throws SQLException{
        loadFromDB();
    }
}
