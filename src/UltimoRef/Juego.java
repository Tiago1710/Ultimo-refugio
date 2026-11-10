package UltimoRef;

import java.util.ArrayList; //se usa para crear el diario en lista.
import java.util.List;
import java.util.Random; //Genera los números aleatorios. Se usa para decidir si sale un evento y si sale o no.
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lógica completa con 10 eventos únicos por lugar para Médicos y Sobrevivientes. */
public class Juego {

    public enum Accion { DESCANSAR, USAR_MEDICINA, FORTIFICAR, HOSPITAL, BOSQUE, CASA, MERCADO, COMISARIA, GRANJA, SOTANO }

    public static class Decision {
        public final int tipo;
        public final String texto;
        /** Opciones tal como se muestran al jugador: sin avisar cuánta salud se pierde. */
        public final String[] opciones;
        /** Opciones completas (con todos sus efectos). Solo las usa la lógica del juego. */
        final String[] reales;
        
        Decision(int tipo, String texto, String... opciones) {
            this.tipo = tipo;
            this.texto = texto;
            this.reales = opciones;
            this.opciones = new String[opciones.length];
            for (int i = 0; i < opciones.length; i++) {
                this.opciones[i] = ocultarDanio(opciones[i]);
            }
        }
    }
    
    /** El jugador decide terminar su partida. */
public void rendirse() {
    salud = 0;
    vivo = false;
    diario.add("Día " + dias + ": Decidiste poner fin a tu travesía.");
}

    /**
     * Detecta los efectos escritos en el texto de cada opción, por ejemplo
     * "(-1 Medicina, +25 Puntos)" o "(+2 Provisiones)". Solo cuentan los números con signo.
     */
    private static final Pattern EFECTO = Pattern.compile(
            "([+\\-\\u2212])\\s*(\\d+)\\s+(Salud|Provisi[oó]n(?:es)?|Medicinas?|Munici[oó]n(?:es)?|Defensa|Puntos?)",
            Pattern.CASE_INSENSITIVE);

    /** Opciones de tipo "prudente" (ignorar, retirarse, rodear...). Tienen menos riesgo que las audaces. */
    private static final Pattern PRUDENTE = Pattern.compile(
            "^(ignor|dej|retir|rode|march|alej|evit|contin|cerrar|descart|pasar de|no |abandon|rechaz|bajar|dar media|dar la vuelta|apagar|salir|espant|ocult|esper|rend|respet|marcar|guardar|desconectar todo|seguir( de largo| el cauce)?$)",
            Pattern.CASE_INSENSITIVE);

    /** Quita del texto visible los avisos de pérdida de salud, para que el daño sea una sorpresa. */
    private static String ocultarDanio(String opcion) {
        String s = opcion;
        s = s.replaceAll("\\s*\\(\\s*[-\\u2212]\\d+\\s+Salud\\s*\\)", "");      // "(-5 Salud)"
        s = s.replaceAll("\\s*,\\s*[-\\u2212]\\d+\\s+Salud", "");                 // ", -5 Salud"
        s = s.replaceAll("[-\\u2212]\\d+\\s+Salud\\s*,\\s*", "");                 // "-5 Salud, "
        return s;
    }

    /** Resultado inventado para las opciones que no tienen efectos escritos. */
    private static class Giro {
        final String texto;
        int salud, prov, med, mun, def, pts;
        Giro(String texto) { this.texto = texto; }
    }

    private final String rol;
    private int salud = 100;
    private int provisiones;
    private int medicinas;
    private int municion;
    private int defensa = 0;
    private int dias = 1;
    private int puntuacion = 0;
    private boolean vivo = true;
    private int ultimoDano;
    private Decision decision;

    private int misionesCumplidas = 0;

    private final List<String> diario = new ArrayList<>();
    private final Random random = new Random();
    private StringBuilder log = new StringBuilder();

    public Juego(String rol) {
        this.rol = rol;
        if (rol.equals("Médico")) {
            medicinas = 4;
            provisiones = 2;
            municion = 0;
        } else {
            medicinas = 1;
            provisiones = 5;
            municion = 2;
        }
    }

 /* Conexión con la bdd */
 public Juego(String rol, int salud, int provisiones, int medicinas, int municion,
             int defensa, int dias, int puntuacion, List<String> diario) {
    this.rol = rol;
    this.salud = salud;
    this.provisiones = provisiones;
    this.medicinas = medicinas;
    this.municion = municion;
    this.defensa = defensa;
    this.dias = dias;
    this.puntuacion = puntuacion;
    this.diario.addAll(diario);
}   
    
    public String jugarDia(Accion accion) {
        log = new StringBuilder();
        decision = null;
        switch (accion) {
            case DESCANSAR:     descansar();      break;
            case USAR_MEDICINA: usarMedicina();   break;
            case FORTIFICAR:    fortificar();     break;
            case HOSPITAL:      eventoHospital(); break;
            case BOSQUE:        eventoBosque();   break;
            case CASA:          eventoCasa();     break;
            case MERCADO:       eventoMercado();  break;
            case COMISARIA:     eventoComisaria();break;
            case GRANJA:        eventoGranja();   break;
            case SOTANO:        eventoSotano();   break;
        }
        
        eventoAsimetricoPorRol(accion);
        return log.toString();
    }

    public String cerrarDia() {
        log = new StringBuilder();
        provisiones--;
        if (provisiones < 0) {
            provisiones = 0;
            salud -= 20;
            log.append("\nTe quedaste sin provisiones. Pierdes 20 de salud por hambre.");
            diario.add("Día " + dias + ": Pasó hambre (-20 Salud).");
        }
        if (salud <= 0) {
            salud = 0;
            vivo = false;
            log.append("\nTu cuerpo no aguantó más...");
            diario.add("Día " + dias + ": Murió. Sus heridas fueron demasiado graves.");
        } else {
            puntuacion += 10;
            dias++;
        }
        return log.toString();
    }

    private void registrar(String pantalla, String resumen) {
        log.append(pantalla);
        diario.add("Día " + dias + ": " + resumen);
    }

    private String golpe(int base) {
        int total = base + ((dias - 1) / 5) * 3;
        String extra = "";
        if (municion > 0) {
            municion--;
            total /= 2;
            extra = " Gastas 1 munición para defenderte.";
        }
        salud -= total;
        ultimoDano = total;
        return extra + " (-" + total + " Salud)";
    }

    private void descansar() {
        salud = Math.min(100, salud + 10);
        registrar("Descansas en el refugio (+10 Salud).", "Descansó en el refugio (+10 Salud).");
    }

    private void usarMedicina() {
        if (medicinas > 0) {
            medicinas--;
            salud = Math.min(100, salud + 30);
            registrar("Usas una medicina (+30 Salud).", "Usó una medicina en el refugio (+30 Salud).");
        } else {
            salud = Math.min(100, salud + 10);
            registrar("No tienes medicinas. Solo pudiste descansar (+10 Salud).", "Intentó curarse pero no tenía medicinas.");
        }
    }

    private void fortificar() {
        if (defensa < 3) {
            defensa++;
            puntuacion += 5;
            registrar("Refuerzas puertas y ventanas (Defensa: " + defensa + "/3).", "Fortificó el refugio (Defensa " + defensa + "/3).");
        } else {
            salud = Math.min(100, salud + 5);
            registrar("El refugio está al máximo. Descansas (+5 Salud).", "Descansó en un refugio completamente fortificado (+5 Salud).");
        }
    }

    private void eventoHospital() {
        if (rol.equals("Médico")) {
            medicinas += 3;
            puntuacion += 20;
            registrar("¡Destreza clínica! Encuentras un stock farmacéutico intacto (+3 Medicinas).", "Inspeccionó el hospital con maestría médica (+3 Medicinas).");
        } else {
            medicinas += 1;
            puntuacion += 10;
            registrar("Rebuscas en la farmacia del hospital con cautela (+1 Medicina).", "Encontró insumos médicos en el hospital (+1 Medicina).");
        }
    }

    private void eventoBosque() {
        if (rol.equals("Sobreviviente")) {
            provisiones += 3;
            puntuacion += 20;
            registrar("¡Instinto de supervivencia! Cazas y recolectas con gran eficiencia (+3 Provisiones).", "Cazó y recolectó con éxito en el bosque.");
        } else {
            provisiones += 1;
            puntuacion += 10;
            registrar("Recoges algunos frutos silvestres del bosque (+1 Provisión).", "Recolectó recursos básicos en el bosque.");
        }
    }

    private void eventoCasa() {
        provisiones += 1;
        puntuacion += 10;
        registrar("Registras una casa abandonada y encuentras comida (+1 Provisión).", "Rebuscó en una casa abandonada (+1 Provisión).");
    }

    private void eventoMercado() {
        provisiones += 2;
        puntuacion += 15;
        registrar("Exploras los pasillos del mercado y hallas provisiones (+2 Provisiones).", "Saqueó un mercado abandonado (+2 Provisiones).");
    }

    private void eventoComisaria() {
        municion += 2;
        puntuacion += 15;
        registrar("Rebuscas en los cajones de la comisaría y encuentras munición (+2 Munición).", "Inspeccionó la comisaría (+2 Munición).");
    }

    private void eventoGranja() {
        provisiones += 2;
        puntuacion += 15;
        registrar("Inspeccionas los campos de cultivo de la granja y logras cosechar (+2 Provisiones).", "Inspeccionó la granja (+2 Provisiones).");
    }

    private void eventoSotano() {
        medicinas += 1;
        puntuacion += 15;
        registrar("Revisas los estantes del sótano y encuentras viejos fármacos (+1 Medicina).", "Exploró el sótano del refugio (+1 Medicina).");
    }

    // =========================================================================
    // SISTEMA DE 10 EVENTOS ÚNICOS POR LUGAR (DIVIDIDO POR ROL)
    // =========================================================================

    private void eventoAsimetricoPorRol(Accion accion) {
        // Los eventos tácticos aparecen con mayor frecuencia y aumentan con los días.
        // En el día 1 hay un 73% de probabilidad; nunca supera el 95%.
        int probabilidad = Math.min(95, 70 + dias * 3);
        if (random.nextInt(100) >= probabilidad) {
            return;
        }
        
        log.append("\n\n— ⚡ ¡EVENTO TÁCTICO DE ZONA [" + rol.toUpperCase() + "]! —\n");
        int subEvento = random.nextInt(10); // 10 opciones de eventos independientes por cada lugar

        if (rol.equals("Médico")) {
            // =========================================================================
            // 10 EVENTOS CLÍNICOS Y CIENTÍFICOS EXCLUSIVOS DEL MÉDICO
            // =========================================================================
            switch (accion) {
                case GRANJA:
                    if (subEvento == 0) {
                        decision = new Decision(310, "En el granero encuentras a un peón moribundo con una infección bacteriana agresiva.", "Administrar antibióticos de tu reserva (-1 Medicina, +25 Puntos)", "Usar remedios naturales experimentales (-5 Salud)", "Dejarlo a su suerte");
                    } else if (subEvento == 1) {
                        decision = new Decision(311, "Descubres un viejo alambique rural oculto donde destilaban tinturas medicinales.", "Preparar alcohol antiséptico refinado (+2 Medicinas)", "Desarmar el cobre para trueque (+1 Provisión)", "Ignorar el aparato");
                    } else if (subEvento == 2) {
                        decision = new Decision(312, "Una colmena de abejas mutadas bloquea el acceso a un invernadero con hierbas curativas.", "Usar humo para calmar a los insectos y cosechar (+1 Medicina, +1 Provisión)", "Esparcir insecticida químico rústico (-5 Salud)", "Rodear el invernadero perdiendo tiempo");
                    } else if (subEvento == 3) {
                        decision = new Decision(313, "Encuentras un botiquín veterinario de granja repleto de sueros animales.", "Adaptar los sueros animales para uso humano (-10 Salud, +2 Medicinas)", "Guardar componentes para trueque (+1 Provisión)", "Descartar los viales");
                    } else if (subEvento == 4) {
                        decision = new Decision(314, "Un pozo de agua estancada emite un olor químico extraño. Podría ser una fosa común.", "Investigar los restos usando equipo clínico (+15 Puntos)", "Tomar muestras de agua para analizar patógenos (+1 Medicina)", "Sellar el pozo con piedras");
                    } else if (subEvento == 5) {
                        decision = new Decision(321, "Un espantapájaros en los cultivos esconde una mochila de médico militar de campo.", "Revisar la mochila con cautela (+2 Medicinas)", "Desatar la estructura por si hay trampas mecánicas (+1 Munición)", "Alejarse del campo abierto");
                    } else if (subEvento == 6) {
                        decision = new Decision(322, "Una cabaña de aperos cerrada guarda estanterías de productos químicos agrícolas.", "Identificar sales minerales útiles para sales de rehidratación (+1 Medicinas, +1 Provisión)", "Ventilar el cuarto tóxico arriesgando mareos (-5 Salud)", "Cerrar la puerta");
                    } else if (subEvento == 7) {
                        decision = new Decision(323, "Un caballo infectado pero dócil se acerca cojeando a la granja.", "Practicar eutanasia compasiva y extraer material biológico (+1 Medicina)", "Intentar curar su herida expuesta (-1 Medicina)", "Espantarlo para evitar peligros");
                    } else if (subEvento == 8) {
                        decision = new Decision(324, "Un silo de almacenamiento de grano tiene restos de una antigua clínica de evacuación.", "Rebuscar en los escritorios derruidos (+1 Medicina)", "Tomar vendas de algodón industrializadas (+1 Provisiones)", "Marcharse");
                    } else {
                        decision = new Decision(325, "Encuentras un maletín médico de emergencia enterrado bajo un roble en la granja.", "Desenterrarlo y verificar su contenido (+2 Medicinas)", "Dejarlo donde está por temor a contaminación", "Marcar la posición en el mapa para después");
                    }
                    break;

                case HOSPITAL:
                    if (subEvento == 0) {
                        decision = new Decision(300, "Encuentras la antigua sala de maternidad. Hay una incubadora con una vacuna pediátrica.", "Gastar 1 medicina para purificar el sistema (+2 Medicinas, +25 Puntos)", "Desconectar el sistema para piezas (+1 Munición)", "Ignorar y revisar botiquines");
                    } else if (subEvento == 1) {
                        decision = new Decision(315, "Un quirófano de urgencias mantiene una planta de oxígeno autónoma operativa.", "Recargar cilindros de oxígeno para el refugio (+1 Defensa)", "Extraer reguladores de presión para comerciar (+1 Provisión)", "Desconectar todo");
                    } else if (subEvento == 2) {
                        decision = new Decision(316, "Encuentras el archivo clínico central intacto con historiales de pacientes cero.", "Estudiar registros buscando debilidades del virus (+20 Puntos)", "Prender fuego los expedientes", "Buscar fármacos comunes");
                    } else if (subEvento == 3) {
                        decision = new Decision(326, "La farmacia central del hospital tiene estantes caídos pero un sector blindado.", "Forzar la reja con barras de metal (-10 Salud, +2 Medicinas)", "Buscar conductos de ventilación para colarse (+1 Medicina)", "Rendirse y buscar fuera");
                    } else if (subEvento == 4) {
                        decision = new Decision(327, "Un desfibrilador de emergencia guarda condensadores de alta potencia.", "Extraer piezas eléctricas de valor técnico (+1 Munición)", "Probar si la batería interna funciona para reanimar sistemas", "Dejarlo intacto");
                    } else if (subEvento == 5) {
                        decision = new Decision(328, "Encuentras a un enfermero refugiado en coma inducido en la sala de aislamiento.", "Revisar su suero intravenoso descubriendo dosis puras (+1 Medicina)", "Desconectar el suero para terminar su sufrimiento éticamente", "Dejarlo descansar");
                    } else if (subEvento == 6) {
                        decision = new Decision(329, "Un camión de suministros médicos de la Cruz Roja volcó en el estacionamiento.", "Cortar la lona blindada para sustraer cajas selladas (+3 Medicinas)", "Revisar la cabina del conductor (-5 Salud)", "Ignorar el camión");
                    } else if (subEvento == 7) {
                        decision = new Decision(330, "El laboratorio toxicológico del hospital tiene campanas extractoras activas.", "Sintetizar inhibidores antivirales caseros (+2 Medicinas)", "Limpiar el instrumental de precisión (+15 Puntos)", "Apagar las llaves de gas");
                    } else if (subEvento == 8) {
                        decision = new Decision(331, "Una morgue improvisada en el sótano del hospital guarda efectos personales.", "Revisar bolsillos de camillas buscando llaves de botiquines (+1 Medicina)", "Dedicar unos minutos de silencio respetuoso (+10 Salud)", "Marcharse asustado");
                    } else {
                        decision = new Decision(100, "Misión Humanitaria: Un campamento cercano sufre un brote epidémico letal.", "Enviar 2 Medicinas para salvarlos (Misión cumplida, +40 Puntos)", "Enviar solo una dosis parcial", "Rechazar la misión");
                    }
                    break;
                    
                case COMISARIA:
                    if (subEvento == 0) {
                        decision = new Decision(301, "En el sector de detención médica hay un recluso con notas sobre antídotos.", "Examinar los apuntes con detenimiento (+15 Puntos)", "Registrar su celda en busca de analgésicos (+1 Medicina)", "Retirarte");
                    } else if (subEvento == 1) {
                        decision = new Decision(317, "El botiquín de primeros auxilios tácticos de la policía está intacto pero con clave.", "Tantear la combinación analizando patrones numéricos (+2 Medicinas)", "Forzar el maletín con un bisturí (-5 Salud, +1 Medicina)", "Abandonarlo");
                    } else if (subEvento == 2) {
                        decision = new Decision(332, "En la oficina del médico forense policial hay viales de suero antitetánico.", "Recuperar los viales de la morgue policial (+2 Medicinas)", "Investigar bitácoras de autopsias de infectados (+15 Puntos)", "Cerrar con llave");
                    } else if (subEvento == 3) {
                        decision = new Decision(333, "Un estante antidisturbios guarda máscaras de gas y filtros purificadores.", "Equipar filtros nuevos para tu refugio (+1 Defensa)", "Desarmar carbón activado para purificar agua (+1 Provisión)", "Ignorar el equipo");
                    } else if (subEvento == 4) {
                        decision = new Decision(334, "Una celda de aislamiento alberga a un paramédico militar delirante.", "Calmarlo con palabras y administrarle sedantes (+1 Medicina, +15 Puntos)", "Interrogarlo sobre depósitos militares", "Ignorar sus gritos");
                    } else if (subEvento == 5) {
                        decision = new Decision(335, "Encuentras manuales de medicina táctica de combate en el escritorio del oficial jefe.", "Estudiar técnicas de torniquete avanzado (+15 Puntos)", "Utilizar las hojas de papel como yesca para fogatas (+1 Provisiones)", "Dejarlos allí");
                    } else if (subEvento == 6) {
                        decision = new Decision(336, "Un botiquín de patrulla policial patrullera quedó abierto en un coche patrulla.", "Recoger gasas y desinfectantes de guantera (+1 Medicina)", "Sustraer linterna táctica de alta potencia", "Seguir de largo");
                    } else if (subEvento == 7) {
                        decision = new Decision(337, "La armería de la comisaría tiene una sección de gas lacrimógeno y kits médicos de evacuación.", "Sustraer kits de emergencia médica militar (+2 Medicinas)", "Ignorar los químicos", "Forzar armamento");
                    } else if (subEvento == 8) {
                        decision = new Decision(338, "Un cartel de primeros auxilios en la entrada esconde una llave maestra de farmacias.", "Tomar la llave oculta tras el cuadro (+1 Medicina)", "Descolgar el cuadro decorativo", "No tocar nada");
                    } else {
                        decision = new Decision(339, "Revisando casilleros de agentes encuentras botiquines personales de bolsillo.", "Recolectar analgésicos personales (+1 Medicina)", "Examinar efectos personales", "Cerrar casilleros");
                    }
                    break;

                case BOSQUE:
                    if (subEvento == 0) {
                        decision = new Decision(302, "Identificas plantas medicinales raras que solo un médico entrenado reconoce.", "Recolectar y preparar cataplasmas naturales (+1 Medicina, +5 Salud)", "Extraer compuestos químicos avanzados (-5 Salud, +2 Medicinas)", "Caminar rápido");
                    } else if (subEvento == 1) {
                        decision = new Decision(318, "Una clínica de campaña militar abandonada bajo los árboles fue arrasada.", "Rebuscar buscando sueros de combate (+1 Medicina, +1 Munición)", "Montar un vivac temporal de descanso (+10 Salud)", "Continuar la marcha");
                    } else if (subEvento == 2) {
                        decision = new Decision(340, "Un árbol de corteza milenaria es usado tradicionalmente para infusiones febrífugas.", "Cortar corteza curativa para preparar té medicinal (+1 Medicina)", "Descansar bajo su sombra (+5 Salud)", "Continuar");
                    } else if (subEvento == 3) {
                        decision = new Decision(341, "Un arroyo limpio en el bosque boscoso desciende de vertientes puras.", "Recolectar agua pura para esterilizar instrumental (+1 Provisión)", "Lavar tus heridas abiertas con agua fría (+10 Salud)", "Seguir el cauce");
                    } else if (subEvento == 4) {
                        decision = new Decision(342, "Cazadores furtivos dejaron una mochila con suministros de emergencia y analgésicos.", "Revisar el contenido de la mochila abandonada (+1 Medicina, +1 Provisiones)", "Armar una contra-trampa táctica", "Alejarse");
                    } else if (subEvento == 5) {
                        decision = new Decision(343, "Un panal de abejas silvestres en un tronco hueco ofrece miel antiséptica.", "Extraer miel cruda con precauciones (-5 Salud, +2 Provisiones)", "Dejar el panal tranquilo", "Usar fuego para ahuyentar abejas");
                    } else if (subEvento == 6) {
                        decision = new Decision(344, "Encuentras hongos silvestres comestibles y otros venenosos.", "Clasificar cuidadosamente seleccionando los comestibles (+1 Provisión)", "Descartar todo por seguridad botánica", "Probar un hongo al azar (-20 Salud)");
                    } else if (subEvento == 7) {
                        decision = new Decision(345, "Un ciervo herido por cazadores yace agonizante en un matorral.", "Aplicar eutanasia humanitaria y aprovechar su carne (+2 Provisiones)", "Intentar curar su pata rota", "Dejarlo en paz");
                    } else if (subEvento == 8) {
                        decision = new Decision(346, "Una tormenta eléctrica se desata en el bosque y buscas refugio en una cueva seca.", "Descansar protegido de la intemperie (+10 Salud)", "Recolectar agua de lluvia purificada", "Avanzar bajo la lluvia");
                    } else {
                        decision = new Decision(347, "Encuentras restos de un campamento científico clandestino abandonado.", "Revisar cuadernos de notas epidemiológicas (+15 Puntos)", "Tomar frascos de reactivos químicos (+1 Medicina)", "Ignorar el sitio");
                    }
                    break;

                case MERCADO:
                    if (subEvento == 0) {
                        decision = new Decision(303, "Un mercader ambulante enfermo te ofrece un trato: un botiquín por atención médica.", "Tratar su infección gastando 1 medicina", "Exigirle provisiones por la fuerza", "Ignorar al mercader");
                    } else if (subEvento == 1) {
                        decision = new Decision(319, "Encuentras una farmacia saqueada; queda una caja fuerte pequeña.", "Usar estetoscopio médico para escuchar resortes (+2 Medicinas)", "Golpear la caja con barra de metal (-10 Salud, +1 Medicina)", "Dejarla intacta");
                    } else if (subEvento == 2) {
                        decision = new Decision(348, "Un viejo dispensador automático de golosinas guarda barritas energéticas intactas.", "Forzar el cristal para extraer provisiones (+2 Provisiones)", "Desarmar monedas para cobre", "Ignorar la máquina");
                    } else if (subEvento == 3) {
                        decision = new Decision(349, "Una tienda de suplementos nutricionales y vitamínicos conserva frascos sellados.", "Rebuscar vitaminas y complementos fortificantes (+1 Medicina, +5 Salud)", "Llevar suplementos energéticos (+1 Provisión)", "Vaciar estantes");
                    } else if (subEvento == 4) {
                        decision = new Decision(350, "Un grupo de civiles hace trueque en la entrada del mercado ofreciendo fármacos por comida.", "Intercambiar 1 provisión por 2 medicinas", "Rechazar el trato por desconfianza", "Aconsejarles sobre higiene clínica (+10 Puntos)");
                    } else if (subEvento == 5) {
                        decision = new Decision(351, "Una sección de perfumería conserva alcohol puro y lociones antisépticas.", "Destilar alcohol base para desinfección (+1 Medicina)", "Llevar aromas para enmascarar olores corporales", "Ignorar la sección");
                    } else if (subEvento == 6) {
                        decision = new Decision(352, "Un carrito de emergencias médicas de centro comercial está volcado en el pasillo.", "Recoger vendas elásticas y parches de gasa (+1 Medicina)", "Revisar si hay analgésicos de venta libre", "Continuar");
                    } else if (subEvento == 7) {
                        decision = new Decision(353, "Una farmacia comunitaria muestra señales de haber sido sellada por profesionales.", "Romper el sello sanitario para buscar reservas ocultas (+2 Medicinas)", "Respetar el sello y buscar en pasillos externos", "Marcar zona");
                    } else if (subEvento == 8) {
                        decision = new Decision(354, "Encuentras un manual de primeros auxilios corporativos en un mostrador.", "Leer técnicas de reanimación rápida (+10 Puntos)", "Usar papel para notas", "Dejarlo");
                    } else {
                        decision = new Decision(355, "Un estante de productos de higiene femenina e infantil conserva algodón estéril.", "Recolectar algodón industrial para vendajes quirúrgicos (+1 Medicina)", "Tomar pañales para trueque", "Ignorar");
                    }
                    break;

                case SOTANO:
                    if (subEvento == 0) {
                        decision = new Decision(305, "Organizas tu banco de pruebas en el sótano para fabricar antisépticos caseros.", "Destilar alcohol clínico (+2 Medicinas)", "Descansar y repasar manuales de anatomía (+10 Salud)", "Dejar el laboratorio ordenado");
                    } else if (subEvento == 1) {
                        decision = new Decision(320, "Revisando cajas en el sótano encuentras un microscopio óptico portátil.", "Calibrar para estudiar muestras biológicas (+20 Puntos)", "Vender las lentes de aumento (+1 Provisión)", "Guardarlo");
                    } else if (subEvento == 2) {
                        decision = new Decision(356, "Un botiquín familiar antiguo cuelga de la pared del sótano con remedios vencidos.", "Revisar qué fármacos aún son químicamente estables (+1 Medicina)", "Descartar todo lo vencido", "Dejarlo colgado");
                    } else if (subEvento == 3) {
                        decision = new Decision(357, "Diseñas un sistema de filtración de agua por goteo con arena y carbón en el sótano.", "Instalar el filtro purificador mejorando tus reservas (+1 Provisión)", "Optimizar el flujo de agua limpia (+5 Salud)", "Revisar tuberías");
                    } else if (subEvento == 4) {
                        decision = new Decision(358, "Un viejo colchón médico de ambulancia fue guardado en el sótano.", "Mejorar tu cama para descansar con mayor comodidad (+15 Salud)", "Cortar la lona impermeable para aislar humedad", "Dejarlo");
                    } else if (subEvento == 5) {
                        decision = new Decision(359, "Revisando libros de medicina heredados encuentras esquemas de acupuntura y relajación.", "Practicar técnicas de reducción de estrés y fatiga (+10 Salud)", "Usar papel para encender fuego", "Guardar libros");
                    } else if (subEvento == 6) {
                        decision = new Decision(360, "Una caja de herramientas de carpintería en el sótano incluye bisturíes y pinzas finas.", "Adaptar herramientas metálicas para uso quirúrgico de emergencia (+1 Medicina)", "Usar herramientas para fortificar puertas (+1 Defensa)", "Guardar");
                    } else if (subEvento == 7) {
                        decision = new Decision(361, "Un frasco de sales de baño aromáticas conservadas puede servir para baños relajantes.", "Usar sales para aliviar dolores musculares tras las caminatas (+10 Salud)", "Cambiar por comida", "Ignorar");
                    } else if (subEvento == 8) {
                        decision = new Decision(362, "Instalas un cartel de aislamiento estricto en la entrada de tu sótano clínico.", "Reforzar protocolos de bioseguridad en el refugio (+1 Defensa)", "Escribir normas de higiene", "Borrar cartel");
                    } else {
                        decision = new Decision(363, "Una pequeña ventana alta del sótano permite ver el exterior sin ser visto.", "Vigilar la calle tomando notas de movimientos hostiles (+10 Puntos)", "Tapar la ventana con cartón", "Dejar entrar luz");
                    }
                    break;

                default:
                    // DESCANSAR, USAR_MEDICINA, FORTIFICAR y CASA: eventos propios para cada acción
                    decision = eventoMedicoGeneral(accion, subEvento % 5);
                    break;
            }

        } else {
            // =========================================================================
            // 10 EVENTOS TÁCTICOS Y DE COMBATE EXCLUSIVOS DEL SOBREVIVIENTE
            // =========================================================================
            switch (accion) {
                case GRANJA:
                    if (subEvento == 0) {
                        decision = new Decision(410, "Un grupo de saqueadores armados ha montado barricadas en la casa principal de la granja.", "Atacarlos por sorpresa desde los maizales (-1 Munición, +3 Provisiones)", "Infiltrarte sigilosamente por la noche (+1 Provisión)", "Retirarte sin hacer ruido");
                    } else if (subEvento == 1) {
                        decision = new Decision(411, "Un tractor abandonado bloquea el camino; aún le queda combustible.", "Sifonar el estanque para extraer combustible útil (+2 Provisiones)", "Desarmar la batería para piezas eléctricas (+1 Munición)", "Dejar el tractor en paz");
                    } else if (subEvento == 2) {
                        decision = new Decision(412, "Una jauría de perros salvajes infectados rodea el silo de grano.", "Combatirlos cuerpo a cuerpo con machete (-10 Salud, +2 Provisiones)", "Usar munición para abatir líderes (-1 Munición)", "Trepar a lo alto del silo");
                    } else if (subEvento == 3) {
                        decision = new Decision(413, "Encuentras un arsenal clandestino bajo el suelo de un establo.", "Forzar el cerrojo de madera para saquear munición (+3 Munición)", "Establecer una trampa explosiva casera en la entrada (+1 Defensa)", "Ignorar el escondite");
                    } else if (subEvento == 4) {
                        decision = new Decision(414, "Un helicóptero militar derribado y calcinado descansa en los cultivos.", "Rebuscar entre los restos humeantes buscando armamento (+2 Munición)", "Desmontar blindaje metálico para tu refugio (+1 Defensa)", "Alejarte por precaución");
                    } else if (subEvento == 5) {
                        decision = new Decision(420, "Un espantapájaros vestido con uniforme militar cuelga en los maizales.", "Registrar sus bolsillos tácticos buscando cargadores (+2 Munición)", "Desmontar su casco de acero táctico (+1 Defensa)", "Ignorarlo");
                    } else if (subEvento == 6) {
                        decision = new Decision(421, "Un gran cobertizo de herramientas agrícolas conserva bidones de aceite y poleas.", "Robar poleas y cables de acero para fortificaciones (+1 Defensa)", "Llevar herramientas pesadas para defensa cuerpo a cuerpo", "Inspeccionar y marchar");
                    } else if (subEvento == 7) {
                        decision = new Decision(422, "Una torre de alta tensión cercana a la granja tiene una escalera de servicio accesible.", "Trepar para vigilar el horizonte detectando amenazas lejanas (+15 Puntos)", "Desmontar cables de cobre para trueque (+1 Provisión)", "No arriesgarse a subir");
                    } else if (subEvento == 8) {
                        decision = new Decision(423, "Un pozo ciego en desuso en la granja oculta una bolsa militar impermeable.", "Bajar con una cuerda para rescatar el botín (+2 Provisiones, +1 Munición)", "Ignorar el pozo maloliente", "Lanzar piedras");
                    } else {
                        decision = new Decision(424, "Un grupo de cuervos picotea restos humanos junto a una valla rota.", "Espantar a las aves y revisar si portaban objetos de valor (+1 Munición)", "Rodear el sitio en silencio", "Disparar a las aves (-1 Munición)");
                    }
                    break;

                case COMISARIA:
                    if (subEvento == 0) {
                        decision = new Decision(400, "Encuentras la armería principal bloqueada con puerta electrónica.", "Forzar el panel eléctrico con riesgo de cortocircuito", "Utilizar explosivos caseros para reventar bisagras (+3 Munición)", "Asegurar el perímetro");
                    } else if (subEvento == 1) {
                        decision = new Decision(415, "El despacho del comisario está intacto pero la puerta tiene barra anti-pánico.", "Derribar la puerta a patadas (-10 Salud, +2 Munición)", "Escabullirte por conductos de ventilación", "Descartar la oficina");
                    } else if (subEvento == 2) {
                        decision = new Decision(425, "El polígono de tiro subterráneo de la comisaría está oscuro pero intacto.", "Revisar blancos móviles buscando cartuchos caídos (+2 Munición)", "Practicar puntería a oscuras (+15 Puntos)", "Evitar el subterráneo");
                    } else if (subEvento == 3) {
                        decision = new Decision(426, "Una patrulla antidisturbios abandonada en el garaje tiene escudos acrílicos.", "Desmontar escudos antidisturbios para barricadas del refugio (+1 Defensa)", "Sustraer baliza luminosa de emergencia", "Revisar maletero");
                    } else if (subEvento == 4) {
                        decision = new Decision(427, "El depósito de evidencias incautadas guarda contrabando decomisado.", "Forzar la puerta del depósito de pruebas (+2 Provisiones, +1 Munición)", "Buscar armas blancas artesanales", "Ignorar las leyes del viejo mundo");
                    } else if (subEvento == 5) {
                        decision = new Decision(428, "Una terminal de computadoras policiales parpadea con energía de emergencia.", "Intentar extraer rutas de patrullaje militar seguras (+15 Puntos)", "Desarmar componentes de pantalla", "Apagar sistema");
                    } else if (subEvento == 6) {
                        decision = new Decision(429, "El comedor de la policía municipal guarda raciones secas de larga duración.", "Saquear la despensa del comedor policial (+3 Provisiones)", "Verificar si hay bebidas energéticas", "Salir de la cocina");
                    } else if (subEvento == 7) {
                        decision = new Decision(430, "Un oficial zombi esposado a una barandilla lleva una pistola reglamentaria visible.", "Acercarse con cuidado para arrebatarle el arma (-5 Salud, +2 Munición)", "Dispararle a distancia (-1 Munición, +1 Munición)", "Ignorarlo");
                    } else if (subEvento == 8) {
                        decision = new Decision(431, "El tejado de la comisaría permite observar barricadas vecinas.", "Instalar puesto de vigilancia táctica (+1 Defensa)", "Bajar rápidamente", "Señalizar con linterna");
                    } else {
                        decision = new Decision(432, "Una caja fuerte empotrada en la sala de finanzas policiales.", "Reventar la cerradura con carga explosiva casera (+3 Munición, +2 Provisiones)", "Intentar combinación aleatoria", "Dejarla");
                    }
                    break;

                case HOSPITAL:
                    if (subEvento == 0) {
                        decision = new Decision(401, "Un grupo de saqueadores se ha atrincherado en la farmacia del hospital.", "Atacarlos por sorpresa usando tu munición (-1 Munición, +2 Provisiones)", "Esperar sigilosamente en las sombras", "Retirarte del hospital");
                    } else if (subEvento == 1) {
                        decision = new Decision(416, "En la azotea del hospital hay un campamento de francotiradores abandonado.", "Asegurar la posición elevada ganando ventaja táctica (+1 Munición, +1 Provisiones)", "Desmantelar apostadero", "Bajar inmediatamente");
                    } else if (subEvento == 2) {
                        decision = new Decision(433, "El sótano de caldera del hospital tiene bidones de combustible diésel.", "Trasladar combustible al refugio (-5 Salud, +3 Provisiones)", "Sifonar una pequeña cantidad", "Dejarlo ahí");
                    } else if (subEvento == 3) {
                        decision = new Decision(434, "Una ambulancia blindada del S.W.A.T. está estacionada en la rampa de urgencias.", "Forzar el panel de armas de la ambulancia (+2 Munición)", "Revisar equipo de radio", "Descartar");
                    } else if (subEvento == 4) {
                        decision = new Decision(435, "Encuentras pasillos colapsados por escombros donde se refugian infectados dormidos.", "Atravesar silenciosamente conteniendo la respiración (+10 Puntos)", "Lanzar una piedra para distraerlos y correr", "Dar media vuelta");
                    } else if (subEvento == 5) {
                        decision = new Decision(436, "La oficina de administración del hospital tiene mapas detallados de la ciudad.", "Estudiar rutas de escape y zonas de saqueo (+15 Puntos)", "Romper muebles para hacer leña", "Ignorar");
                    } else if (subEvento == 6) {
                        decision = new Decision(437, "Un carrito de limpieza industrial guarda cloro y amoníaco concentrado.", "Fabricar agentes químicos asfixiantes defensivos (+1 Defensa)", "Usar baldes para transportar agua", "Ignorar");
                    } else if (subEvento == 7) {
                        decision = new Decision(438, "Encuentras un cadáver de un mercenario equipado con chaleco táctico intacto.", "Despojarle el chaleco antibalas (+1 Defensa)", "Quitarle su cuchillo de combate", "Respetar al caído");
                    } else if (subEvento == 8) {
                        decision = new Decision(439, "El sistema de seguridad del hospital parpadea con cámaras operativas en un monitor.", "Revisar cámaras para detectar hordas cercanas (+10 Puntos)", "Destruir pantallas de un golpe", "Apagar monitores");
                    } else {
                        decision = new Decision(440, "Una sala de descanso del personal médico guarda latas de café y raciones.", "Apropiarse de las provisiones ocultas en lockers (+2 Provisiones)", "Descansar en los sillones (+5 Salud)", "Salir");
                    }
                    break;

                case BOSQUE:
                    if (subEvento == 0) {
                        decision = new Decision(402, "Descubres huellas frescas de una patrulla enemiga que camina hacia un refugio.", "Seguir su rastro a distancia para emboscarlos", "Instalar trampas con estacas de madera (+1 Defensa)", "Ocultarte en la espesura");
                    } else if (subEvento == 1) {
                        decision = new Decision(417, "Un oso infectado de gran tamaño bloquea el sendero principal.", "Enfrentarlo cara a cara disparando (-1 Munición, +3 Provisiones)", "Trepar a un árbol y esperar (-1 Provisión)", "Rodear el sector");
                    } else if (subEvento == 2) {
                        decision = new Decision(441, "Una torre forestal de vigilancia contra incendios se alza entre los árboles.", "Subir a la torre para establecer puesto de tiro y observación (+1 Munición, +1 Defensa)", "Descansar en la cabina elevada (+10 Salud)", "Ignorar la torre");
                    } else if (subEvento == 3) {
                        decision = new Decision(442, "Encuentras una motocicleta todoterreno abandonada con medio estanque.", "Extraer gasolina y componentes mecánicos (+2 Provisiones, +1 Munición)", "Intentar repararla para huir rápido", "Dejarla");
                    } else if (subEvento == 4) {
                        decision = new Decision(443, "Un campamento scout infantil abandonado conserva carpas y latas de sopa.", "Inspeccionar las carpas en busca de comida enlatada (+2 Provisiones)", "Desarmar estacas de carpa para trampas (+1 Defensa)", "Seguir");
                    } else if (subEvento == 5) {
                        decision = new Decision(444, "Una quebrada rocosa esconde una cueva profunda y oscura.", "Explorar la cueva con linterna buscando refugio o minerales (+1 Munición)", "Evitar la cueva por peligro de derrumbe", "Acampar en la entrada");
                    } else if (subEvento == 6) {
                        decision = new Decision(445, "Encuentras trampas de oso colocadas furtivamente en el suelo del bosque.", "Desarmar las trampas para reutilizar su acero en tu refugio (+1 Defensa)", "Marcarlas para evitar accidentes", "Alejarse corriendo");
                    } else if (subEvento == 7) {
                        decision = new Decision(446, "Un grupo de jabalíes salvajes busca raíces en el lodo.", "Cazar un ejemplar utilizando sigilo y munición (-1 Munición, +3 Provisiones)", "Espantarlos con ruido", "Observar sin intervenir");
                    } else if (subEvento == 8) {
                        decision = new Decision(447, "Un puente colgante de madera cruza un abismo profundo en el bosque.", "Cruzar con extremo cuidado equilibrando tu carga (+15 Puntos)", "Buscar un cruce alternativo por el río", "Dar la vuelta");
                    } else {
                        decision = new Decision(448, "Restos de un automóvil civil volcado en un sendero forestal.", "Revisar la guantera y el baúl buscando herramientas (+1 Munición)", "Sacar baterías", "Ignorar");
                    }
                    break;

                case MERCADO:
                    if (subEvento == 0) {
                        decision = new Decision(403, "Un enorme estante metálico lleno de conservas enlatadas está a punto de colapsar.", "Arriesgarte a sostener el peso para rescatar el botín (+3 Provisiones)", "Golpear el estante para que caiga rápido (-10 Salud, +1 Provisión)", "Ignorar el estante");
                    } else if (subEvento == 1) {
                        decision = new Decision(418, "Encuentras la caja registradora central intacta pero con cerradura pesada.", "Reventar la caja con una palanca metálica (+2 Provisiones)", "Llevarte la registradora a cuestas (+1 Provisión)", "Ignorarla");
                    } else if (subEvento == 2) {
                        decision = new Decision(449, "Un almacén mayorista de bebidas conserva agua embotellada y refrescos.", "Cargar tantas botellas como puedas en tu mochila (+3 Provisiones)", "Destruir envases vacíos", "Continuar");
                    } else if (subEvento == 3) {
                        decision = new Decision(450, "Una ferretería dentro del centro comercial tiene herramientas y candados intactos.", "Robar herramientas pesadas y cadenas para fortificar (+1 Defensa)", "Sustraer clavos y martillos", "Ignorar ferretería");
                    } else if (subEvento == 4) {
                        decision = new Decision(451, "Saqueadores ebrios celebran con una fogata en el patio de comidas del mercado.", "Atacar su campamento aprovechando su descuido (-1 Munición, +3 Provisiones)", "Rodearlos sigilosamente", "Retirarse");
                    } else if (subEvento == 5) {
                        decision = new Decision(452, "Una tienda de ropa de invierno conserva abrigos gruesos y mantas térmicas.", "Llevar mantas para soportar las noches frías del refugio (+10 Salud)", "Cortar telas para vendas", "Ignorar tienda");
                    } else if (subEvento == 6) {
                        decision = new Decision(453, "Un camión de reparto de panificados está estacionado en la puerta trasera.", "Revisar los compartimentos buscando pan y embutidos sellados (+2 Provisiones)", "Sustraer baterías del camión", "Ignorar");
                    } else if (subEvento == 7) {
                        decision = new Decision(454, "Encuentras un maletín de herramientas de cerrajería olvidado en un mostrador.", "Apropiarte del kit de ganzúas para abrir puertas bloqueadas (+1 Punto)", "Dejarlo", "Examinarlo sin llevártelo");
                    } else if (subEvento == 8) {
                        decision = new Decision(455, "Una sección de electrónica conserva linternas de dinamo y baterías solares.", "Equiparte con una linterna solar de carga manual (+1 Defensa)", "Desarmar aparatos", "Pasar de largo");
                    } else {
                        decision = new Decision(456, "Un cartel luminoso publicitario gigante cruza peligrosamente la entrada del mercado.", "Desconectar los cables de alta tensión con precaución (+1 Munición)", "Rodear por los escombros", "Ignorar");
                    }
                    break;

                case SOTANO:
                    if (subEvento == 0) {
                        decision = new Decision(405, "El generador auxiliar del refugio falla y el sótano se queda en absoluta oscuridad.", "Reparar el motor a tientas arriesgando descarga (-10 Salud, +1 Defensa)", "Fabricar antorchas improvisadas", "Dejar el sótano a oscuras");
                    } else if (subEvento == 1) {
                        decision = new Decision(419, "Notas humedad excesiva y filtraciones amenazan con derrumbar una pared.", "Apuntalar el muro con maderas de refuerzo (+1 Defensa)", "Despejar canales de desagüe a mano (-5 Salud)", "Ignorar grieta");
                    } else if (subEvento == 2) {
                        decision = new Decision(457, "Un banco de trabajo en el sótano permite afilar armas blancas y herramientas.", "Afilar machetes y cuchillos mejorando tu capacidad de defensa (+1 Defensa)", "Desordenar banco", "Dejar herramientas");
                    } else if (subEvento == 3) {
                        decision = new Decision(458, "Instalas un sistema de poleas y contrapesos para bloquear la escotilla del sótano.", "Optimizar el cierre hermético de seguridad (+1 Defensa)", "Desarmar poleas", "Probar apertura");
                    } else if (subEvento == 4) {
                        decision = new Decision(459, "Revisando cajas antiguas encuentras pólvora y componentes de pirotecnia.", "Fabricar una mina trampa defensiva casera (+1 Defensa, +1 Munición)", "Descartar por seguridad", "Guardar pólvora");
                    } else if (subEvento == 5) {
                        decision = new Decision(460, "Un rincón oscuro del sótano acumula cartón y papeles viejos que atraen humedad.", "Limpiar y ordenar todo el sector ganando espacio higiénico (+10 Salud)", "Prender fuego controlado", "Ignorar suciedad");
                    } else if (subEvento == 6) {
                        decision = new Decision(461, "Instalas ganchos de acero reforzado en el techo para colgar provisiones y alejarlas de ratas.", "Colgar raciones de comida de forma segura (+1 Provisión)", "Colgar herramientas", "No hacer nada");
                    } else if (subEvento == 7) {
                        decision = new Decision(462, "Una tubería de desagüe secundaria del sótano se tapó con escombros.", "Desatascar la tubería evitando inundaciones internas (-5 Salud)", "Sellar el tubo", "Dejarlo");
                    } else if (subEvento == 8) {
                        decision = new Decision(463, "Construyes un pequeño rincón de descanso con colchones y sacos de dormir.", "Mejorar la calidad de tu sueño nocturno recuperando salud (+15 Salud)", "Dejar espacio vacío", "Desarmar camas");
                    } else {
                        decision = new Decision(464, "Revisas los cimientos de concreto buscando marcas de desgaste estructural.", "Calafatear grietas con mezcla de cemento y arena (+1 Defensa)", "Ignorar cimientos", "Marcar fisuras");
                    }
                    break;

                default:
                    // DESCANSAR, USAR_MEDICINA, FORTIFICAR y CASA: eventos propios para cada acción
                    decision = eventoSobrevivienteGeneral(accion, subEvento % 5);
                    break;
            }
        }
    }

    // =========================================================================
    // EVENTOS PARA DESCANSAR, USAR MEDICINA, FORTIFICAR Y CASA (5 por acción y rol)
    // Antes estas acciones no tenían casos y siempre caían en el mismo evento.
    // =========================================================================

    private Decision eventoMedicoGeneral(Accion accion, int n) {
        switch (accion) {
            case DESCANSAR:
                switch (n) {
                    case 0:  return new Decision(500, "Mientras descansas oyes toses y pasos afuera: un herido pide ayuda a la puerta.", "Atenderlo con lo que tengas a mano (-1 Medicina, +20 Puntos)", "Pasarle una venda por la rendija (+5 Puntos)", "Ignorar los pedidos");
                    case 1:  return new Decision(501, "Un sueño inquieto te hace recordar la fórmula de un viejo antiséptico.", "Anotarla y prepararla con lo que hay (+1 Medicina)", "Seguir durmiendo (+5 Salud)", "Olvidar el asunto");
                    case 2:  return new Decision(502, "Al revisar tu botiquín durante el descanso notas frascos mal rotulados.", "Reetiquetar y reorganizar el instrumental (+10 Puntos)", "Mezclar sobrantes en una solución nueva (-5 Salud, +1 Medicina)", "Dejarlo como está");
                    case 3:  return new Decision(503, "Una gotera cae sobre tus apuntes médicos mientras descansas.", "Salvar los apuntes y estudiarlos (+15 Puntos)", "Mover tu catre y seguir durmiendo (+5 Salud)", "Ignorar la gotera");
                    default: return new Decision(504, "Un gato callejero entra al refugio con una herida infectada.", "Curarlo con tus conocimientos (-1 Medicina, +15 Puntos)", "Compartir tu ración con él (-1 Provisión, +10 Salud)", "Echarlo con cuidado");
                }
            case USAR_MEDICINA:
                switch (n) {
                    case 0:  return new Decision(510, "Al preparar la dosis notas que podrías estirar la medicina con agua destilada.", "Diluir con cuidado y fraccionar (+1 Medicina, -5 Salud)", "Usar la dosis completa (+10 Salud)", "Guardar el frasco");
                    case 1:  return new Decision(511, "Durante la cura se te ocurre un método mejor para esterilizar las agujas.", "Probar el método nuevo (+15 Puntos)", "Hervir todo como siempre (+5 Salud)", "Dejarlo para otro día");
                    case 2:  return new Decision(512, "Desde la calle, un vecino dice estar enfermo y llama a gritos.", "Ir a revisarlo con tu maletín (-1 Medicina, +25 Puntos)", "Hablarle desde lejos dándole consejos (+10 Puntos)", "No responder");
                    case 3:  return new Decision(102, "Misión Científica: Sintetizar un suero purificador avanzado utilizando el entorno.", "Trabajar toda la noche en el laboratorio (-1 Provisión, +3 Medicinas)", "Hacer una versión rápida (+1 Medicina)", "Abandonar la investigación");
                    default: return new Decision(513, "Encuentras notas tuyas de un caso parecido al tuyo en el fondo del maletín.", "Aplicar el tratamiento anotado (+10 Salud, +10 Puntos)", "Archivar las notas (+5 Puntos)", "Guardar el maletín");
                }
            case FORTIFICAR:
                switch (n) {
                    case 0:  return new Decision(520, "Al clavar tablones descubres un doble fondo en la pared con instrumental médico.", "Abrirlo con cuidado (+1 Medicina)", "Sellarlo de nuevo (+1 Defensa)", "Ignorarlo");
                    case 1:  return new Decision(521, "Improvisas una esclusa de bioseguridad con plásticos en la entrada.", "Terminar la esclusa (+1 Defensa, +10 Puntos)", "Usar el plástico para hacer vendas (+1 Medicina)", "Dejar el plástico");
                    case 2:  return new Decision(522, "Tu martillo golpea tu dedo y el tablón se parte en dos.", "Seguir trabajando con la mano herida (-5 Salud, +1 Defensa)", "Vendarte la mano y descansar un rato (+5 Salud)", "Hacer una pausa");
                    case 3:  return new Decision(523, "Un vecino ofrece clavos y alambre a cambio de una consulta médica.", "Hacerle la consulta (+1 Defensa, +10 Puntos)", "Pedirle comida en lugar de clavos (+1 Provisión)", "Rechazar su trato");
                    default: return new Decision(524, "Una tabla del piso suelta esconde una cajita metálica.", "Abrirla (+1 Medicina, +5 Puntos)", "Usar la tabla para reforzar la puerta (+1 Defensa)", "Dejarla");
                }
            default: // CASA
                switch (n) {
                    case 0:  return new Decision(530, "En un consultorio doméstico abandonado quedan cajones con muestras médicas.", "Revisar los cajones (+1 Medicina)", "Llevar los libros de consulta (+10 Puntos)", "Salir");
                    case 1:  return new Decision(531, "Un anciano enfermo vive escondido en el dormitorio de la casa.", "Tratarlo (-1 Medicina, +2 Provisiones, +15 Puntos)", "Dejarle una nota con consejos (+5 Puntos)", "Retirarte");
                    case 2:  return new Decision(532, "La cocina huele a lejía y conservas: alguien las almacenó con método.", "Llevar las conservas (+2 Provisiones)", "Quedarte la lejía para desinfectar (+1 Medicina)", "Cerrar la puerta");
                    case 3:  return new Decision(533, "En el baño hay un botiquín colgado, pero el espejo está roto y hay vidrios por todos lados.", "Recoger con cuidado (+1 Medicina)", "Apurarte a tomarlo todo (-5 Salud, +2 Medicinas)", "Pasar de largo");
                    default: return new Decision(534, "El diario de una enfermera describe un escondite de suministros en el ático.", "Subir al ático (+1 Medicina, +1 Provisión)", "Copiar el diario (+10 Puntos)", "Dejar el diario");
                }
        }
    }

    private Decision eventoSobrevivienteGeneral(Accion accion, int n) {
        switch (accion) {
            case DESCANSAR:
                switch (n) {
                    case 0:  return new Decision(600, "Un ruido en la puerta interrumpe tu descanso: algo rasca la madera.", "Salir a enfrentarlo (-1 Munición, +15 Puntos)", "Esperar en silencio", "Barricar la puerta (+1 Defensa)");
                    case 1:  return new Decision(601, "Mientras dormitas oyes los pasos de una patrulla que pasa cerca.", "Quedarte quieto y dejar que pasen (+5 Salud)", "Espiar por la rendija (+10 Puntos)", "Salir a seguirlos");
                    case 2:  return new Decision(602, "Un sueño agitado te hace recordar un escondite de víveres cerca de aquí.", "Ir a buscarlo (+2 Provisiones)", "Seguir descansando (+10 Salud)", "Olvidarlo");
                    case 3:  return new Decision(603, "Un perro flaco merodea el refugio buscando comida.", "Darle un poco de comida (-1 Provisión, +10 Puntos)", "Espantarlo", "Ignorarlo");
                    default: return new Decision(604, "Te despiertas con calambres en las piernas.", "Estirar y hacer ejercicio suave (+5 Salud)", "Masajearte y dormir otro rato (+5 Salud, +5 Puntos)", "Seguir durmiendo");
                }
            case USAR_MEDICINA:
                switch (n) {
                    case 0:  return new Decision(610, "Mientras te curas escuchas disparos lejanos.", "Terminar rápido y salir a mirar (+10 Puntos)", "Apagar la luz y esperar", "Seguir con la cura (+5 Salud)");
                    case 1:  return new Decision(611, "Tu venda está sucia y necesitas improvisar una nueva.", "Cortar tela de una manta (+5 Salud)", "Usar una tira de tu camisa", "Reusar la vieja");
                    case 2:  return new Decision(612, "Un viajero herido golpea tu puerta pidiendo medicina.", "Darle una medicina (-1 Medicina, +25 Puntos)", "Venderle información por comida (+1 Provisión)", "No abrirle");
                    case 3:  return new Decision(200, "Misión Táctica: Un viejo mapa militar señala un depósito de armas abandonado.", "Organizar expedición táctica arriesgada (+3 Munición, -1 Provisión)", "Explorar solo alrededores seguros", "Descartar mapa");
                    default: return new Decision(613, "El olor a antiséptico atrae a un infectado hasta tu refugio.", "Enfrentarlo con tu arma (-1 Munición, +15 Puntos)", "Esperar a que se vaya", "Atraerlo lejos con ruido");
                }
            case FORTIFICAR:
                switch (n) {
                    case 0:  return new Decision(620, "Tus golpes de martillo se oyen demasiado lejos.", "Seguir de todos modos (+1 Defensa)", "Envolver el martillo en trapos (+1 Defensa, +5 Puntos)", "Parar y esperar");
                    case 1:  return new Decision(621, "Encuentras clavos oxidados y chapas en un rincón del refugio.", "Usarlos para reforzar la entrada (+1 Defensa)", "Armar una alarma de latas (+10 Puntos)", "Guardarlos");
                    case 2:  return new Decision(622, "Una tabla del piso se rompe y revela una caja de cartuchos.", "Abrir la caja (+2 Munición)", "Usar la tabla para reforzar la puerta (+1 Defensa)", "Dejarla");
                    case 3:  return new Decision(623, "Una pared se agrieta mientras la refuerzas.", "Apuntalarla con vigas (-5 Salud, +1 Defensa)", "Taparla con muebles", "Ignorar la grieta");
                    default: return new Decision(624, "Un vecino que ronda por la zona ofrece ayuda a cambio de comida.", "Aceptar y compartir una ración (-1 Provisión, +1 Defensa)", "Rechazar con firmeza", "Espantarlo");
                }
            default: // CASA
                switch (n) {
                    case 0:  return new Decision(630, "Una casa tiene la puerta trabada desde dentro y se oyen gemidos.", "Forzar la puerta y pelear (-1 Munición, +2 Provisiones)", "Entrar por la ventana trasera (+1 Provisión)", "Alejarte");
                    case 1:  return new Decision(631, "Encuentras una caja fuerte pequeña detrás de un cuadro.", "Forzarla con una palanca (+2 Munición)", "Intentar adivinar la clave (+10 Puntos)", "Dejarla");
                    case 2:  return new Decision(632, "El garaje de la casa guarda herramientas y una escopeta sin cartuchos.", "Llevar las herramientas (+1 Defensa)", "Llevar la escopeta para trueque (+1 Provisión)", "Ignorar el garaje");
                    case 3:  return new Decision(633, "En la cocina, una alacena oculta latas de conserva.", "Vaciarla (+2 Provisiones)", "Llevar solo lo imprescindible (+1 Provisión)", "Cerrar la alacena");
                    default: return new Decision(634, "Un cuarto infantil tiene mochilas con linternas y chocolates.", "Revisar las mochilas (+1 Provisión, +5 Salud)", "Llevar las linternas (+10 Puntos)", "Salir sin tocar nada");
                }
        }
    }

    public Decision getDecision() {
        return decision;
    }

    
    // RESOLUCIÓN DE DECISIONES: aplica los efectos de la opción elegida
    // (los efectos escritos en el texto, o un giro inventado si no tiene ninguno)
    

    public String resolverDecision(int opcionElegida) {
        log = new StringBuilder();
        if (decision == null) {
            return "";
        }

        if (opcionElegida < 0 || opcionElegida >= decision.reales.length) {
            opcionElegida = decision.reales.length - 1; // opción inválida = la más prudente
        }
        String opcion = decision.reales[opcionElegida]; // texto completo, con todos los efectos

        int dSalud = 0, dProv = 0, dMed = 0, dMun = 0, dDef = 0, dPts = 0;
        String narrativa = null;

        // 1) Efectos escritos entre paréntesis en el texto de la opción
        Matcher m = EFECTO.matcher(opcion);
        while (m.find()) {
            int v = Integer.parseInt(m.group(2));
            if (!m.group(1).equals("+")) v = -v;
            String k = m.group(3).toLowerCase();
            if (k.startsWith("salud"))          dSalud += v;
            else if (k.startsWith("provis"))    dProv  += v;
            else if (k.startsWith("medicin"))   dMed   += v;
            else if (k.startsWith("munici"))    dMun   += v;
            else if (k.startsWith("defensa"))   dDef   += v;
            else if (k.startsWith("punto"))     dPts   += v;
        }

        // 2) Opciones cuyo efecto está escrito en palabras y no entre paréntesis
        switch (decision.tipo) {
            case 100: // Misión humanitaria
                if (opcionElegida == 0) {
                    dMed -= 2;
                } else if (opcionElegida == 1) {
                    dMed -= 1; dPts += 15;
                    narrativa = "Alcanzas a estabilizar a los más graves, pero no a todos.";
                } else {
                    dPts -= 10;
                    narrativa = "Les das la espalda y el remordimiento te pesa.";
                }
                break;
            case 303: // Mercader: "Tratar su infección gastando 1 medicina" (a cambio recibes un botiquín)
                if (opcionElegida == 0) { dMed += 2; dPts += 10; } // -1 gastada, +3 del botiquín
                break;
            case 350: // Trueque: "Intercambiar 1 provisión por 2 medicinas"
                if (opcionElegida == 0) { dProv -= 1; dMed += 2; }
                break;
            default:
                break;
        }

        String limpio = opcion.replaceAll("\\s*\\([^)]*\\)", "").trim();

        // 3) Si la opción no cambia nada, se inventa algo que pase
        boolean sinEfecto = dSalud == 0 && dProv == 0 && dMed == 0 && dMun == 0 && dDef == 0 && dPts == 0;
        if (sinEfecto) {
            Giro g = inventarGiro(PRUDENTE.matcher(limpio).find());
            dSalud = g.salud; dProv = g.prov; dMed = g.med;
            dMun = g.mun; dDef = g.def; dPts = g.pts;
            narrativa = g.texto;
        }

        // 4) Si no alcanzan los recursos para pagar el costo, la opción falla
        if (provisiones + dProv < 0 || medicinas + dMed < 0 || municion + dMun < 0) {
            registrar("No tienes los recursos necesarios para: " + limpio + ". La oportunidad se pierde.",
                    "Intentó \"" + limpio + "\" sin tener los recursos necesarios.");
            decision = null;
            return log.toString();
        }

        // 5) Aplicar los efectos con sus límites
        salud = Math.max(0, Math.min(100, salud + dSalud));
        provisiones += dProv;
        medicinas += dMed;
        municion += dMun;
        defensa = Math.max(0, Math.min(3, defensa + dDef));
        puntuacion = Math.max(0, puntuacion + dPts);

        // 6) Mensaje y diario
        StringBuilder efectos = new StringBuilder();
        agregarEfecto(efectos, dSalud, "Salud");
        agregarEfecto(efectos, dProv, "Provisiones");
        agregarEfecto(efectos, dMed, "Medicinas");
        agregarEfecto(efectos, dMun, "Munición");
        agregarEfecto(efectos, dDef, "Defensa");
        agregarEfecto(efectos, dPts, "Puntos");

        String pantalla = "Elegiste: " + limpio + ".";
        if (narrativa != null) pantalla += "\n" + narrativa;
        if (efectos.length() > 0) pantalla += "\nResultado: " + efectos + ".";

        String resumen = limpio;
        if (narrativa != null) resumen += " — " + narrativa;
        if (efectos.length() > 0) resumen += " (" + efectos + ")";

        registrar(pantalla, resumen);
        decision = null;
        return log.toString();
    }

    /**
     * Inventa un resultado para una opción sin efectos escritos.
     * Las opciones prudentes (ignorar, retirarse...) tienen giros más suaves;
     * las audaces, más grandes en lo bueno y en lo malo.
     * El daño crece con los días y la defensa lo reduce. Nunca se avisa de antemano.
     */
    private Giro inventarGiro(boolean prudente) {
        // Ahora hay un 35% de posibilidades de obtener un resultado positivo.
        // Las opciones negativas provocan daño con mucha más frecuencia.
        boolean bueno = random.nextInt(100) < 35;
        Giro g;

        if (prudente && bueno) {
            switch (random.nextInt(5)) {
                case 0:  g = new Giro("Mientras te alejas, encuentras algo comestible tirado en el camino."); g.prov = 1; break;
                case 1:  g = new Giro("Entre los escombros brilla una caja de cartuchos olvidada."); g.mun = 1; break;
                case 2:  g = new Giro("Un botiquín arrugado asoma bajo unos papeles."); g.med = 1; break;
                case 3:  g = new Giro("Tu cautela te deja observar el lugar y descubrir un atajo seguro."); g.pts = 5; break;
                default: g = new Giro("Aprovechas la calma para recuperar el aliento."); g.salud = 5; break;
            }
        } else if (prudente) {
            // En las opciones prudentes, 4 de cada 5 resultados negativos
            // causan daño. El quinto hace perder provisiones o puntos.
            switch (random.nextInt(5)) {
                case 0:
                    g = new Giro("Un ruido te sobresalta y tropiezas al retirarte.");
                    g.salud = -danio(8); break;
                case 1:
                    g = new Giro("Una rata te muerde la mano mientras te alejas.");
                    g.salud = -danio(5); break;
                case 2:
                    g = new Giro("Pisas mal entre los escombros y te torcés el tobillo.");
                    g.salud = -danio(10); break;
                case 3:
                    g = new Giro("Una rama oculta te corta el brazo al retirarte.");
                    g.salud = -danio(8); break;
                default:
                    if (provisiones > 0) {
                        g = new Giro("En el camino pierdes parte de tus provisiones.");
                        g.prov = -1;
                    } else {
                        g = new Giro("El contratiempo te hace perder tiempo valioso.");
                        g.pts = -5;
                    }
                    break;
            }
        } else if (bueno) {
            switch (random.nextInt(5)) {
                case 0:  g = new Giro("La jugada sale bien y encuentras provisiones extra."); g.prov = 2; break;
                case 1:  g = new Giro("Tu audacia da resultado: hallas munición escondida."); g.mun = 2; break;
                case 2:  g = new Giro("Entre el desorden aparece un botiquín sin abrir."); g.med = 1; g.pts = 10; break;
                case 3:  g = new Giro("Descubres un punto débil y refuerzas el refugio con lo que encuentras."); g.def = 1; break;
                default: g = new Giro("Todo sale mejor de lo esperado."); g.pts = 15; break;
            }
        } else {
            // En las opciones audaces, 4 de cada 5 resultados negativos
            // causan daño, y los golpes son más fuertes.
            switch (random.nextInt(5)) {
                case 0:
                    g = new Giro("Sales mal parado: algo te golpea en la oscuridad.");
                    g.salud = -danio(15); break;
                case 1:
                    g = new Giro("Un infectado te sorprende y debes huir herido.");
                    g.salud = -danio(12); break;
                case 2:
                    g = new Giro("Enfrentas un obstáculo y recibes un golpe fuerte.");
                    g.salud = -danio(18); break;
                case 3:
                    g = new Giro("El intento sale mal y terminas lastimado durante la huida.");
                    g.salud = -danio(10); break;
                default:
                    if (provisiones > 0 && municion > 0) {
                        g = new Giro("En la huida pierdes parte de tu equipo.");
                        g.prov = -1;
                        g.mun = -1;
                    } else if (provisiones > 0) {
                        g = new Giro("En la huida se te cae la mochila y pierdes comida.");
                        g.prov = -1;
                    } else if (municion > 0) {
                        g = new Giro("En la huida pierdes parte de tu munición.");
                        g.mun = -1;
                    } else {
                        g = new Giro("En la huida te llevas un golpe fuerte.");
                        g.salud = -danio(10);
                    }
                    break;
            }
        }
        return g;
    }

    /** Daño inventado: crece cada 5 días y la defensa lo reduce (mínimo 1). */
    private int danio(int base) {
        return Math.max(1, base + ((dias - 1) / 5) * 3 - defensa);
    }

    private void agregarEfecto(StringBuilder sb, int delta, String nombre) {
        if (delta == 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(delta > 0 ? "+" : "").append(delta).append(" ").append(nombre);
    }

    public String getRol()                  { return rol; }
    public int getSalud()                   { return salud; }
    public int getProvisiones()             { return provisiones; }
    public int getMedicinas()               { return medicinas; }
    public int getMunicion()                { return municion; }
    public int getDefensa()                 { return defensa; }
    public int getDias()                    { return dias; }
    public int getPuntuacion()              { return puntuacion + (misionesCumplidas * 20); } 
    public boolean isVivo()                 { return vivo; }
    public List<String> getDiario()         { return diario; }
}