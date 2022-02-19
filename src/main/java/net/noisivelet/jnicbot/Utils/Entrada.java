/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import java.sql.SQLException;

/**
 *
 * @author Francis
 */
public class Entrada {
    public enum Tipo{
        palabras, frases;
    }
    final public String string;
    final public Long id_creador;
    final public long timestamp;
    
    
    public Entrada(String string, long timestamp, Long id_creador){
        this.string=string;
        this.id_creador=id_creador;
        this.timestamp=timestamp;
    }
    
    public Entrada(String string, long timestamp){
        this(string, timestamp, null);
    }
    
    public Entrada(String string){
        this(string, System.currentTimeMillis(), null);
    }
    
    public void addToDatabase(Tipo tabla) throws SQLException{
        Database.queryInsert("INSERT INTO "+tabla.name()+" VALUES(?,?,?)", string, id_creador==null?null:id_creador.toString(), timestamp+"");
    }
}
