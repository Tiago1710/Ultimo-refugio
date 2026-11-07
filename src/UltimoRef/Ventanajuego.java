package UltimoRef;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

// ===== [CAMBIO SUPABASE 1] Imports nuevos para el guardado en segundo plano =====
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
// ===== FIN CAMBIO 1 =====

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;

import UltimoRef.Juego.Accion;

/** Ventana principal con HUD táctico mejorado usando iconos personalizados y panel lateral. */
public class Ventanajuego extends JFrame {

    // PALETA DE COLORES Y ESTÉTICA APOCALÍPTICA

    private static final Color COLOR_FONDO_GENERAL = new Color(12, 16, 12);
    private static final Color COLOR_HUD_FONDO = new Color(16, 24, 16);
    private static final Color TEXTO_CLARO = new Color(225, 235, 215);
    private static final Color ACENTO_VERDE = new Color(80, 160, 90);
    private static final Color BOTON_FONDO = new Color(30, 40, 30);
    private static final Color BOTON_BORDE = new Color(60, 90, 60);

    private Juego juego;
    private int mejorPuntuacion = 0;

    // ===== [CAMBIO SUPABASE 2] =====
    // Cola de guardado en un solo hilo: los guardados se hacen en orden
    // y nunca congelan la interfaz.
    private final ExecutorService guardador = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "guardado-supabase");
        t.setDaemon(true);
        return t;
    });
    // ===== FIN CAMBIO 2 =====

    // SONIDO AMBIENTAL DEL JUEGO

    private Clip sonidoFondo;

    // IMÁGENES PRECARGADAS (se escalan una sola vez al crear la ventana)

    private ImageIcon iconoMedicoTarjeta;
    private ImageIcon iconoSobrevivienteTarjeta;
    private ImageIcon iconoMedicoLateral;
    private ImageIcon iconoSobrevivienteLateral;

    // Componentes visuales principales del HUD
    private final Panelfondo fondo = new Panelfondo();
    private final JLabel lblRol = crearEtiquetaHud(new Color(255, 215, 0), Font.BOLD);
    private final JLabel lblDia = crearEtiquetaHud(new Color(130, 190, 255), Font.BOLD);

    // Etiquetas con iconos para el inventario superior
    private final JLabel lblProv = crearEtiquetaConIcono("/UltimoRef/img/cuchilleria.png", new Color(255, 165, 80));
    private final JLabel lblMed = crearEtiquetaConIcono("/UltimoRef/img/botiquin.png", new Color(80, 220, 220));
    private final JLabel lblMun = crearEtiquetaConIcono("/UltimoRef/img/Municionn.png", new Color(220, 220, 80));
    private final JLabel lblDef = crearEtiquetaConIcono("/UltimoRef/img/escudo.png", new Color(190, 130, 255));
    private final JLabel lblPunt = crearEtiquetaConIcono("/UltimoRef/img/Puntos.png", new Color(255, 215, 0));

    // Componentes del panel lateral permanente
    private final JPanel panelLateral = new JPanel(new BorderLayout(5, 10));
    private final JLabel lblRolLateralTitulo = new JLabel("", JLabel.CENTER);
    private final JLabel lblImagenPerfilLateral = new JLabel("", JLabel.CENTER);

    private final JProgressBar barraSalud = new JProgressBar(0, 100);
    private final JTextArea txtLog = new JTextArea(7, 45);

    // 10 botones de acción del jugador
    private final JButton[] botones = new JButton[10];

    private final String[] textos = {
        "Descansar (+10 Salud)", "Usar Medicina", "Fortificar Refugio",
        "Ir al Hospital", "Explorar Bosque", "Investigar Casa",
        "Saquear Mercado", "Ir al Comisaría", "Ir a la Granja", "Explorar Sótano"
    };

    private final Accion[] acciones = {
        Accion.DESCANSAR, Accion.USAR_MEDICINA, Accion.FORTIFICAR,
        Accion.HOSPITAL, Accion.BOSQUE, Accion.CASA,
        Accion.MERCADO, Accion.COMISARIA, Accion.GRANJA, Accion.SOTANO
    };

    // CONSTRUCTOR DE LA VENTANA PRINCIPAL
    // Construye toda la interfaz SIN mostrarla. La ventana se muestra
    // desde iniciarDesde(...), cuando ya está todo listo.

    public Ventanajuego() {
        setTitle("ÚLTIMO REFUGIO - SUPERVIVENCIA");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO_GENERAL);

        // Se cargan y escalan las imágenes pesadas una sola vez.
        precargarImagenes();

        // PANEL SUPERIOR (HUD TÁCTICO CON ICONOS)

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 6));
        top.setBackground(COLOR_HUD_FONDO);
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 90, 50), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Fila 1 del HUD
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
        fila1.setOpaque(false);

        JLabel lblSaludIcono = crearLabelIconoTexto("/UltimoRef/img/corazon.png", " SALUD:");
        lblSaludIcono.setForeground(new Color(255, 100, 100));
        lblSaludIcono.setFont(new Font("SansSerif", Font.BOLD, 12));

        barraSalud.setStringPainted(true);
        barraSalud.setPreferredSize(new Dimension(160, 22));
        barraSalud.setFont(new Font("SansSerif", Font.BOLD, 11));
        barraSalud.setBackground(new Color(25, 15, 15));
        barraSalud.setBorder(BorderFactory.createLineBorder(new Color(80, 40, 40), 1));

        fila1.add(lblRol);
        fila1.add(crearSeparadorVertical());
        fila1.add(lblDia);
        fila1.add(crearSeparadorVertical());
        fila1.add(lblSaludIcono);
        fila1.add(barraSalud);
        fila1.add(crearSeparadorVertical());
        fila1.add(lblPunt);

        // Fila 2 del HUD: inventario y botón para volver al menú
        JPanel fila2 = new JPanel(new BorderLayout(15, 0));
        fila2.setOpaque(false);

        JPanel panelInventario = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        panelInventario.setOpaque(false);
        panelInventario.add(lblProv);
        panelInventario.add(crearSeparadorVertical());
        panelInventario.add(lblMed);
        panelInventario.add(crearSeparadorVertical());
        panelInventario.add(lblMun);
        panelInventario.add(crearSeparadorVertical());
        panelInventario.add(lblDef);

        JPanel panelBotonInicioSuperior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBotonInicioSuperior.setOpaque(false);
        panelBotonInicioSuperior.add(crearBotonInicio());

        fila2.add(panelInventario, BorderLayout.CENTER);
        fila2.add(panelBotonInicioSuperior, BorderLayout.EAST);

        top.add(fila1);
        top.add(fila2);
        add(top, BorderLayout.NORTH);

        // PANEL CENTRAL COMPUESTO

        JPanel panelCentroCompleto = new JPanel(new BorderLayout(5, 0));
        panelCentroCompleto.setBackground(COLOR_FONDO_GENERAL);
        panelCentroCompleto.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Configuración del Panel Lateral
        panelLateral.setBackground(new Color(14, 20, 14));
        panelLateral.setPreferredSize(new Dimension(180, 0));
        panelLateral.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 80, 50), 2),
            BorderFactory.createEmptyBorder(10, 8, 10, 8)
        ));

        lblRolLateralTitulo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRolLateralTitulo.setForeground(new Color(240, 220, 130));
        panelLateral.add(lblRolLateralTitulo, BorderLayout.NORTH);

        lblImagenPerfilLateral.setVerticalAlignment(JLabel.CENTER);
        panelLateral.add(lblImagenPerfilLateral, BorderLayout.CENTER);

        panelCentroCompleto.add(panelLateral, BorderLayout.WEST);
        panelCentroCompleto.add(fondo, BorderLayout.CENTER);

        add(panelCentroCompleto, BorderLayout.CENTER);

        // PANEL INFERIOR

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setBackground(new Color(12, 15, 12, 240));
        txtLog.setForeground(TEXTO_CLARO);
        txtLog.setFont(new Font("Monospaced", Font.BOLD, 13));
        txtLog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACENTO_VERDE, 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setOpaque(false);
        scrollLog.getViewport().setOpaque(false);
        scrollLog.setBorder(null);

        JPanel panelRegistro = new JPanel(new BorderLayout(10, 0));
        panelRegistro.setOpaque(false);

        JPanel panelBotonDiario = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelBotonDiario.setOpaque(false);
        panelBotonDiario.add(crearBotonDiario());

        panelRegistro.add(panelBotonDiario, BorderLayout.WEST);
        panelRegistro.add(scrollLog, BorderLayout.CENTER);

        bottom.add(panelRegistro, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new GridLayout(4, 3, 8, 8));
        panelBotones.setOpaque(false);

        for (int i = 0; i < botones.length; i++) {
            final Accion accion = acciones[i];
            botones[i] = new JButton(textos[i]);
            final JButton btn = botones[i];

            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setBackground(BOTON_FONDO);
            btn.setForeground(TEXTO_CLARO);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BOTON_BORDE, 2),
                BorderFactory.createEmptyBorder(8, 5, 8, 5)
            ));

            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (btn.isEnabled()) {
                        btn.setBackground(new Color(45, 65, 45));
                        btn.setForeground(new Color(255, 255, 255));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(BOTON_FONDO);
                    btn.setForeground(TEXTO_CLARO);
                }
            });

            btn.addActionListener(e -> ejecutar(accion));
            panelBotones.add(btn);
        }

        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setOpaque(false);
        panelInferior.add(panelBotones, BorderLayout.CENTER);

        bottom.add(panelInferior, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // PANTALLA COMPLETA (todavía sin mostrar)

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    // =========================================================================
    // ARRANQUE SIN HUECOS ENTRE PANTALLAS
    // =========================================================================

    /**
     * Muestra la selección de rol encima del menú (que sigue visible debajo),
     * y cuando el jugador elige, abre la ventana del juego.
     * Devuelve true si se inició la partida, false si el jugador volvió al menú.
     * El menú debe cerrarlo quien lo llama cuando esto devuelve true.
     */
    public boolean iniciarDesde(JFrame menu) {

        int r = elegirRol(menu);

        if (r < 0) {
            dispose();
            return false;
        }

        iniciarPartida(r);

        // El juego se muestra mientras el menú sigue debajo: no hay pantalla vacía.
        setVisible(true);

        return true;
    }

    // ===== [CAMBIO SUPABASE 3] =====
    // MÉTODOS NUEVOS: CONTINUAR UNA PARTIDA GUARDADA Y GUARDAR EN LA NUBE

    /**
     * Abre el juego directamente en una partida guardada (botón CONTINUAR).
     * Se saltea la selección de rol: el rol viene dentro de la partida.
     */
    public boolean continuarDesde(JFrame menu, Juego guardado) {

        restaurarPartida(guardado);

        // El juego se muestra mientras el menú sigue debajo: no hay pantalla vacía.
        setVisible(true);

        return true;
    }

    /** Deja la interfaz lista con los datos de una partida guardada. */
    private void restaurarPartida(Juego guardado) {

        juego = guardado;

        String rol = guardado.getRol();

        fondo.setRol(rol);
        fondo.setLugar(Panelfondo.Lugar.PRINCIPAL);

        detenerSonidoFondo();
        reproducirSonidoFondo();

        actualizarPanelLateral(rol);

        txtLog.setText(
            "=== PARTIDA RESTAURADA ===\n"
            + "Bienvenido de vuelta, " + SupabaseClient.username + ".\n"
            + "Retomás tu travesía como " + rol.toUpperCase()
            + " en el día " + guardado.getDias() + ".\n"
            + "El refugio te esperaba."
        );

        for (JButton b : botones) {
            b.setEnabled(true);
        }

        actualizar();
    }

    /** Guarda la partida en Supabase sin congelar la interfaz. */
    private void guardarEnNube() {

        if (!SupabaseClient.haySesion() || juego == null) {
            return;
        }

        final Juego copia = juego;

        guardador.submit(() -> {
            try {
                SupabaseClient.guardarPartida(copia);
            } catch (Exception ex) {
                System.out.println("Error al guardar: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                    txtLog.append("\n\n⚠ No se pudo guardar el progreso (sin conexión)."));
            }
        });
    }
    // ===== FIN CAMBIO 3 =====

    // IMÁGENES: se leen y escalan UNA vez

    private void precargarImagenes() {
        iconoMedicoTarjeta = cargarIcono("/UltimoRef/img/Medicoo.png", 800, 530);
        iconoSobrevivienteTarjeta = cargarIcono("/UltimoRef/img/superviviente.png", 750, 530);
        iconoMedicoLateral = cargarIcono("/UltimoRef/img/Medicoo.png", 550, 400);
        iconoSobrevivienteLateral = cargarIcono("/UltimoRef/img/superviviente.png", 550, 400);
    }

    /** Lee y escala una imagen de forma inmediata (sin carga diferida). */
    private ImageIcon cargarIcono(String ruta, int ancho, int alto) {
        try {
            URL url = getClass().getResource(ruta);
            if (url == null) {
                return null;
            }
            BufferedImage original = ImageIO.read(url);
            if (original == null) {
                return null;
            }
            BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = salida.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(original, 0, 0, ancho, alto, null);
            g2.dispose();
            return new ImageIcon(salida);
        } catch (Exception e) {
            return null;
        }
    }

    // REPRODUCIR SONIDO DE FONDO

    private void reproducirSonidoFondo() {
        try {
            URL urlSonido = getClass().getResource("/audio/sonidojuego.wav");

            if (urlSonido != null) {

                AudioInputStream audio = AudioSystem.getAudioInputStream(urlSonido);

                sonidoFondo = AudioSystem.getClip();
                sonidoFondo.open(audio);

                if (sonidoFondo.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volumen = (FloatControl) sonidoFondo.getControl(
                        FloatControl.Type.MASTER_GAIN
                    );

                    volumen.setValue(-10.0f);
                }

                sonidoFondo.loop(Clip.LOOP_CONTINUOUSLY);

                audio.close();

            } else {
                System.out.println("No se encontró el archivo sonidojuego.wav");
            }

        } catch (Exception e) {
            System.out.println("Error al reproducir el sonido: " + e.getMessage());
        }
    }

    // DETENER SONIDO DE FONDO

    private void detenerSonidoFondo() {
        if (sonidoFondo != null) {
            sonidoFondo.stop();
            sonidoFondo.close();
            sonidoFondo = null;
        }
    }

    // BOTÓN CON IMAGEN DEL LIBRO: ABRIR EL DIARIO DE SUPERVIVENCIA

       private JButton crearBotonDiario() {

        JButton btnDiario = new JButton();

        ImageIcon icono = cargarIcono("/UltimoRef/img/diario.png", 180, 140);

        // Versión con brillo para el efecto hover (se crea una sola vez)
        final ImageIcon iconoHover = crearIconoBrillante(icono);

        if (icono != null) {
            btnDiario.setIcon(icono);
        } else {
            btnDiario.setText("📖 DIARIO");
            btnDiario.setForeground(new Color(232, 211, 165));
            btnDiario.setFont(new Font("SansSerif", Font.BOLD, 14));
        }

        btnDiario.setBorderPainted(false);
        btnDiario.setContentAreaFilled(false);
        btnDiario.setFocusPainted(false);
        btnDiario.setOpaque(false);
        btnDiario.setToolTipText("Abrir diario de supervivencia");
        btnDiario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Efecto al pasar el mouse por encima
        btnDiario.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (iconoHover != null) {
                    btnDiario.setIcon(iconoHover);
                } else {
                    btnDiario.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (icono != null) {
                    btnDiario.setIcon(icono);
                } else {
                    btnDiario.setForeground(new Color(232, 211, 165));
                }
            }
        });

        btnDiario.addActionListener(e -> mostrarDiario());

        return btnDiario;
    }

    /** Crea una copia del ícono con un resplandor dorado y más luminosa. */
    private ImageIcon crearIconoBrillante(ImageIcon original) {

        if (original == null) {
            return null;
        }

        int ancho = original.getIconWidth();
        int alto = original.getIconHeight();

        java.awt.image.BufferedImage salida = new java.awt.image.BufferedImage(
            ancho, alto, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = salida.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Resplandor dorado de fondo
        g2.setColor(new Color(255, 215, 120, 60));
        g2.fillRoundRect(0, 0, ancho, alto, 150, 150);

        // Imagen original encima
        g2.drawImage(original.getImage(), 0, 0, null);

        // Aclarado suave sobre la imagen (solo sobre píxeles visibles)
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_ATOP, 0.25f));
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, ancho, alto);

        g2.dispose();

        return new ImageIcon(salida);
    }
    
    // VENTANA QUE MUESTRA LO QUE FUE SUCEDIENDO DÍA A DÍA

    private void mostrarDiario() {

        JDialog dialogDiario = new JDialog(this, "DIARIO DE SUPERVIVENCIA", true);
        dialogDiario.setLayout(new BorderLayout(10, 10));
        dialogDiario.setSize(650, 520);
        dialogDiario.setLocationRelativeTo(this);

        JPanel panelDiario = new JPanel(new BorderLayout(10, 10));
        panelDiario.setBackground(new Color(32, 25, 18));
        panelDiario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 110, 60), 3),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JLabel titulo = new JLabel(
            "<html><div style='text-align:center;'>"
            + "<h1 style='color:#E8D3A5; margin:0;'>DIARIO DE SUPERVIVENCIA</h1>"
            + "<p style='color:#BDA77D;'>Las anotaciones de todo lo que ocurrió durante tu viaje</p>"
            + "</div></html>",
            JLabel.CENTER
        );
        panelDiario.add(titulo, BorderLayout.NORTH);

        JTextArea areaDiario = new JTextArea();
        areaDiario.setEditable(false);
        areaDiario.setLineWrap(true);
        areaDiario.setWrapStyleWord(true);
        areaDiario.setBackground(new Color(238, 226, 196));
        areaDiario.setForeground(new Color(55, 43, 29));
        areaDiario.setFont(new Font("Serif", Font.PLAIN, 16));
        areaDiario.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        StringBuilder contenido = new StringBuilder();
        contenido.append("PERSONAJE: ").append(juego.getRol()).append("\n");
        contenido.append("DÍA ACTUAL: ").append(juego.getDias()).append("\n");
        contenido.append("==============================================\n\n");

        if (juego.getDiario().isEmpty()) {
            contenido.append("Todavía no hay anotaciones.\n");
            contenido.append("Tus experiencias aparecerán aquí a medida que avances cada día.");
        } else {
            for (String entrada : juego.getDiario()) {
                contenido.append("• ").append(entrada).append("\n\n");
            }
        }

        areaDiario.setText(contenido.toString());
        areaDiario.setCaretPosition(0);

        JScrollPane scrollDiario = new JScrollPane(areaDiario);
        scrollDiario.setBorder(BorderFactory.createLineBorder(new Color(150, 110, 60), 2));
        panelDiario.add(scrollDiario, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("CERRAR DIARIO");
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCerrar.setBackground(new Color(75, 55, 35));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> dialogDiario.dispose());

        JPanel panelCerrar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelCerrar.setOpaque(false);
        panelCerrar.add(btnCerrar);
        panelDiario.add(panelCerrar, BorderLayout.SOUTH);

        dialogDiario.add(panelDiario);
        dialogDiario.setVisible(true);
    }

    // BOTÓN PARA VOLVER A LA PANTALLA DE INICIO

    private JButton crearBotonInicio() {

        JButton btnInicio = new JButton("VOLVER AL INICIO");

        btnInicio.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnInicio.setBackground(new Color(120, 75, 25));
        btnInicio.setForeground(Color.WHITE);
        btnInicio.setFocusPainted(false);
        btnInicio.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 140, 50), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        btnInicio.addActionListener(e -> {

            // ===== [CAMBIO SUPABASE 4] =====
            // Antes decía "Perderás el progreso de la partida actual."
            // Ahora el progreso se guarda, así que se cambia el mensaje
            // y se guarda la partida antes de volver al menú.
            int opcion = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "¿Querés volver al menú principal?\n"
                + "Tu progreso queda guardado y podés retomarlo con CONTINUAR.",
                "VOLVER AL INICIO",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
            );

            if (opcion == javax.swing.JOptionPane.YES_OPTION) {

                detenerSonidoFondo();

                // Se guarda y se espera a que termine, para que el menú
                // encuentre la partida actualizada al consultar CONTINUAR.
                try {
                    SupabaseClient.guardarPartida(juego);
                } catch (Exception ex) {
                    System.out.println("Error al guardar: " + ex.getMessage());
                }
            // ===== FIN CAMBIO 4 =====

                // Primero se abre el menú y recién después se cierra el juego,
                // así nunca queda el escritorio a la vista.
                new Pantallainicio();
                dispose();
            }
        });

        return btnInicio;
    }

    // ETIQUETAS DEL HUD

    private JLabel crearEtiquetaHud(Color color, int estilo) {
        JLabel l = new JLabel();
        l.setForeground(color);
        l.setFont(new Font("SansSerif", estilo, 13));
        return l;
    }

    private JLabel crearEtiquetaConIcono(String rutaRelativa, Color colorTexto) {
        JLabel l = new JLabel();
        l.setForeground(colorTexto);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));

        ImageIcon icono = cargarIcono(rutaRelativa, 18, 18);
        if (icono != null) {
            l.setIcon(icono);
        }

        return l;
    }

    private JLabel crearLabelIconoTexto(String rutaRelativa, String texto) {
        JLabel l = new JLabel(texto);

        ImageIcon icono = cargarIcono(rutaRelativa, 16, 16);
        if (icono != null) {
            l.setIcon(icono);
        }

        return l;
    }

    private JLabel crearSeparadorVertical() {
        JLabel separador = new JLabel("|");
        separador.setForeground(new Color(50, 80, 50));
        separador.setFont(new Font("SansSerif", Font.BOLD, 14));
        return separador;
    }

    // =========================================================================
    // SELECCIÓN DE ROL
    // =========================================================================

    /**
     * Muestra el diálogo de selección de rol (sin bordes, a pantalla completa)
     * sobre la ventana "owner". Devuelve 0 = Médico, 1 = Sobreviviente, -1 = volver.
     */
    private int elegirRol(JFrame owner) {

        JDialog dialogRol = new JDialog(owner, "SELECCIÓN DE CLASE - ÚLTIMO REFUGIO", true);

        // Sin barra de título ni bordes: parece la misma pantalla.
        dialogRol.setUndecorated(true);
        dialogRol.setLayout(new BorderLayout());
        dialogRol.setBounds(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
        );

        JPanel panelFondoRol = new JPanel(new BorderLayout());
        panelFondoRol.setBackground(COLOR_FONDO_GENERAL);
        panelFondoRol.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JPanel panelEncabezado = new JPanel(new BorderLayout(5, 5));
        panelEncabezado.setOpaque(false);

        JLabel lblTitulo = new JLabel(
            "<html><div style='text-align: center; color: #E1EBD7;'>"
            + "<h1 style='font-size: 26px; color: #78C282; margin: 0; letter-spacing: 2px;'>EL ÚLTIMO REFUGIO</h1>"
            + "<p style='font-size: 13px; color: #a3c1ad; margin-top: 5px;'>El mundo civilizado ha colapsado. La infección se extiende. Elige tu especialidad para resistir.</p>"
            + "</div></html>",
            JLabel.CENTER
        );

        panelEncabezado.add(lblTitulo, BorderLayout.CENTER);
        panelFondoRol.add(panelEncabezado, BorderLayout.NORTH);

        JPanel panelRolesDesc = new JPanel(new GridLayout(1, 2, 30, 0));
        panelRolesDesc.setOpaque(false);
        panelRolesDesc.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        final int[] seleccionRol = {-1};

        // TARJETA 1: MÉDICO

        JPanel panelMedico = new JPanel(new BorderLayout(15, 15));
        panelMedico.setBackground(new Color(16, 26, 38));
        panelMedico.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 156, 255), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblImagenMedico = new JLabel();
        lblImagenMedico.setHorizontalAlignment(JLabel.CENTER);
        if (iconoMedicoTarjeta != null) {
            lblImagenMedico.setIcon(iconoMedicoTarjeta);
        }

        panelMedico.add(lblImagenMedico, BorderLayout.NORTH);

        JLabel lblMedicoDesc = new JLabel(
            "<html><div style='color: #E1EBD7; text-align: center;'>"
            + "<h2 style='color: #64B5F6; margin: 0 0 10px 0;'>ROL: MÉDICO</h2>"
            + "<p style='font-size: 11px; line-height: 1.4;'><b>Inventario Inicial:</b><br>4 Medicinas &nbsp;|&nbsp;  2 Provisiones &nbsp;|&nbsp;  0 Munición</p>"
            + "<p style='font-size: 11px; line-height: 1.4; color: #b0bec5;'><b>Enfoque:</b> Ética clínica, investigación científica avanzada y alta eficiencia curativa en hospitales.</p>"
            + "</div></html>",
            JLabel.CENTER
        );

        panelMedico.add(lblMedicoDesc, BorderLayout.CENTER);

              JButton btnMedico = new JButton("SELECCIONAR MÉDICO");
        btnMedico.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnMedico.setBackground(new Color(25, 70, 120));
        btnMedico.setForeground(Color.WHITE);
        btnMedico.setFocusPainted(false);
        btnMedico.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        btnMedico.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Efecto al pasar el mouse por encima
        btnMedico.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnMedico.setBackground(new Color(45, 110, 175));
                btnMedico.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(130, 200, 255), 2),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnMedico.setBackground(new Color(25, 70, 120));
                btnMedico.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            }
        });

        btnMedico.addActionListener(e -> {
            seleccionRol[0] = 0;
            dialogRol.dispose();
        });
        
        panelMedico.add(btnMedico, BorderLayout.SOUTH);

        // TARJETA 2: SOBREVIVIENTE

        JPanel panelSobreviviente = new JPanel(new BorderLayout(15, 15));
        panelSobreviviente.setBackground(new Color(38, 22, 16));
        panelSobreviviente.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 152, 0), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblImagenSobreviviente = new JLabel();
        lblImagenSobreviviente.setHorizontalAlignment(JLabel.CENTER);
        if (iconoSobrevivienteTarjeta != null) {
            lblImagenSobreviviente.setIcon(iconoSobrevivienteTarjeta);
        }

        panelSobreviviente.add(lblImagenSobreviviente, BorderLayout.NORTH);

        JLabel lblSobrevivienteDesc = new JLabel(
            "<html><div style='color: #E1EBD7; text-align: center;'>"
            + "<h2 style='color: #FFB74D; margin: 0 0 10px 0;'>ROL: SOBREVIVIENTE</h2>"
            + "<p style='font-size: 11px; line-height: 1.4;'><b>Inventario Inicial:</b><br> 1 Medicina &nbsp;|&nbsp;  5 Provisiones &nbsp;|&nbsp;  2 Munición</p>"
            + "<p style='font-size: 11px; line-height: 1.4; color: #ffccbc;'><b>Enfoque:</b> Saqueo táctico, expediciones de combate rudo y recolección eficiente en bosques y casas.</p>"
            + "</div></html>",
            JLabel.CENTER
        );

        panelSobreviviente.add(lblSobrevivienteDesc, BorderLayout.CENTER);

               JButton btnSobreviviente = new JButton("SELECCIONAR SOBREVIVIENTE");
        btnSobreviviente.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnSobreviviente.setBackground(new Color(160, 60, 20));
        btnSobreviviente.setForeground(Color.WHITE);
        btnSobreviviente.setFocusPainted(false);
        btnSobreviviente.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        btnSobreviviente.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Efecto al pasar el mouse por encima
        btnSobreviviente.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSobreviviente.setBackground(new Color(210, 90, 35));
                btnSobreviviente.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 190, 100), 2),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSobreviviente.setBackground(new Color(160, 60, 20));
                btnSobreviviente.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            }
        });

        btnSobreviviente.addActionListener(e -> {
            seleccionRol[0] = 1;
            dialogRol.dispose();
        });

        panelSobreviviente.add(btnSobreviviente, BorderLayout.SOUTH);

        panelRolesDesc.add(panelMedico);
        panelRolesDesc.add(panelSobreviviente);

        panelFondoRol.add(panelRolesDesc, BorderLayout.CENTER);

        // PIE: texto + botón para volver al menú (el diálogo no tiene botón de cerrar)

        JPanel panelPie = new JPanel(new BorderLayout(5, 8));
        panelPie.setOpaque(false);

        JLabel lblPie = new JLabel(
            "<html><div style='text-align: center; color: #889988; font-size: 11px;'>Cada elección altera drásticamente los eventos, desafíos y tácticas de supervivencia. Elige con sabiduría.</div></html>",
            JLabel.CENTER
        );

        JButton btnVolver = new JButton("< VOLVER AL MENÚ");
        btnVolver.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnVolver.setBackground(new Color(30, 40, 30));
        btnVolver.setForeground(TEXTO_CLARO);
        btnVolver.setFocusPainted(false);
        btnVolver.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BOTON_BORDE, 2),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        btnVolver.addActionListener(e -> {
            seleccionRol[0] = -1;
            dialogRol.dispose();
        });

        JPanel panelBotonVolver = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBotonVolver.setOpaque(false);
        panelBotonVolver.add(btnVolver);

        panelPie.add(lblPie, BorderLayout.CENTER);
        panelPie.add(panelBotonVolver, BorderLayout.SOUTH);

        panelFondoRol.add(panelPie, BorderLayout.SOUTH);

        dialogRol.add(panelFondoRol);
        dialogRol.setVisible(true); // bloquea hasta que se elija un rol

        return seleccionRol[0];
    }

    /** Prepara la partida con el rol elegido (sin mostrar ni ocultar ventanas). */
    private void iniciarPartida(int r) {

        String rolSeleccionado = (r == 0) ? "Médico" : "Sobreviviente";

        juego = new Juego(rolSeleccionado);
        fondo.setRol(rolSeleccionado);
        fondo.setLugar(Panelfondo.Lugar.PRINCIPAL);

        detenerSonidoFondo();
        reproducirSonidoFondo();

        actualizarPanelLateral(rolSeleccionado);

        txtLog.setText(
            "=== INICIO DE LA TRAVESÍA ===\n"
            + "Elegiste el rol de: " + rolSeleccionado.toUpperCase() + "\n"
            + "Te encuentras a salvo en el refugio inicial. Cada día que pasa consumes 1 provisión.\n"
            + "Elige sabiamente tus acciones y sobrevive al apocalipsis."
        );

        for (JButton b : botones) {
            b.setEnabled(true);
        }

        actualizar();

        // ===== [CAMBIO SUPABASE 5] =====
        // La partida nueva se guarda desde el día 1 (reemplaza la anterior, si había).
        guardarEnNube();
        // ===== FIN CAMBIO 5 =====
    }

    /** Nueva partida desde dentro del juego (botón "JUGAR DE NUEVO"). */
    private void nuevaPartida() {

        int r = elegirRol(this);

        if (r < 0) {
            // Se abre el menú antes de cerrar esta ventana para no dejar huecos.
            new Pantallainicio();
            dispose();
            return;
        }

        iniciarPartida(r);
    }

    // ACTUALIZAR PANEL LATERAL CON ROL E IMAGEN

    private void actualizarPanelLateral(String rol) {

        lblRolLateralTitulo.setText(
            "<html><div style='text-align: center;'>Sos:<br><span style='color: "
            + (rol.equals("Médico") ? "#64B5F6" : "#FFB74D")
            + "; font-size: 14px;'>"
            + rol.toUpperCase()
            + "</span></div></html>"
        );

        ImageIcon icono = rol.equals("Médico") ? iconoMedicoLateral : iconoSobrevivienteLateral;

        if (icono != null) {
            lblImagenPerfilLateral.setText("");
            lblImagenPerfilLateral.setIcon(icono);
        } else {
            lblImagenPerfilLateral.setIcon(null);
            lblImagenPerfilLateral.setText("[Sin Imagen]");
            lblImagenPerfilLateral.setForeground(Color.WHITE);
        }
    }

    // FLUJO PRINCIPAL DE EJECUCIÓN DE ACCIONES

    private void ejecutar(Accion accion) {

        int dia = juego.getDias();

        String textoAccion = juego.jugarDia(accion);

        fondo.setLugar(lugarDe(accion));

        StringBuilder logCompleto = new StringBuilder();

        logCompleto.append("[DÍA ")
            .append(dia)
            .append("] - ACCIÓN REALIZADA\n")
            .append(textoAccion);

        Juego.Decision d = juego.getDecision();

        if (d != null) {

            int opcionElegida = mostrarVentanaDecision(d);

            String resultadoDecision = juego.resolverDecision(opcionElegida);

            mostrarInformeDecision(resultadoDecision);

            logCompleto.append("\n\nSUCESO CRÍTICO:\n")
                .append(resultadoDecision);
        }

        String cierre = juego.cerrarDia();

        logCompleto.append("\n").append(cierre);

        txtLog.setText(logCompleto.toString());
        txtLog.setCaretPosition(0);

        actualizar();

        // ===== [CAMBIO SUPABASE 6] =====
        // Se guarda el progreso al cerrar cada día.
        // Si el personaje murió, se guarda con vivo=false:
        // CONTINUAR queda deshabilitado y la partida terminada no se retoma.
        guardarEnNube();
        // ===== FIN CAMBIO 6 =====

        if (!juego.isVivo()) {
            mostrarFin();
        }
    }

    // VENTANA EMERGENTE DE DECISIONES

    private int mostrarVentanaDecision(Juego.Decision d) {

        final int[] seleccion = {-1};

        JDialog dialog = new JDialog(this, "¡DECISIÓN CRÍTICA!", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(540, 380);
        dialog.setLocationRelativeTo(this);

        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBackground(new Color(25, 15, 15));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblMensajeAlerta = new JLabel(
            "<html><div style='text-align: center; color: #ffcccc;'><h3 style='color: #ff5555; margin-bottom: 5px;'>EVENTO INESPERADO</h3><p style='font-size: 13px;'>"
            + d.texto.replace("\n", "<br>")
            + "</p></div></html>",
            JLabel.CENTER
        );

        panelPrincipal.add(lblMensajeAlerta, BorderLayout.NORTH);

        JPanel panelOpciones = new JPanel(new GridLayout(d.opciones.length, 1, 10, 10));
        panelOpciones.setBackground(new Color(25, 15, 15));

        for (int i = 0; i < d.opciones.length; i++) {

            final int index = i;

            JButton btnOpcion = new JButton("> " + d.opciones[i]);
            btnOpcion.setFont(new Font("SansSerif", Font.BOLD, 13));
            btnOpcion.setBackground(new Color(45, 25, 25));
            btnOpcion.setForeground(new Color(240, 230, 220));
            btnOpcion.setFocusPainted(false);
            btnOpcion.setBorder(BorderFactory.createLineBorder(new Color(120, 40, 40), 2));

            btnOpcion.addActionListener(e -> {
                seleccion[0] = index;
                dialog.dispose();
            });

            panelOpciones.add(btnOpcion);
        }

        panelPrincipal.add(panelOpciones, BorderLayout.CENTER);

        dialog.add(panelPrincipal);
        dialog.setVisible(true);

        return seleccion[0] != -1 ? seleccion[0] : 0;
    }

    // VENTANA DE INFORME DE RESULTADOS

    private void mostrarInformeDecision(String resultado) {

        JDialog dialogInforme = new JDialog(this, "INFORME DE RESULTADOS", true);
        dialogInforme.setLayout(new BorderLayout());
        dialogInforme.setSize(480, 260);
        dialogInforme.setLocationRelativeTo(this);

        JPanel panelInforme = new JPanel(new BorderLayout(15, 15));
        panelInforme.setBackground(new Color(15, 25, 20));
        panelInforme.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTituloInforme = new JLabel(
            "<html><div style='text-align: center; color: #aaffaa;'><h3>CONSECUENCIAS DE TU ELECCIÓN</h3></div></html>",
            JLabel.CENTER
        );

        panelInforme.add(lblTituloInforme, BorderLayout.NORTH);

        JTextArea areaResultado = new JTextArea(resultado);
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setOpaque(false);
        areaResultado.setForeground(new Color(220, 235, 220));
        areaResultado.setFont(new Font("SansSerif", Font.BOLD, 14));

        JScrollPane scrollRes = new JScrollPane(areaResultado);
        scrollRes.setOpaque(false);
        scrollRes.getViewport().setOpaque(false);
        scrollRes.setBorder(BorderFactory.createLineBorder(new Color(50, 90, 60), 1));

        panelInforme.add(scrollRes, BorderLayout.CENTER);

        JButton btnContinuar = new JButton("Continuar Jornada ->");
        btnContinuar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnContinuar.setBackground(new Color(30, 60, 40));
        btnContinuar.setForeground(Color.WHITE);
        btnContinuar.setFocusPainted(false);
        btnContinuar.addActionListener(e -> dialogInforme.dispose());

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.setBackground(new Color(15, 25, 20));
        panelSur.add(btnContinuar);

        panelInforme.add(panelSur, BorderLayout.SOUTH);

        dialogInforme.add(panelInforme);
        dialogInforme.setVisible(true);
    }

    // DETERMINAR LUGAR SEGÚN ACCIÓN

    private Panelfondo.Lugar lugarDe(Accion a) {

        switch (a) {
            case HOSPITAL:
                return Panelfondo.Lugar.HOSPITAL;

            case BOSQUE:
                return Panelfondo.Lugar.BOSQUE;

            case CASA:
                return Panelfondo.Lugar.CASA;

            case MERCADO:
                return Panelfondo.Lugar.MERCADO;

            case COMISARIA:
                return Panelfondo.Lugar.COMISARIA;

            case GRANJA:
                return Panelfondo.Lugar.GRANJA;

            case SOTANO:
                return Panelfondo.Lugar.SOTANO;

            default:
                return Panelfondo.Lugar.REFUGIO;
        }
    }

    // ESTADÍSTICAS EN PANTALLA

    private void actualizar() {

        int s = juego.getSalud();

        lblRol.setText(" ROL: " + juego.getRol().toUpperCase());
        lblDia.setText(" DÍA: " + juego.getDias());

        barraSalud.setValue(s);
        barraSalud.setString(s + "%");

        barraSalud.setForeground(
            s > 50
                ? new Color(60, 180, 90)
                : s > 25
                    ? new Color(220, 150, 30)
                    : new Color(200, 50, 50)
        );

        lblProv.setText(" Provisiones: " + juego.getProvisiones());
        lblMed.setText(" Medicinas: " + juego.getMedicinas());
        lblMun.setText(" Munición: " + juego.getMunicion());
        lblDef.setText(" Defensa: " + juego.getDefensa() + "/3");
        lblPunt.setText(" PUNTOS: " + juego.getPuntuacion());
    }

    // PANTALLA DE FIN DE JUEGO

    private void mostrarFin() {

        detenerSonidoFondo();

        for (JButton b : botones) {
            b.setEnabled(false);
        }

        mejorPuntuacion = Math.max(mejorPuntuacion, juego.getPuntuacion());

        StringBuilder sb = new StringBuilder();

        sb.append("=== BITÁCORA / REGISTRO HISTÓRICO DE SUPERVIVENCIA ===\n\n");

        for (String entrada : juego.getDiario()) {
            sb.append("  • ").append(entrada).append("\n");
        }

        JDialog dialogFin = new JDialog(
            this,
            "INFORME FINAL - CAPÍTULO TERMINADO",
            true
        );

        dialogFin.setUndecorated(true);
        dialogFin.setLayout(new BorderLayout());
        dialogFin.setBounds(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
        );

        JPanel panelFondoFin = new JPanel(new BorderLayout());
        panelFondoFin.setBackground(new Color(18, 10, 10));
        panelFondoFin.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JPanel panelContenidoFin = new JPanel(new BorderLayout(15, 15));
        panelContenidoFin.setBackground(new Color(28, 16, 16));
        panelContenidoFin.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 60, 50), 2),
            BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));

        JLabel lblFinTitulo = new JLabel(
            "<html><div style='text-align: center; color: #E1EBD7;'>"
            + "<h1 style='color: #FF5555; margin: 0; font-size: 26px;'> EL REFUGIO HA CAÍDO</h1>"
            + "<p style='font-size: 13px; color: #ffcccc; margin-top: 5px;'>El personaje <b>["
            + juego.getRol().toUpperCase()
            + "]</b> no pudo soportar las hostilidades del exterior.</p>"
            + "</div></html>",
            JLabel.CENTER
        );

        panelContenidoFin.add(lblFinTitulo, BorderLayout.NORTH);

        JPanel panelCentroReporte = new JPanel(new BorderLayout(10, 10));
        panelCentroReporte.setOpaque(false);

        JPanel panelStats = new JPanel(new GridLayout(1, 3, 15, 0));
        panelStats.setOpaque(false);
        panelStats.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        panelStats.add(crearTarjetaStat(
            "DÍAS SOBREVIVIDOS",
            String.valueOf(juego.getDias()),
            new Color(100, 150, 200)
        ));

        panelStats.add(crearTarjetaStat(
            "PUNTUACIÓN TOTAL",
            juego.getPuntuacion() + " pts",
            new Color(255, 215, 0)
        ));

        panelStats.add(crearTarjetaStat(
            "RÉCORD DE SESIÓN",
            mejorPuntuacion + " pts",
            new Color(100, 220, 120)
        ));

        panelCentroReporte.add(panelStats, BorderLayout.NORTH);

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(14, 8, 8));
        area.setForeground(new Color(230, 215, 205));
        area.setFont(new Font("Monospaced", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(120, 40, 40), 1));

        panelCentroReporte.add(scrollPane, BorderLayout.CENTER);

        panelContenidoFin.add(panelCentroReporte, BorderLayout.CENTER);

        JPanel panelBotonesFin = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        panelBotonesFin.setOpaque(false);

        JButton btnReiniciar = new JButton("JUGAR DE NUEVO");
        btnReiniciar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnReiniciar.setBackground(new Color(40, 90, 50));
        btnReiniciar.setForeground(Color.WHITE);
        btnReiniciar.setPreferredSize(new Dimension(220, 45));
        btnReiniciar.setFocusPainted(false);

        JButton btnSalir = new JButton("SALIR DEL JUEGO");
        btnSalir.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnSalir.setBackground(new Color(120, 40, 40));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setPreferredSize(new Dimension(220, 45));
        btnSalir.setFocusPainted(false);

        final int[] opcionFin = {-1};

        btnReiniciar.addActionListener(e -> {
            opcionFin[0] = 0;
            dialogFin.dispose();
        });

        btnSalir.addActionListener(e -> {
            opcionFin[0] = 1;
            dialogFin.dispose();
        });

        panelBotonesFin.add(btnReiniciar);
        panelBotonesFin.add(btnSalir);

        panelContenidoFin.add(panelBotonesFin, BorderLayout.SOUTH);

        panelFondoFin.add(panelContenidoFin, BorderLayout.CENTER);

        dialogFin.add(panelFondoFin);
        dialogFin.setVisible(true);

        if (opcionFin[0] == 0) {
            nuevaPartida();
        } else {
            System.exit(0);
        }
    }

    // CREAR TARJETAS DE ESTADÍSTICAS

    private JPanel crearTarjetaStat(String titulo, String valor, Color colorValor) {

        JPanel tarjeta = new JPanel(new BorderLayout(5, 5));
        tarjeta.setBackground(new Color(36, 22, 22));

        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorValor, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTit = new JLabel(
            "<html><div style='text-align: center; color: #aaaaaa; font-size: 10px;'>"
            + titulo
            + "</div></html>",
            JLabel.CENTER
        );

        JLabel lblVal = new JLabel(
            "<html><div style='text-align: center; color: "
            + colorToHex(colorValor)
            + "; font-size: 16px; font-weight: bold;'>"
            + valor
            + "</div></html>",
            JLabel.CENTER
        );

        tarjeta.add(lblTit, BorderLayout.NORTH);
        tarjeta.add(lblVal, BorderLayout.CENTER);

        return tarjeta;
    }

    // CONVERTIR COLOR A HEXADECIMAL

    private String colorToHex(Color c) {
        return String.format(
            "#%02x%02x%02x",
            c.getRed(),
            c.getGreen(),
            c.getBlue()
        );
    }
}