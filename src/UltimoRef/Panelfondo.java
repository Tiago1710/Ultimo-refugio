package UltimoRef;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** Panel encargado de renderizar la imagen de fondo según el lugar actual. */
public class Panelfondo extends JPanel {

    public enum Lugar { PRINCIPAL, REFUGIO, HOSPITAL, BOSQUE, CASA, MERCADO, COMISARIA, GRANJA, SOTANO }

    // Caché: las imágenes se leen del disco UNA sola vez.
    private final Map<Lugar, BufferedImage> cache = new EnumMap<>(Lugar.class);

    private Lugar lugarActual = Lugar.PRINCIPAL;
    private String rolActual = "Sobreviviente";
    private BufferedImage imagenActual;

    // Caché de la versión escalada para no reescalar en cada repintado.
    private BufferedImage escalada;
    private int escaladaAncho = -1;
    private int escaladaAlto = -1;

    public Panelfondo() {
        setOpaque(false);
        precargarImagenes();
        setLugar(Lugar.PRINCIPAL);
    }

    private void precargarImagenes() {
        for (Lugar l : Lugar.values()) {
            cache.put(l, leerImagen(nombreArchivo(l)));
        }
    }

    private String nombreArchivo(Lugar lugar) {
        switch (lugar) {
            case PRINCIPAL: return "FondoPrincipal.jpg";
            case REFUGIO:   return "refugio.jpg";
            case HOSPITAL:  return "hospital.jpg";
            case BOSQUE:    return "bosque.jpg";
            case CASA:      return "casaabandonada.jpg";
            case MERCADO:   return "mercado.jpg";
            case COMISARIA: return "Comisaria.png";
            case GRANJA:    return "Granja.png";
            case SOTANO:    return "sotano.jpg";
            default:        return "";
        }
    }

    private BufferedImage leerImagen(String nombreArchivo) {
        try {
            URL url = getClass().getResource("/UltimoRef/img/" + nombreArchivo);
            if (url != null) {
                return ImageIO.read(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setRol(String rol) {
        this.rolActual = rol;
        repaint();
    }

    public void setLugar(Lugar lugar) {
        this.lugarActual = lugar;
        this.imagenActual = cache.get(lugar);
        this.escalada = null; // fuerza reescalado solo cuando cambia el lugar
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (imagenActual != null) {
            // Solo se reescala si cambió el tamaño del panel o el lugar.
            if (escalada == null || escaladaAncho != w || escaladaAlto != h) {
                escalada = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = escalada.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(imagenActual, 0, 0, w, h, null);
                g2.dispose();
                escaladaAncho = w;
                escaladaAlto = h;
            }
            g.drawImage(escalada, 0, 0, null);
        } else {
            g.setColor(new Color(25, 30, 25));
            g.fillRect(0, 0, w, h);
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("Imagen no encontrada: " + lugarActual, 20, 30);
        }
    }
}