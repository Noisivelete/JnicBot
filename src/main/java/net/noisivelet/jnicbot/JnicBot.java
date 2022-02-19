/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.noisivelet.jnicbot.Listeners.GuildJoinLeaveListener;
import net.noisivelet.jnicbot.Listeners.SlashCommandEventListener;
import net.noisivelet.jnicbot.Utils.Database;

/**
 *
 * @author Francis
 */
public class JnicBot {
    public static JDA jda;
    public static final ExecutorService EXECUTOR=Executors.newCachedThreadPool();
    
    public static void main(String... args){
        try{
            System.out.println("Starting...");
            jda=buildJDA();
            System.out.println("BuildJDA finished.");
            if(jda == null){
                System.out.println("Error inicializando el bot. No se puede continuar.");
                return;
            }
            try {
                System.out.println("Inicializando la base de datos...");
                Database.start();
                System.out.println("Base de datos inicializada.");
            } catch (SQLException ex) {
                System.err.println("No se puede obtener acceso a la base de datos. Error: ");
                ex.printStackTrace();
                jda.shutdownNow();
                return;
            }

            System.out.println("Añadiendo listeners...");
            addListeners();
            System.out.println("Añadiendo comandos...");
            registerCommands();
            System.out.println("Todo listo.");
            
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    static private JDA buildJDA(){
        JDA _jda;
        System.out.println("Leyendo archivo token.discordbot");
        File tokenFile=new File("token.discordbot");
        if (!tokenFile.exists() || !tokenFile.isFile()){
            System.out.println("El archivo de token de Discord \"token.discordbot\" no existe. Generando uno.");
            try {
                tokenFile.createNewFile();
            } catch (IOException ex) {
                System.out.println("Error creando el archivo: "+ex.getMessage());
                ex.printStackTrace();
            }
            return null;
        }
        
        if(!tokenFile.canRead()){
            System.out.println("No se puede leer el archivo \"token.discordbot\".");
            return null;
        }
        
        if(tokenFile.length() == 0){
            System.err.println("El archivo token \"token.discordbot\" no es válido.");
            return null;
        }
        
        String token=loadTokenFromFile(tokenFile);
        System.out.println("Token cargado.");
        try {
            System.out.println("Conectando a Discord...");
            _jda=JDABuilder.createDefault(token).build();
            System.out.println("Orden de conexión enviada.");
            System.out.println(_jda.getStatus().name());
            _jda.awaitReady();
            System.out.println("Conexión establecida.");
        } catch (LoginException | InterruptedException ex) {
            ex.printStackTrace();
            System.out.println("Error al conectar a los servidores de Discord.");
            return null;
        }
        
        return _jda;
    }
    
    static private String loadTokenFromFile(File file){
        try (BufferedReader is = new BufferedReader(new FileReader(file))) {    
            return is.readLine();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    static private void registerCommands() {
        CommandListUpdateAction commands=jda.updateCommands();
        List<Command> globalCommands=commands.addCommands(
                Commands.slash("jnicpara", "Añade una palabra al jnicpara.")
                        .addOption(OptionType.STRING, "palabra", "Palabra a añadir al diccionario.", true)
                        .addOption(OptionType.STRING, "id-usuario", "La ID del usuario que ha jnicparado dicha palabra.", false)
                        .setDefaultEnabled(false),
                Commands.slash("jnicpara-frase", "Añade una frase al jnicpara")
                        .addOption(OptionType.STRING, "frase", "Frase a añadir al diccionario", true)
                        .addOption(OptionType.STRING, "id-usuario", "La ID del usuario que ha jnicparado dicha frase.", true)
                        .setDefaultEnabled(false),
                Commands.slash("frase-random", "Genera una frase aleatoria del jnicpara, en formato de imagen.")
        ).complete();
        
        for(Guild g : jda.getGuilds()){
            CommandListUpdateAction cgua=g.updateCommands();
            cgua.addCommands(
                    Commands.slash("publicar_jnicpara", "Publica el jnicpara en el canal actual. Se actualizará automáticamente."),
                    Commands.slash("canal-updates", "Cambia el canal en el que se publicarán actualizaciones del jnicpara.")
                        .addOption(OptionType.CHANNEL, "canal", "Dónde publicar actualizaciones del jnicpara. De no rellenar esta opción, se usará el canal actual.")
            ).submit();
            
            for(Command c : globalCommands){
                c.updatePrivileges(g, CommandPrivilege.enableUser(Database.ID_CREATOR)).submit();
            }
        }
    }

    static private void addListeners() {
        jda.addEventListener(new SlashCommandEventListener());
        jda.addEventListener(new GuildJoinLeaveListener());
    }
}
