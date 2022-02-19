/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import java.awt.Color;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

/**
 *
 * @author Francis
 */
public final class EmbededMessages {
    public static Message errorEmbed(String title, String description){
        return new 
        MessageBuilder().setEmbeds(new EmbedBuilder()
                .setTitle("Error: "+title)
                .setDescription(description)
                .setColor(Color.RED)
                .build()
        ).build();
    }
    
    public static Message successEmbed(String title, String description){
        return new 
        MessageBuilder().setEmbeds(new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Color.GREEN)
                .build()
        ).build();
    }

    static Message updateEmbed(Entrada entrada, Entrada.Tipo tabla) {
        String tipo = tabla == Entrada.Tipo.frases? "Frase añadida:" : "Palabra añadida:";
        String usuario = entrada.id_creador == null? "<???>" : "<@"+entrada.id_creador+">";
        
        return new 
        MessageBuilder().setEmbeds(new EmbedBuilder()
                .setTitle("Jnicpara actualizado")
                .setDescription("Se ha actualizado el jnicpara.")
                .addField(tipo, entrada.string, true)
                .addField("Autor:", usuario, true)
                .setTimestamp(Instant.now())
                .setColor(Color.CYAN)
                .build()
        ).build();
    }
}

