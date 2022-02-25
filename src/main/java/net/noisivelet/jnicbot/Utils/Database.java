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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.noisivelet.jnicbot.JnicBot;
import net.noisivelet.jnicbot.Utils.Entrada.Tipo;
import net.noisivelet.jnicbot.Tasks.MessagePublishTask;

/**
 *
 * @author Francis
 */
public class Database {
    public static ArrayList<Entrada> palabras=new ArrayList<>(); //Lista de palabras añadidas al jnicpara.
    public static ArrayList<Entrada> frases=new ArrayList<>(); //Lista de frases añadidas al jnicpara
    public static HashMap<Guild, Config> config=new HashMap<>(); //Lista de servidores que tienen al JnicBot agregado con sus parámetros de configuración
    public static final MysqlDataSource DATASOURCE=getDataSource(); //Conexión a MariaDB
    public static final long ID_CREATOR=178860287292735489L; //Noisivelet en Discord
    
    private static final ArrayList<String> shardedJnicpara=new ArrayList<>(); //Mensajes de jnicpara (palabras), cada uno de un máximo de 2000 caracteres.
    private static final ArrayList<String> shardedJnicparaFrases=new ArrayList<>(); //Igual que arriba, pero para las frases.
    private static final Semaphore shardedMessagesSemaphore=new Semaphore(1); //Necesario su uso debido a que no puede accederse a los mensajes si se están editando.
    
    private static boolean dataLoadedFromDB=false;
    /**
     * Carga los datos desde la base de datos. Esta operación solo puede ejecutarse una única vez.
     * @throws SQLException Si ocurre un error con la base de datos a la hora de extraer su contenido.
     * @throws IllegalStateException Si se intenta ejecutar este método más de una única vez.
     */
    static public void loadFromDB() throws SQLException, IllegalStateException{
        if(dataLoadedFromDB)
            throw new IllegalStateException("Los datos ya se habían cargado desde la database.");
        dataLoadedFromDB=true;
        
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
            Entrada nueva=new Entrada(string, timestamp, creador);
            añadir(nueva, Tipo.palabras, false);
        });
        
        frases.forEach(hm -> {
            String string=hm.get("frase");
            long timestamp=Long.parseLong(hm.get("created_at"));
            long creador=Long.parseLong(hm.get("creator_id"));
            Entrada nueva=new Entrada(string, timestamp, creador);
            añadir(nueva, Tipo.frases, false);
        });
        
        for(HashMap<String, String> hm : configs){
            Guild guild=JnicBot.jda.getGuildById(hm.get("server_id"));
            String publish_channel_id=hm.get("publish_channel_id");
            GuildChannel publish=null;
            if(publish_channel_id != null)
                publish=guild.getGuildChannelById(ChannelType.TEXT, publish_channel_id);
            
            String message_id=hm.get("message_id");
            ArrayList<Message> frases_server=new ArrayList<>(), palabras_server=new ArrayList<>();
            if(message_id != null)
                try{
                    TextChannel jnicparaChannel=((TextChannel)guild.getGuildChannelById(hm.get("message_id_channel")));
                    if(jnicparaChannel != null){
                        ArrayList<HashMap<String, String>> servers_mensajes=querySelect("SELECT * FROM servers_mensajes WHERE server_id=? ORDER BY `orden`", guild.getId());
                    
                        servers_mensajes.forEach(hm_m -> {
                            String tipo_str=hm_m.get("tipo");
                            Tipo tipo=Tipo.valueOf(tipo_str);
                            String msg_id=hm_m.get("id_mensaje");
                            Message msg=jnicparaChannel.retrieveMessageById(msg_id).complete();
                            if(tipo==Tipo.frases)
                                frases_server.add(msg);
                            else
                                palabras_server.add(msg);
                        });
                    }
                    
                } catch(InsufficientPermissionException ex){}
                
            
            config.put(guild, new Config(guild, publish, palabras_server, frases_server));
        }
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
    
    /**
     * Ejecuta una consulta de tipo SELECT a la base de datos.
     * @param statement Consulta que se realizará. Puede contener wildcards '?'.
     * @param params Lista de parámetros que reemplazarán a los wildcards '?' presentes en la consulta.
     * @return Una lista de mapas, donde cada elemento de la lista es una línea de la base de datos. Cada elemento del HashMap será la columna del dato accedido, y su valor será el valor de dicha columna para la línea.
     * @throws SQLException Si no se puede llevar a cabo la consulta en la base de datos.
     */
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
    
    /**
     * Ejecuta una consulta en la base de datos, de tipo INSERT.
     * @param statement La consulta a realizar. Permite wildcards de tipo '?'
     * @param params Parámetros que reemplazarán los wildcards '?' en la consulta. 
     * @return La ID de la nueva fila insertada en la DB.
     * @throws SQLException Si existe un error a la hora de ejecutar la consulta en la base de datos o si no se ha afectado ninguna fila a la hora de ejecutar la consulta.
     */
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
    
    
    /**
     * Ejecuta una orden en la DB, de tipo DML.
     * @param statement Orden a realizar. Admite wildcards de tipo '?'
     * @param params Parámetros que reemplazarán cada wildcard '?' insertados en la orden SQL.
     * @return El número de filas afectadas por la orden SQL ejecutada.
     * @throws SQLException Si ocurre un error a la hora de ejecutar la orden SQL.
     */
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
    
    /**
     * Añade una nueva palabra o frase al jnicpara.
     * @param elemento Entrada a añadir al jnicpara. 
     * @param tipo Tipo de entrada: {@link Tipo#palabras} si es una palabra; {@link Tipo#frases} si es una frase.
     * @param publicarActualizacion Si se publicará la actualización en los servidores que hayan activado esta opción y si se actualizarán los mensajes en cada servidor que haya publicado el jnicpara.
     */
    public static void añadir(Entrada elemento, Entrada.Tipo tipo, boolean publicarActualizacion){
        Entrada antigua=null; //Palabra antigua, para saber si estamos en el mismo año o no
        switch(tipo){
            case palabras -> {
                if(!palabras.isEmpty())
                    antigua=palabras.get(palabras.size()-1);
                
                Database.palabras.add(elemento);
            }
            case frases -> {
                if(!frases.isEmpty())
                    antigua=frases.get(frases.size()-1);
                
                Database.frases.add(elemento);
            }
        }
        
        Calendar c=new GregorianCalendar();
        
        //El año actual sirve para saber si tenemos que añadir la palabra en un año diferente al actual en el mensaje final
        int año_actual=0;
        if(antigua != null){
            c.setTimeInMillis(antigua.timestamp);
            año_actual=c.get(Calendar.YEAR);
        }
        
        String texto_agregado="";
        c.setTimeInMillis(elemento.timestamp);
        if(c.get(Calendar.YEAR) != año_actual && tipo == Tipo.palabras){ //Si es una palabra y cambiamos el año se añade una sección nueva
            texto_agregado+="\n```yaml\n"+c.get(Calendar.YEAR)+"```\n";
        }
        texto_agregado+=elemento.string+" ";
        
        //Si es una frase, hace falta añadir el autor y la fecha al final
        if(tipo == Tipo.frases){
            texto_agregado+="- <@"+elemento.id_creador+"> "+c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+"\n";
        }
        
        int length_texto_agregado=texto_agregado.length();
        try{
            shardedMessagesSemaphore.acquire();
            String mensaje;
            switch(tipo){
                default:
                case palabras:
                    if(shardedJnicpara.isEmpty()){
                        shardedJnicpara.add("");
                    }
                    mensaje=shardedJnicpara.get(shardedJnicpara.size()-1);
                    break;
                case frases:
                    if(shardedJnicparaFrases.isEmpty()){
                        shardedJnicparaFrases.add("");
                    }
                    mensaje=shardedJnicparaFrases.get(shardedJnicparaFrases.size()-1);
            }
            ArrayList<String> shardedList=tipo==Tipo.frases? shardedJnicparaFrases : shardedJnicpara;
            if(mensaje.length() + length_texto_agregado > Message.MAX_CONTENT_LENGTH){
                shardedList.add(texto_agregado);
            } else {
                String msgAfterEdit=mensaje+texto_agregado;
                shardedList.set(shardedList.size()-1, msgAfterEdit);
            }
        } catch(InterruptedException ex){
            ex.printStackTrace();
        } finally{
            shardedMessagesSemaphore.release();
        }
        
        if(!publicarActualizacion){
            //Actualizar mensajes en cada Guild, mandar mensaje de update a las guilds que lo hayan activado.
            for(Guild guild: config.keySet()){
                Config cfg=config.get(guild);
                Message jnicpara_message=(tipo == Tipo.frases)? cfg.getLastJnicparaFrasesMessage() : cfg.getLastJnicparaMessage();
                if(jnicpara_message!= null){ //Si se ha dividido el jnicpara el mensaje ya está actualizado
                    try{
                        jnicpara_message.editMessage(tipo == Tipo.frases? getJnicparaFrases() : getJnicpara()).submit();
                    } catch(InsufficientPermissionException ex){}

                }

                GuildChannel gc=cfg.getUpdatesChannel();
                if(gc != null){
                    try{
                        ((TextChannel)gc).sendMessage(EmbededMessages.updateEmbed(elemento, tipo)).submit();
                    } catch(InsufficientPermissionException ex){}
                }
            }
        }
        
    }

    public static void start() throws SQLException{
        loadFromDB();
    }

    /**
     * Vuelve a publicar el jnicpara en todos los servidores.<br>
     * El método realizará, por orden, las siguientes acciones:<br><br>
     * 
     * - Creará n mensajes de un máximo de {@link Message#MAX_CONTENT_LENGTH} caracteres cada uno<br>
     * - Creará una tarea {@link MessagePublishTask} donde se publicarán, en el canal del jnicpara, cada uno de los nuevos mensajes creados<br>
     * - Actualizará la configuración de todos los servidores actualizando los mensajes del jnicpara de cada servidor.<br>
     * - Borrará los mensajes de jnicpara antiguos.<br>
     */
    private static void republishJnicpara() {
        for(Guild g : config.keySet()){
            Config cfg=config.get(g);
            if(cfg.getLastJnicparaMessage() != null){
                GuildMessageChannel jnicparaChannel=(GuildMessageChannel)cfg.getLastJnicparaMessage().getChannel();
                JnicBot.EXECUTOR.submit(new MessagePublishTask(jnicparaChannel, g));
            }
        }
    }
    
    
    public static String getJnicpara(){
        try {
            shardedMessagesSemaphore.acquire();
            String mensaje=shardedJnicpara.get(shardedJnicpara.size()-1);
            shardedMessagesSemaphore.release();
            return mensaje;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static String getJnicparaFrases(){
        try {
            shardedMessagesSemaphore.acquire();
            String mensaje=shardedJnicparaFrases.get(shardedJnicparaFrases.size()-1);
            shardedMessagesSemaphore.release();
            return mensaje;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static void publicarJnicpara(CompletableFuture<InteractionHook> hook, GuildMessageChannel channel, Guild g){
        try {
            shardedMessagesSemaphore.acquire();
            InteractionHook retrievedHook=null;
            if(hook!=null){
                retrievedHook=hook.get();
                g=retrievedHook.getInteraction().getGuild();
            }
            
            ArrayList<Message> msgs=new ArrayList<>(), msgs_frases=new ArrayList<>();
            
            try{
                for(String s : Database.shardedJnicpara){
                    msgs.add(channel.sendMessage(s).complete());
                }
                for(String s : Database.shardedJnicparaFrases){
                    msgs_frases.add(channel.sendMessage(s).complete());
                }
            }catch(InsufficientPermissionException ex){
                if(retrievedHook!=null)
                    retrievedHook.sendMessage(EmbededMessages.errorEmbed("Error de permisos", "La acción no se ha podido completar porque el bot no tiene permiso para publicar mensajes en el canal. Revisa los permisos (El bot necesita permiso para ver el canal y para publicar mensajes en él)")).setEphemeral(true).submit();
                return;
            }
            
            Config serverConfig=Database.config.get(g);
            ArrayList<Message> palabrasAntiguas=serverConfig.getJnicparaMessages();
            ArrayList<Message> frasesAntiguas=serverConfig.getJnicparaFrasesMessages();
            serverConfig.setJnicparaMessages(msgs, msgs_frases);
            for(Message m : palabrasAntiguas){
                m.delete().submit();
            }
            for(Message m : frasesAntiguas){
                m.delete().submit();
            }
            if(retrievedHook != null)
                retrievedHook.sendMessage(EmbededMessages.successEmbed("Mensajes actualizados", "Los mensajes anteriores (si había algunos) ya no se actualizarán automáticamente; lo harán los nuevos mensajes creados.")).submit();
        } catch (InterruptedException | SQLException | ExecutionException ex) {
            ex.printStackTrace();
        } finally{
            shardedMessagesSemaphore.release();
        }
    }
}
