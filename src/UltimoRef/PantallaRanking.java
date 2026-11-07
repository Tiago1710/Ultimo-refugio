package UltimoRef;

import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Ventana con el top 10 de jugadores. Se puede abrir desde el menú o desde la pantalla final. */
public class PantallaRanking extends JDialog {

    private static final Color FONDO = new Color(12, 16, 12);
    private static final Color TEXTO = new Color(225, 235, 215);
    private static final Color VERDE = new Color(75, 145, 85);
    private static final Color ORO = new Color(255, 215, 0);
    private static final Color PLATA = new Color(200, 205, 210);
    private static final Color BRONCE = new Color(205, 140, 80);

    private final JPanel panelFilas = new JPanel();
    private final JLabel lblEstado = new JLabel("Cargando ranking...", JLabel.CENTER);

    public PantallaRanking(Window owner) {
        super(owner, "RANKING - MEJORES SUPERVIVIENTES", ModalityType.APPLICATION_MODAL);

        setSize(640, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VERDE, 2),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)));

        // ----- Título -----
        JLabel titulo = new JLabel(
                "<html><div style='text-align:center;'>"
                + "<h1 style='color:#78C282; margin:0;'>TOP 10 SUPERVIVIENTES</h1>"
                + "<p style='color:#a3c1ad; font-size:11px;'>Mejor puntuación de cada jugador</p>"
                + "</div></html>", JLabel.CENTER);
        raiz.add(titulo, BorderLayout.NORTH);

        // ----- Tabla -----
        panelFilas.setOpaque(false);
        panelFilas.setLayout(new GridLayout(11, 1, 0, 4)); // encabezado + 10 filas

        lblEstado.setForeground(TEXTO);
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(lblEstado, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        // ----- Botón cerrar -----
        JButton btnCerrar = new JButton("CERRAR");
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCerrar.setBackground(new Color(30, 40, 30));
        btnCerrar.setForeground(TEXTO);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 90, 60), 2),
                BorderFactory.createEmptyBorder(8, 30, 8, 30)));
        btnCerrar.addActionListener(e -> dispose());

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sur.setOpaque(false);
        sur.add(btnCerrar);
        raiz.add(sur, BorderLayout.SOUTH);

        add(raiz);

        // Carga en segundo plano (para no congelar la ventana)
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                return SupabaseClient.obtenerTop10();
            }

            @Override
            protected void done() {
                try {
                    mostrarFilas(get(), centro);
                } catch (Exception ex) {
                    lblEstado.setText("No se pudo cargar el ranking.");
                    lblEstado.setForeground(new Color(230, 120, 110));
                }
            }
        }.execute();

        setVisible(true); // bloquea hasta que se cierre
    }

    private void mostrarFilas(List<String[]> top, JPanel centro) {

        if (top.isEmpty()) {
            lblEstado.setText("Todavía no hay puntajes. ¡Sé el primero en el ranking!");
            return;
        }

        panelFilas.removeAll();
        panelFilas.add(crearFila("#", "JUGADOR", "ROL", "DÍAS", "PUNTOS", VERDE, true));

        for (int i = 0; i < 10; i++) {
            if (i < top.size()) {
                String[] f = top.get(i);
                boolean esYo = f[0].equals(SupabaseClient.username);

                Color color = (i == 0) ? ORO : (i == 1) ? PLATA : (i == 2) ? BRONCE : TEXTO;
                if (esYo) color = new Color(130, 220, 140);

                panelFilas.add(crearFila(String.valueOf(i + 1),
                        esYo ? f[0] + "  (vos)" : f[0], f[1], f[2], f[3], color, false));
            } else {
                panelFilas.add(crearFila(String.valueOf(i + 1), "—", "", "", "",
                        new Color(90, 100, 90), false));
            }
        }

        centro.removeAll();
        centro.add(panelFilas, BorderLayout.CENTER);
        centro.revalidate();
        centro.repaint();
    }

    private JPanel crearFila(String pos, String jugador, String rol, String dias,
                             String puntos, Color color, boolean encabezado) {

        JPanel fila = new JPanel(new GridBagLayout());
        fila.setBackground(encabezado ? new Color(20, 32, 22) : new Color(18, 24, 18));
        fila.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        double[] pesos = {0.08, 0.38, 0.22, 0.12, 0.20};
        String[] textos = {pos, jugador, rol, dias, puntos};

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < textos.length; i++) {
            JLabel l = new JLabel(textos[i]);
            l.setForeground(color);
            l.setFont(new Font("SansSerif", Font.BOLD, encabezado ? 12 : 14));
            if (i >= 3) l.setHorizontalAlignment(JLabel.RIGHT);
            gbc.gridx = i;
            gbc.weightx = pesos[i];
            fila.add(l, gbc);
        }
        return fila;
    }
}