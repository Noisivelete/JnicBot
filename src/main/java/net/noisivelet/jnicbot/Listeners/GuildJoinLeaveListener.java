/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Listeners;

import java.sql.SQLException;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.noisivelet.jnicbot.Utils.Config;
import net.noisivelet.jnicbot.Utils.Database;

/**
 *
 * @author Francis
 */
public class GuildJoinLeaveListener extends ListenerAdapter {
    @Override
    public void onGuildJoin(GuildJoinEvent event){
        Config cfg=new Config(event.getGuild(), null, null, null);
        try {
            cfg.addToDatabase();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        Database.config.put(event.getGuild(), cfg);
        
    }
}
