package UltimoRef;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** Panel encargado de renderizar la imagen de fondo según el lugar actual. */
public class Panelfondo extends JPanel {

    // Añadimos SOTANO al enum de lugares disponibles
    public enum Lugar { PRINCIPAL, REFUGIO, HOSPITAL, BOSQUE, CASA, MERCADO, COMISARIA, GRANJA, SOTANO }

    private Lugar lugarActual = Lugar.PRINCIPAL; 
    private String rolActual = "Sobreviviente";
    private Image imagenActual;

    public Panelfondo() {
        setOpaque(false);
        setLugar(Lugar.PRINCIPAL);
    }

    public void setRol(String rol) {
        this.rolActual = rol;
        repaint();
    }

    public void setLugar(Lugar lugar) {
        this.lugarActual = lugar;
        actualizarImagen();
        repaint();
    }

    private void actualizarImagen() {
        String nombreArchivo = "";
        switch (lugarActual) {
            case PRINCIPAL: 
                nombreArchivo = "FondoPrincipal.jpg"; 
                break;
            case REFUGIO:   
                nombreArchivo = "refugio.jpg"; 
                break;
            case HOSPITAL:  
                nombreArchivo = "hospital.jpg"; 
                break;
            case BOSQUE:    
                nombreArchivo = "bosque.jpg"; 
                break;
            case CASA:      
                nombreArchivo = "casaabandonada.jpg"; 
                break;
            case MERCADO:   
                nombreArchivo = "mercado.jpg"; 
                break;
            case COMISARIA: 
                nombreArchivo = "Comisaria.png"; 
                break;
            case GRANJA:    
                nombreArchivo = "Granja.png"; 
                break;
            case SOTANO:    
                nombreArchivo = "sotano.jpg"; // Imagen del sótano que ya tienes
                break;
        }

        try {
            URL url = getClass().getResource("/UltimoRef/img/" + nombreArchivo);
            if (url != null) {
                imagenActual = ImageIO.read(url);
            } else {
                imagenActual = null; 
            }
        } catch (IOException e) {
            imagenActual = null;
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imagenActual != null) {
            Image imagenEscalada = imagenActual.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            g.drawImage(imagenEscalada, 0, 0, this);
        } else {
            g.setColor(new Color(25, 30, 25));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("Imagen no encontrada: " + lugarActual, 20, 30);
        }
    }
}