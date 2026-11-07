package UltimoRef;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;

public class Pantallainicio extends JFrame {

    // COLORES

    private final Color VERDE = new Color(75, 145, 85);
    private final Color TEXTO = new Color(225, 235, 215);
    private final Color FONDO_BOTON = new Color(15, 25, 17);

    // VARIABLE DE MUSICA

    private Clip musica;

    // ===== [CAMBIO SUPABASE 1] =====
    // Indica si el usuario tiene una partida guardada en Supabase.
    // Se usa para avisar antes de pisarla al empezar una partida nueva.
    private boolean hayGuardada = false;
    // ===== FIN CAMBIO 1 =====

    // CONSTRUCTOR

    public Pantallainicio() {

        setTitle("EL ÚLTIMO REFUGIO");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // FONDO DEL MENU

        JPanel fondo = new JPanel() {

            private Image imagen;

            {
                URL url = getClass().getResource(
                    "/UltimoRef/img/MenuPrincipal.png"
                );

                if (url != null) {
                    imagen = new ImageIcon(url).getImage();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                if (imagen != null) {

                    g.drawImage(
                        imagen, 0, 0,
                        getWidth(), getHeight(), this
                    );

                } else {

                    g.setColor(new Color(15, 25, 17));
                    g.fillRect(0, 0, getWidth(), getHeight());

                }

                // Oscurecer fondo
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setColor(new Color(0, 0, 0, 155));

                g2.fillRect(
                    0, 0, getWidth(), getHeight()
                );

                // Degradado
                GradientPaint degradado = new GradientPaint(
                    0, 0,
                    new Color(0, 0, 0, 180),
                    getWidth(), 0,
                    new Color(0, 0, 0, 50)
                );

                g2.setPaint(degradado);

                g2.fillRect(
                    0, 0, getWidth(), getHeight()
                );

                g2.dispose();
            }
        };

        fondo.setLayout(new GridBagLayout());

        // PANEL PRINCIPAL

        JPanel menu = new JPanel();

        menu.setLayout(
            new BoxLayout(menu, BoxLayout.Y_AXIS)
        );

        menu.setOpaque(false);

        // TITULO

        TituloAnimado titulo = new TituloAnimado(
            "EL ÚLTIMO REFUGIO"
        );

        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        titulo.setForeground(TEXTO);

        titulo.setFont(
            new Font("Serif", Font.BOLD, 55)
        );

        // SUBTITULO

        JLabel subtitulo = new JLabel(
            "LA CIVILIZACIÓN CAYÓ. SOBREVIVIR ES TU ÚNICA OPCIÓN."
        );

        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitulo.setForeground(
            new Color(190, 210, 185)
        );

        subtitulo.setFont(
            new Font("SansSerif", Font.BOLD, 14)
        );

        // LINEA DECORATIVA

        JPanel linea = new JPanel();

        linea.setMaximumSize(
            new Dimension(400, 2)
        );

        linea.setBackground(VERDE);

        linea.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        // BOTON JUGAR

        JButton btnJugar = crearBoton("JUGAR");

        btnJugar.addActionListener(e -> iniciarJuego());

        // BOTON CONTINUAR

        // ===== [CAMBIO SUPABASE 2] =====
        // Antes: el botón quedaba siempre deshabilitado.
        // Ahora: se consulta en segundo plano si hay partida guardada
        // y, si existe, se habilita y se conecta con continuarJuego().
        JButton btnContinuar = crearBoton("CONTINUAR");

        btnContinuar.setEnabled(false);

        btnContinuar.setToolTipText(
            "Todavía no hay partidas guardadas"
        );

        btnContinuar.addActionListener(e -> continuarJuego());

        // Consultar en segundo plano (para no congelar el menú)
        new Thread(() -> {
            try {
                Juego guardado = SupabaseClient.cargarPartida();
                if (guardado != null) {
                    SwingUtilities.invokeLater(() -> {
                        hayGuardada = true;
                        btnContinuar.setEnabled(true);
                        btnContinuar.setToolTipText(
                            "Día " + guardado.getDias() + " · " + guardado.getRol());
                    });
                }
            } catch (Exception ex) {
                System.out.println("No se pudo consultar la partida: " + ex.getMessage());
            }
        }).start();
        // ===== FIN CAMBIO 2 =====

        // BOTON COMO JUGAR

        JButton btnComoJugar = crearBoton("CÓMO JUGAR");

        btnComoJugar.addActionListener(
            e -> mostrarInstrucciones()
        );

        // ===== [CAMBIO SUPABASE 3] =====
        // Botón nuevo: cerrar sesión y volver a la pantalla de login.
        JButton btnUsuario = crearBoton("CAMBIAR DE USUARIO");

        btnUsuario.addActionListener(e -> {
            detenerMusica();
            SupabaseClient.cerrarSesion();
            new PantallaLogin();
            dispose();
        });
        // ===== FIN CAMBIO 3 =====

        // ===== [CAMBIO RANKING 1] =====
        // Botón nuevo: abre el top 10 de jugadores.
        JButton btnRanking = crearBoton("RANKING");

        btnRanking.addActionListener(e -> new PantallaRanking(this));
        // ===== FIN CAMBIO RANKING 1 =====

        // BOTON SALIR

        JButton btnSalir = crearBoton("SALIR");

        btnSalir.addActionListener(e -> {

            int respuesta = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de que querés abandonar el juego?",
                "SALIR DEL JUEGO",
                JOptionPane.YES_NO_OPTION
            );

            if (respuesta == JOptionPane.YES_OPTION) {

                detenerMusica();

                dispose();

                System.exit(0);
            }
        });

        // AGREGAR COMPONENTES

        menu.add(titulo);

        menu.add(Box.createVerticalStrut(12));

        menu.add(subtitulo);

        menu.add(Box.createVerticalStrut(20));

        menu.add(linea);

        menu.add(Box.createVerticalStrut(45));

        menu.add(btnJugar);

        menu.add(Box.createVerticalStrut(12));

        menu.add(btnContinuar);

        menu.add(Box.createVerticalStrut(12));

        menu.add(btnComoJugar);

        menu.add(Box.createVerticalStrut(12));

        // ===== [CAMBIO RANKING 1 - continuación] =====
        menu.add(btnRanking);

        menu.add(Box.createVerticalStrut(12));
        // ===== FIN CAMBIO RANKING 1 =====

        // ===== [CAMBIO SUPABASE 3 - continuación] =====
        menu.add(btnUsuario);

        menu.add(Box.createVerticalStrut(12));
        // ===== FIN CAMBIO 3 =====

        menu.add(btnSalir);

        menu.add(Box.createVerticalStrut(40));

        // FRASE INFERIOR

        JLabel frase = new JLabel(
            "\"Cada día es una nueva oportunidad para sobrevivir.\""
        );

        frase.setForeground(
            new Color(190, 205, 185)
        );

        frase.setFont(
            new Font("Serif", Font.ITALIC, 16)
        );

        frase.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        menu.add(frase);

        // ===== [CAMBIO SUPABASE 4] =====
        // Muestra el nombre del jugador que inició sesión.
        menu.add(Box.createVerticalStrut(10));

        JLabel lblJugador = new JLabel("Jugador: " + SupabaseClient.username);
        lblJugador.setForeground(VERDE);
        lblJugador.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblJugador.setAlignmentX(Component.CENTER_ALIGNMENT);

        menu.add(lblJugador);
        // ===== FIN CAMBIO 4 =====

        // UBICAR MENU

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.anchor = GridBagConstraints.CENTER;

        fondo.add(menu, gbc);

        setContentPane(fondo);

        // CERRAR VENTANA

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                detenerMusica();

            }
        });

        setVisible(true);

        // ANIMACION Y MUSICA

        titulo.iniciarAnimacion();

        reproducirMusica();
    }

    // TITULO ANIMADO

    private class TituloAnimado extends JLabel {

        private float opacidad = 0f;

        public TituloAnimado(String texto) {

            super(texto);
        }

        public void iniciarAnimacion() {

            Timer temporizador = new Timer(40, e -> {

                opacidad += 0.025f;

                if (opacidad >= 1f) {

                    opacidad = 1f;

                    ((Timer) e.getSource()).stop();
                }

                repaint();
            });

            temporizador.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    opacidad
                )
            );

            super.paintComponent(g2);

            g2.dispose();
        }
    }

    // CREAR BOTONES

    private JButton crearBoton(String texto) {

        JButton boton = new JButton(texto);

        boton.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        boton.setPreferredSize(
            new Dimension(310, 48)
        );

        boton.setMaximumSize(
            new Dimension(310, 48)
        );

        boton.setMinimumSize(
            new Dimension(310, 48)
        );

        boton.setFont(
            new Font("SansSerif", Font.BOLD, 15)
        );

        boton.setForeground(TEXTO);

        boton.setBackground(FONDO_BOTON);

        boton.setFocusPainted(false);

        boton.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setBorder(
            BorderFactory.createCompoundBorder(

                BorderFactory.createLineBorder(
                    new Color(80, 130, 80), 2
                ),

                BorderFactory.createEmptyBorder(
                    10, 20, 10, 20
                )
            )
        );

        boton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {

                if (boton.isEnabled()) {

                    boton.setBackground(
                        new Color(55, 100, 60)
                    );

                    boton.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {

                boton.setBackground(FONDO_BOTON);

                boton.setForeground(TEXTO);
            }
        });

        return boton;
    }

    // INICIAR PARTIDA (SIN HUECOS ENTRE PANTALLAS)

    private void iniciarJuego() {

        // ===== [CAMBIO SUPABASE 5] =====
        // Si ya hay una partida guardada, se avisa que empezar una nueva la reemplaza.
        // (Solo se reemplaza si después elige rol; si vuelve atrás, queda intacta.)
        if (hayGuardada) {
            int r = JOptionPane.showConfirmDialog(this,
                "Ya tenés una partida guardada.\nSi empezás una nueva, se va a reemplazar.\n¿Querés continuar?",
                "NUEVA PARTIDA", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }
        // ===== FIN CAMBIO 5 =====

        // El menú sigue visible mientras se carga todo lo demás.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // invokeLater permite que el cursor se actualice antes de la carga pesada.
        SwingUtilities.invokeLater(() -> {

            // Se construye la ventana del juego (imágenes, HUD, etc.) con el menú aún a la vista.
            Ventanajuego juego = new Ventanajuego();

            // Muestra la selección de rol por encima del menú y, al terminar,
            // abre el juego. Devuelve false si el jugador volvió atrás.
            boolean iniciado = juego.iniciarDesde(this);

            if (iniciado) {
                detenerMusica();
                dispose(); // El menú se cierra recién cuando el juego ya está visible.
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    // ===== [CAMBIO SUPABASE 6] =====
    // MÉTODO NUEVO: CONTINUAR PARTIDA GUARDADA
    // Carga la partida desde Supabase (en segundo plano) y abre el juego donde se dejó.

    private void continuarJuego() {

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Juego, Void>() {

            @Override
            protected Juego doInBackground() throws Exception {
                return SupabaseClient.cargarPartida();
            }

            @Override
            protected void done() {
                try {
                    Juego guardado = get();

                    if (guardado == null) {
                        JOptionPane.showMessageDialog(Pantallainicio.this,
                            "No hay una partida guardada.");
                        setCursor(Cursor.getDefaultCursor());
                        return;
                    }

                    Ventanajuego v = new Ventanajuego();
                    v.continuarDesde(Pantallainicio.this, guardado);

                    detenerMusica();
                    dispose();

                } catch (Exception ex) {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(Pantallainicio.this,
                        "No se pudo cargar la partida:\n" + ex.getMessage());
                }
            }
        }.execute();
    }
    // ===== FIN CAMBIO 6 =====

    // INSTRUCCIONES

    private void mostrarInstrucciones() {

        String instrucciones =

            "BIENVENIDO A EL ÚLTIMO REFUGIO\n\n" +

            "El mundo ha sido devastado por una infección.\n" +
            "Tu objetivo es sobrevivir la mayor cantidad de días posible.\n\n" +

            "¿CÓMO JUGAR?\n\n" +

            "1. Elegí tu personaje: Médico o Sobreviviente.\n\n" +

            "2. Cada día deberás realizar una acción.\n\n" +

            "3. Explorá diferentes lugares para conseguir recursos:\n" +
            "   Hospital, bosque, mercado, granja y comisaría.\n\n" +

            "4. Administrá tus recursos:\n" +
            "   Salud, medicinas, provisiones, munición y defensa.\n\n" +

            "5. Cada día consumís una provisión.\n\n" +

            "6. Tomá decisiones cuidadosamente.\n\n" +

            "OBJETIVO: SOBREVIVIR TODO LO QUE PUEDAS.";

        JTextArea area = new JTextArea(instrucciones);

        area.setEditable(false);

        area.setLineWrap(true);

        area.setWrapStyleWord(true);

        area.setBackground(
            new Color(18, 27, 19)
        );

        area.setForeground(TEXTO);

        area.setFont(
            new Font("Monospaced", Font.PLAIN, 13)
        );

        area.setBorder(
            BorderFactory.createEmptyBorder(
                15, 15, 15, 15
            )
        );

        JScrollPane scroll = new JScrollPane(area);

        scroll.setPreferredSize(
            new Dimension(530, 450)
        );

        JOptionPane.showMessageDialog(
            this,
            scroll,
            "MANUAL DE SUPERVIVENCIA",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // REPRODUCIR AUDIO WAV EN BUCLE

    private void reproducirMusica() {

        try {

            URL sonido = getClass().getResource(
                "/audio/Sonidoambiental.wav"
            );

            if (sonido != null) {

                AudioInputStream audio =
                    AudioSystem.getAudioInputStream(sonido);

                musica = AudioSystem.getClip();

                musica.open(audio);

                // Reproducir infinitamente
                musica.loop(Clip.LOOP_CONTINUOUSLY);

            } else {

                System.out.println(
                    "No se encontró Sonidoambiental.wav"
                );

            }

        } catch (Exception e) {

            System.out.println(
                "Error al reproducir música: " + e.getMessage()
            );

        }
    }

    // DETENER AUDIO

    private void detenerMusica() {

        if (musica != null) {

            musica.stop();

            musica.close();

            musica = null;
        }
    }
}