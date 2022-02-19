/*
 * Copyright (c) 2021-2022 Mineval.net Servers
 * Unless stated otherwise, modification, distribution or comertialitation of this software is prohibited by law.
 */
package net.noisivelet.jnicbot.Utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 *
 * @author Francis
 */
public class Utils {
    public static byte[] jnicparaFraseGenRandom() throws IOException{
        File source=new File("./elabuelo.png");
        if(!source.exists())
            throw new IOException("El archivo elabuelo.png no existe.");
        BufferedImage image= ImageIO.read(new File("elabuelo.png"));
        Font font = new Font("Impact", Font.BOLD, 18);
        
        Graphics g = image.getGraphics();
        g.setFont(font);
        g.setColor(Color.BLACK);
        Random r=new Random();
        int size_jnic=Database.frases.size();
        int random=r.nextInt(0, size_jnic);
        String frase=Database.frases.get(random).string;
        g.drawString(frase, 20, 100);
        
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
