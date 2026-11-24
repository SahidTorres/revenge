package alcaldedigital.cliente;
import alcaldedigital.compartido.modelo.*;
import alcaldedigital.compartido.protocolo.SesionJuego;
import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.estructuras.decisiones.ArbolDecisiones;
import alcaldedigital.cliente.visualizadores.ConsultaEstructuras;
import alcaldedigital.servidor.misiones.Mision5;
import alcaldedigital.servidor.minijuegos.Minijuego;
import alcaldedigital.cliente.campana.Campana;
import alcaldedigital.cliente.campana.Campana.Capitulo;
import alcaldedigital.cliente.mision6.MisionSeisMotor;
import alcaldedigital.cliente.mision6.MisionSeisSketch;
import java.util.*;

/** Control de entrada y navegación; los efectos siempre pasan por la sesión. */
public final class JuegoCliente {
    public enum Pantalla { MENU, ROLES, PARQUE, CIVITAS, EVIDENCIAS, ARBOLES, AYUDA, RESULTADO,
        MISION5_MODO, MISION5_INTRO, MISION5, MISION5_FIN, MINIJUEGOS, MINIJUEGO, MINIJUEGO_FIN,
        HISTORIA, CAPITULO, MISION6, MISION6_PAUSA, EPILOGO }
    public Pantalla pantalla=Pantalla.MENU, anterior=Pantalla.MENU;
    public Pantalla origenArboles=Pantalla.PARQUE;
    public enum Pestana { RADAR, CAMINO }
    public Pestana pestana=Pestana.RADAR;
    public boolean laboratorio, editandoBusqueda, avisos=true;
    public int cursorBusqueda;
    public final ConsultaEstructuras consulta=new ConsultaEstructuras();
    public String aviso="";
    public double segundosAviso;
    public SesionJuego sesion;
    private final SesionJuego.Fabrica fabrica;
    public JuegoCliente(SesionJuego.Fabrica fabrica) { this(fabrica,new Campana()); }
    public JuegoCliente(SesionJuego.Fabrica fabrica,Campana campana) { this.fabrica=Objects.requireNonNull(fabrica);this.campana=Objects.requireNonNull(campana); }
    // ───────── Modo historia «La noche del Palomo»: misión 5 → 4 minijuegos → misión 6 ─────────
    public final Campana campana;
    public final alcaldedigital.cliente.audio.OpcionesAudio audio=new alcaldedigital.cliente.audio.OpcionesAudio();
    /** Capítulo de la historia que se está jugando; {@code null} en el juego libre. */
    public Capitulo capitulo;
    public int paginaCapitulo, paginaEpilogo;
    /** Ventana Processing anfitriona: la misión 6 dibuja con PGraphics. En pruebas es {@code null}. */
    public processing.core.PApplet anfitrion;
    public MisionSeisSketch seis;
    private MisionSeisMotor.Fase faseSeis;
    public boolean enHistoria() { return capitulo!=null; }
    public boolean mision6EnCurso() { return seis!=null; }
    /** Puntaje de la misión 6: completarla vale 250 y cada corazón que queda suma 30. */
    public static int puntosMision6(MisionSeisMotor.Resultado r) {
        return 250+30*Math.max(0,r.integridad1())+30*Math.max(0,r.integridad2());
    }
    public void abrirCapitulo(Capitulo c) {
        if(c==null) {paginaEpilogo=0;abrir(Pantalla.EPILOGO);return;}
        if(!campana.disponible(c)) return;
        capitulo=c;paginaCapitulo=0;abrir(Pantalla.CAPITULO);
    }
    /** Empieza el capítulo abierto; {@code modo} solo cuenta en los minijuegos de un jugador. */
    public void jugarCapitulo(Minijuego.Modo modo) {
        if(capitulo==null) return;
        boolean dos=campana.dosJugadores();
        switch(capitulo) {
            case M5 -> iniciarMision5(dos?Mision5.Modo.DUO:Mision5.Modo.SOLO);
            case M6 -> abrirMision6();
            default -> iniciarMinijuego(capitulo.minijuego,dos?Minijuego.Modo.DUO:modo==null?Campana.modoRecomendado(capitulo):modo);
        }
    }
    /** Vuelve al capítulo de la historia que quedó a medias. */
    public boolean capituloEnCurso() {
        if(capitulo==null) return false;
        return switch(capitulo) {
            case M5 -> mision5EnCurso();
            case M6 -> seis!=null;
            default -> minijuegoEnCurso()&&minijuego.tipo()==capitulo.minijuego;
        };
    }
    private void reanudarCapitulo() {
        if(!capituloEnCurso()) {abrirCapitulo(capitulo==null?campana.actual():capitulo);return;}
        switch(capitulo) {
            case M5 -> abrir(introHecha5?Pantalla.MISION5:Pantalla.MISION5_INTRO);
            case M6 -> {seis.reanudar();abrir(Pantalla.MISION6);}
            default -> abrir(Pantalla.MINIJUEGO);
        }
    }
    public void abrirMision6() {
        if(seis==null) {seis=new MisionSeisSketch();seis.iniciarEn(anfitrion);}
        seis.reanudar();faseSeis=seis.motor().fase;abrir(Pantalla.MISION6);
    }
    private void pausarMision6() {if(seis!=null) seis.perderFoco();abrir(Pantalla.MISION6_PAUSA);}
    private void cerrarMision6() {if(seis!=null) {seis.cerrar();seis=null;}}
    private void salirMision6() {cerrarMision6();abrir(enHistoria()?Pantalla.HISTORIA:Pantalla.MENU);}
    /** Una historia nueva no debe ofrecer la partida guardada de la misión 6 de otra historia. */
    private void borrarGuardadoSeis() {
        cerrarMision6();
        var f=alcaldedigital.cliente.mision6.AutoguardadoSeis.ubicacion();
        try {java.nio.file.Files.deleteIfExists(f);java.nio.file.Files.deleteIfExists(f.resolveSibling(f.getFileName()+".bak"));}
        catch(java.io.IOException e) {aviso="No se pudo borrar la partida anterior de la misión 6.";}
    }
    private void actualizarMision6(double dt) {
        seis.actualizar((float)dt);
        var m=seis.motor();
        // En la historia ya se eligió cuántos juegan: la misión 6 no vuelve a preguntarlo.
        if(enHistoria()&&m.fase==MisionSeisMotor.Fase.MODO) m.comenzar(campana.dosJugadores()?MisionSeisMotor.Modo.DUO:MisionSeisMotor.Modo.SOLO);
        if(m.fase==MisionSeisMotor.Fase.DERROTA&&faseSeis!=MisionSeisMotor.Fase.DERROTA&&capitulo==Capitulo.M6) campana.derrota(Capitulo.M6);
        faseSeis=m.fase;
        if(seis.salidaSolicitada()) terminarMision6(seis.resultado());
    }
    /** Cierre de la misión 6: en la historia lleva al epílogo; suelta, vuelve al menú. */
    public void terminarMision6(MisionSeisMotor.Resultado r) {
        cerrarMision6();
        if(capitulo==Capitulo.M6&&r.completada()) {
            campana.ruta(r.ruta().name());campana.victoria(Capitulo.M6,puntosMision6(r));
            paginaEpilogo=0;abrir(Pantalla.EPILOGO);return;
        }
        if(r.completada()) {aviso="Misión 6 completada · ruta "+(r.ruta()==MisionSeisMotor.Ruta.ESCUCHA?"«Escuchar y adaptar»":"«Convicción y diálogo»");segundosAviso=8;}
        abrir(enHistoria()?Pantalla.HISTORIA:Pantalla.MENU);
    }
    /** Anota en la historia el resultado de la misión 5 o de un minijuego (una sola vez por partida). */
    private void registrarMision5() {
        boolean gano=mision5.estado()==Mision5.Estado.VICTORIA;
        if(capitulo==Capitulo.M5) {if(gano) campana.victoria(Capitulo.M5,mision5.puntos());else campana.derrota(Capitulo.M5);}
        else if(gano) campana.victoriaLibreM5();
    }
    private void registrarMinijuego() {
        if(capitulo==null||capitulo.minijuego!=minijuego.tipo()) return;
        if(minijuego.estado()==Minijuego.Estado.VICTORIA) campana.victoria(capitulo,minijuego.puntos());else campana.derrota(capitulo);
    }
    /** Botón «Continuar la historia» tras ganar un capítulo. */
    private void siguienteCapitulo() {
        Capitulo sig=campana.actual();
        if(sig==null) {paginaEpilogo=0;abrir(Pantalla.EPILOGO);} else abrirCapitulo(sig);
    }
    /** Destino de Esc / Volver dentro de una pantalla de la historia o del juego libre. */
    private Pantalla salidaDeJuego(Pantalla libre) {return enHistoria()?Pantalla.HISTORIA:libre;}
    public boolean contraste, movimientoReducido, tiempoAmpliado=true, salir;
    public long semilla=20260917;
    public int direccion, cuadro, foco, pasoAVL=-1, pasoDecision=-1, recorrido, recorridoNario, visita;
    public boolean automatico;
    public String busqueda="", encontrado="";
    public String ultimaAccion="";
    public double animacion, efecto;
    private long solicitud;
    public final Set<Integer> teclas=new HashSet<>();
    // Misión 5 · La calle del pánico (la lógica vive en Mision5; aquí solo control y navegación).
    public Mision5 mision5;
    public Mision5.Modo modo5=Mision5.Modo.SOLO;
    public int pagina5, seleccion5;
    public double tiempoFin5;
    private boolean introHecha5;
    public static final int PAGINAS_INTRO5=4;
    public boolean enPantallaMision5() {
        return pantalla==Pantalla.MISION5_MODO||pantalla==Pantalla.MISION5_INTRO||pantalla==Pantalla.MISION5||pantalla==Pantalla.MISION5_FIN;
    }
    /** La misión existe y todavía no terminó: el menú ofrece continuarla. */
    public boolean mision5EnCurso() { return mision5!=null&&!mision5.terminada(); }
    // Minijuegos de la misión 5 (la lógica vive en servidor/minijuegos; aquí solo control y navegación).
    public Minijuego minijuego;
    public Minijuego.Tipo tipoMini=Minijuego.Tipo.CADENA;
    public Minijuego.Modo modoMini=Minijuego.Modo.DUO;
    public double tiempoFinMini;
    /** Teclas tocadas desde el último cuadro: una pulsación muy corta no se pierde. */
    private final Set<Integer> toquesMini=new HashSet<>();
    public boolean enPantallaMinijuego() {
        return pantalla==Pantalla.MINIJUEGOS||pantalla==Pantalla.MINIJUEGO||pantalla==Pantalla.MINIJUEGO_FIN;
    }
    public boolean minijuegoEnCurso() { return minijuego!=null&&!minijuego.terminada(); }
    public void iniciarMinijuego(Minijuego.Tipo tipo,Minijuego.Modo modo) {
        tipoMini=tipo;modoMini=modo;minijuego=Minijuego.crear(tipo,modo,semilla);tiempoFinMini=0;toquesMini.clear();
        abrir(Pantalla.MINIJUEGO);
    }
    public void iniciarMision5(Mision5.Modo modo) {
        modo5=modo;mision5=new Mision5(modo,semilla);pagina5=0;seleccion5=0;tiempoFin5=0;introHecha5=false;
        abrir(Pantalla.MISION5_INTRO);
    }
    private void comenzarMision5() {
        if(mision5==null||mision5.terminada()) mision5=new Mision5(modo5,semilla);
        seleccion5=0;tiempoFin5=0;introHecha5=true;abrir(Pantalla.MISION5);
    }
    public void iniciar(Rol rol) {
        sesion=fabrica.crear(rol,semilla,tiempoAmpliado?60:10);
        pantalla=Pantalla.PARQUE; direccion=0; cuadro=0; pasoAVL=-1; pasoDecision=-1; foco=0;
        busqueda="";encontrado="";automatico=false;visita=0;efecto=0;teclas.clear();
        laboratorio=false;pestana=Pestana.RADAR;origenArboles=Pantalla.PARQUE;
        editandoBusqueda=false;cursorBusqueda=0;consulta.iniciar(sesion);aviso="";segundosAviso=0;
    }
    public void abrir(Pantalla destino) {
        if(destino==Pantalla.AYUDA&&pantalla!=destino) anterior=pantalla;
        if(destino==Pantalla.ARBOLES&&pantalla!=destino&&!(pantalla==Pantalla.AYUDA&&anterior==Pantalla.ARBOLES)) origenArboles=pantalla;
        pantalla=destino; foco=0; editandoBusqueda=false; teclas.clear();
    }
    public void cerrarArboles() { abrir(origenArboles); }
    public int indiceAVL() { return pasoAVL<0?sesion.pasosAVL().size()-1:Math.min(pasoAVL,sesion.pasosAVL().size()-1); }
    public int indiceDecision() { return pasoDecision<0?sesion.pasosDecisiones().size()-1:Math.min(pasoDecision,sesion.pasosDecisiones().size()-1); }
    public ArbolAVLPublicaciones.Vista vistaAVL() { return pasoAVL<0?sesion.vista().avl():sesion.pasosAVL().get(indiceAVL()).raiz(); }
    public ArbolDecisiones.Vista vistaDecision() { return pasoDecision<0?sesion.vista().decisiones():sesion.pasosDecisiones().get(indiceDecision()).raiz(); }
    private void seleccionCambiada() { visita=0;automatico=false;encontrado="";editandoBusqueda=false;foco=0; }
    public void accion(String accion) {
        if(accion.startsWith("nodo:")) {
            String id=accion.substring(5);var nodo=ConsultaEstructuras.buscar(vistaDecision(),id);
            encontrado=nodo==null?"No existe en este estado":id+": "+nodo.texto();return;
        }
        if(accion.startsWith("rol:")) { iniciar(Rol.valueOf(accion.substring(4))); return; }
        if(accion.startsWith("mj:")) {
            String[] partes=accion.split(":");capitulo=null;
            iniciarMinijuego(Minijuego.Tipo.valueOf(partes[1]),Minijuego.Modo.valueOf(partes[2]));return;
        }
        if(accion.startsWith("m5modo:")) { capitulo=null;iniciarMision5(Mision5.Modo.valueOf(accion.substring(7))); return; }
        if(accion.startsWith("hnueva:")) { borrarGuardadoSeis();campana.nueva(Integer.parseInt(accion.substring(7)));mision5=null;minijuego=null;abrirCapitulo(campana.actual()); return; }
        if(accion.startsWith("hcap:")) { abrirCapitulo(Capitulo.valueOf(accion.substring(5))); return; }
        if(accion.startsWith("hjugar")) { jugarCapitulo(accion.startsWith("hjugar:")?Minijuego.Modo.valueOf(accion.substring(7)):null); return; }
        if(accion.startsWith("m5v:")) {
            if(pantalla==Pantalla.MISION5&&mision5!=null) {seleccion5=Integer.parseInt(accion.substring(4));mision5.verificar(seleccion5);}
            return;
        }
        switch(accion) {
            case "jugar" -> abrir(Pantalla.ROLES);
            case "menu" -> abrir(Pantalla.MENU);
            case "salir" -> salir=true;
            case "contraste" -> contraste=!contraste;
            case "movimiento" -> movimientoReducido=!movimientoReducido;
            case "avisos" -> avisos=!avisos;
            case "tiempo" -> tiempoAmpliado=!tiempoAmpliado;
            case "semilla" -> semilla++;
            case "ayuda" -> abrir(Pantalla.AYUDA);
            case "mision5" -> { if(capitulo!=Capitulo.M5) capitulo=null;if(mision5EnCurso()) abrir(introHecha5?Pantalla.MISION5:Pantalla.MISION5_INTRO); else abrir(Pantalla.MISION5_MODO); }
            case "m5sig" -> { if(pagina5<PAGINAS_INTRO5-1) pagina5++; else comenzarMision5(); }
            case "m5empezar" -> comenzarMision5();
            case "m5reintentar" -> { mision5=new Mision5(modo5,++semilla);comenzarMision5(); }
            case "m5otromodo" -> { capitulo=null;abrir(Pantalla.MISION5_MODO); }
            case "minijuegos" -> { if(pantalla==Pantalla.MINIJUEGO||pantalla==Pantalla.MINIJUEGO_FIN) abrir(salidaDeJuego(Pantalla.MINIJUEGOS)); else {capitulo=null;abrir(Pantalla.MINIJUEGOS);} }
            case "mjcontinuar" -> { if(minijuegoEnCurso()) abrir(Pantalla.MINIJUEGO); }
            case "mjreintentar" -> { semilla++;iniciarMinijuego(tipoMini,modoMini); }
            case "historia" -> abrir(Pantalla.HISTORIA);
            case "hcontinuar" -> { if(campana.completada()) {paginaEpilogo=0;abrir(Pantalla.EPILOGO);} else abrirCapitulo(campana.actual()); }
            case "hreanudar" -> reanudarCapitulo();
            case "hsiguiente" -> siguienteCapitulo();
            case "hsig" -> { if(capitulo!=null&&paginaCapitulo<capitulo.relato().size()-1) paginaCapitulo++; }
            case "hsaltar" -> { if(capitulo!=null) paginaCapitulo=capitulo.relato().size()-1; }
            case "esig" -> { if(paginaEpilogo<campana.epilogo().size()) paginaEpilogo++; if(paginaEpilogo>=campana.epilogo().size()) campana.epilogoVisto(true); }
            case "mision6" -> { if(campana.mision6Desbloqueada()) {if(capitulo!=Capitulo.M6) {capitulo=null;}abrirMision6();}
                else {aviso="Gana la misión 5 para desbloquear la misión 6.";segundosAviso=6;} }
            case "seguir6" -> { if(seis!=null) {seis.reanudar();abrir(Pantalla.MISION6);} }
            case "salir6" -> salirMision6();
            case "sonido" -> audio.alternar("mudo");
            case "volver" -> { if(pantalla==Pantalla.ARBOLES) cerrarArboles();
                else if(pantalla==Pantalla.CAPITULO||pantalla==Pantalla.EPILOGO) abrir(Pantalla.HISTORIA);
                else if(pantalla==Pantalla.HISTORIA) abrir(Pantalla.MENU);
                else if(pantalla==Pantalla.MISION6_PAUSA) accion("seguir6");
                else if(enPantallaMision5()) abrir(salidaDeJuego(Pantalla.MENU));
                else if(pantalla==Pantalla.MINIJUEGO||pantalla==Pantalla.MINIJUEGO_FIN) abrir(salidaDeJuego(Pantalla.MINIJUEGOS));
                else if(pantalla==Pantalla.MINIJUEGOS) abrir(Pantalla.MENU);
                else abrir(pantalla==Pantalla.AYUDA?anterior:sesion==null?Pantalla.MENU:Pantalla.PARQUE); }
            case "civitas" -> abrir(Pantalla.CIVITAS);
            case "evidencias" -> abrir(Pantalla.EVIDENCIAS);
            case "arboles" -> { if(sesion!=null) {if(pantalla==Pantalla.ARBOLES) cerrarArboles();else abrir(Pantalla.ARBOLES);} }
            case "laboratorio" -> { if(sesion!=null) {if(pantalla!=Pantalla.ARBOLES) abrir(Pantalla.ARBOLES);laboratorio=true;seleccionCambiada();} }
            case "jugador" -> {laboratorio=false;seleccionCambiada();}
            case "radar" -> {pestana=Pestana.RADAR;seleccionCambiada();}
            case "camino" -> {pestana=Pestana.CAMINO;seleccionCambiada();}
            case "resultado" -> abrir(Pantalla.RESULTADO);
            case "avlAnterior" -> {pasoAVL=Math.max(0,indiceAVL()-1);seleccionCambiada();}
            case "avlSiguiente" -> {pasoAVL=Math.min(sesion.pasosAVL().size()-1,indiceAVL()+1);seleccionCambiada();}
            case "narAnterior" -> {pasoDecision=Math.max(0,indiceDecision()-1);seleccionCambiada();}
            case "narSiguiente" -> {pasoDecision=Math.min(sesion.pasosDecisiones().size()-1,indiceDecision()+1);seleccionCambiada();}
            case "actual" -> {if(pestana==Pestana.RADAR) pasoAVL=-1;else pasoDecision=-1;seleccionCambiada();}
            case "recorrido" -> {recorrido=(recorrido+1)%4;visita=0;}
            case "recorridoNario" -> {recorridoNario=(recorridoNario+1)%3;visita=0;}
            case "visitar" -> visita++;
            case "automatico" -> automatico=!automatico;
            case "campoBusqueda" -> { editandoBusqueda=true;cursorBusqueda=busqueda.length(); }
            case "buscar" -> {
                try { long id=Long.parseLong(busqueda);var p=ConsultaEstructuras.publicaciones(vistaAVL(),"inorden").stream().filter(v->v.id()==id).findFirst().orElse(null);
                    encontrado=p==null?"No existe en este estado":"#"+p.id()+" · "+p.autor()+" · riesgo "+p.riesgo(); }
                catch(NumberFormatException e) { encontrado="Escribe un ID numérico, por ejemplo 1."; }
            }
            case "interactuar" -> {
                if(sesion.cerca("aviso")) solicitar("aviso");
                else if(sesion.cerca("fuente")) solicitar("fuente");
                else solicitar("rol");
            }
            default -> solicitar(accion);
        }
    }
    private void solicitar(String accion) {
        if(sesion==null) return;
        int antes=sesion.vista().version();
        sesion.solicitar("local-"+(++solicitud),accion);
        registrarCambio(switch(accion) {
            case "fuente" -> "Contrastar ambas fuentes";case "aviso" -> "Leer aviso municipal";
            case "contexto" -> "Publicar contexto";case "guardar" -> "Guardar pruebas";
            default -> Character.toUpperCase(accion.charAt(0))+accion.substring(1);
        });
        if(sesion.vista().version()!=antes) { efecto=.8; ultimaAccion=accion; }
        if(sesion.vista().terminado()) abrir(Pantalla.RESULTADO);
        else if(accion.equals("verificar")) abrir(Pantalla.PARQUE);
    }
    private void registrarCambio(String causa) {
        if(consulta.registrar(sesion,causa)) {
            var evento=consulta.ultimo();aviso=evento.causa()+" · "+evento.resumen();segundosAviso=9;
        }
    }
    public void actualizar(double dt) {
        dt=Math.min(.1,Math.max(0,dt)); animacion+=dt;efecto=Math.max(0,efecto-dt);
        if(pantalla==Pantalla.PARQUE||pantalla==Pantalla.CIVITAS||pantalla==Pantalla.RESULTADO) segundosAviso=Math.max(0,segundosAviso-dt);
        if(pantalla==Pantalla.MISION5_INTRO||pantalla==Pantalla.MISION5_FIN) tiempoFin5+=dt;
        if(pantalla==Pantalla.EPILOGO||pantalla==Pantalla.MENU||pantalla==Pantalla.HISTORIA) segundosAviso=Math.max(0,segundosAviso-dt);
        if(pantalla==Pantalla.MISION6&&seis!=null) {actualizarMision6(dt);return;}
        if(pantalla==Pantalla.MISION5&&mision5!=null) {actualizarMision5(dt);return;}
        if(pantalla==Pantalla.MINIJUEGO&&minijuego!=null) {actualizarMinijuego(dt);return;}
        if(pantalla==Pantalla.MINIJUEGO_FIN&&minijuego!=null) {tiempoFinMini+=dt;minijuego.actualizar(dt);return;}
        if(sesion==null) return;
        if(pantalla==Pantalla.PARQUE||pantalla==Pantalla.CIVITAS) {
            sesion.actualizar(dt);
            registrarCambio("Plazo agotado: ignorar");
            if(sesion.vista().terminado()) { abrir(Pantalla.RESULTADO);return; }
        }
        if(pantalla==Pantalla.ARBOLES&&laboratorio&&automatico&&!movimientoReducido) {
            double antes=animacion-dt;
            if((int)(antes*1.5)!=(int)(animacion*1.5)) visita++;
        }
        if(pantalla!=Pantalla.PARQUE) return;
        float dx=(teclas.contains(68)||teclas.contains(39)?1:0)-(teclas.contains(65)||teclas.contains(37)?1:0);
        float dy=(teclas.contains(83)||teclas.contains(40)?1:0)-(teclas.contains(87)||teclas.contains(38)?1:0);
        if(dx!=0||dy!=0) {
            float n=(float)Math.hypot(dx,dy);dx/=n;dy/=n;
            direccion=Math.abs(dx)>Math.abs(dy)?(dx>0?2:1):(dy>0?0:3);
            // Subpasos para no atravesar una colisión cuando baja el framerate.
            int pasos=Math.max(1,(int)Math.ceil(dt*80/3));
            for(int i=0;i<pasos;i++) sesion.mover(dx*(float)dt*80/pasos,dy*(float)dt*80/pasos);
            cuadro=movimientoReducido?0:(int)(animacion*8)%4;
        } else cuadro=0;
    }
    /** Ciudadano: WASD (también flechas en un jugador). Con dos jugadores las flechas son del Periodista. */
    private void actualizarMision5(double dt) {
        boolean flechas=modo5==Mision5.Modo.SOLO;
        float dx=(teclas.contains(68)||(flechas&&teclas.contains(39))?1:0)-(teclas.contains(65)||(flechas&&teclas.contains(37))?1:0);
        float dy=(teclas.contains(83)||(flechas&&teclas.contains(40))?1:0)-(teclas.contains(87)||(flechas&&teclas.contains(38))?1:0);
        if(dx!=0||dy!=0) {
            float n=(float)Math.hypot(dx,dy);dx/=n;dy/=n;
            int pasos=Math.max(1,(int)Math.ceil(dt*70/3));
            for(int i=0;i<pasos;i++) mision5.mover(dx*(float)dt*70/pasos,dy*(float)dt*70/pasos);
        }
        mision5.actualizar(dt);
        int visibles=mision5.noticias().size();
        seleccion5=visibles==0?0:Math.max(0,Math.min(visibles-1,seleccion5));
        if(mision5.terminada()) {tiempoFin5=0;registrarMision5();abrir(Pantalla.MISION5_FIN);}
    }
    // Teclas: J1 Ciudadano = WASD + E/Espacio · J2 Periodista = flechas + Enter (o 0 del teclado numérico).
    // En un jugador, el personaje elegido acepta los dos juegos de teclas.
    private static final int[] ARRIBA1={87},ABAJO1={83},IZQ1={65},DER1={68},ACCION1={69,32};
    private static final int[] ARRIBA2={38},ABAJO2={40},IZQ2={37},DER2={39},ACCION2={10,13,96};
    private boolean tocada(int[]... grupos) {
        for(int[] g:grupos) for(int k:g) if(teclas.contains(k)||toquesMini.contains(k)) return true;
        return false;
    }
    private void actualizarMinijuego(double dt) {
        var m=minijuego;
        for(Minijuego.Jugador j:Minijuego.Jugador.values()) {
            if(!m.humano(j)) continue;
            boolean solo=modoMini!=Minijuego.Modo.DUO,ciu=j==Minijuego.Jugador.CIUDADANO;
            // En pareja cada personaje usa su lado del teclado; en un jugador, ambos lados.
            int[] ar=ciu||solo?ARRIBA1:new int[0],ab=ciu||solo?ABAJO1:new int[0],iz=ciu||solo?IZQ1:new int[0],de=ciu||solo?DER1:new int[0],ac=ciu||solo?ACCION1:new int[0];
            int[] ar2=!ciu||solo?ARRIBA2:new int[0],ab2=!ciu||solo?ABAJO2:new int[0],iz2=!ciu||solo?IZQ2:new int[0],de2=!ciu||solo?DER2:new int[0],ac2=!ciu||solo?ACCION2:new int[0];
            float dx=(tocada(de,de2)?1:0)-(tocada(iz,iz2)?1:0),dy=(tocada(ab,ab2)?1:0)-(tocada(ar,ar2)?1:0);
            m.entrada(j,dx,dy,tocada(ac,ac2));
        }
        m.actualizar(dt);
        toquesMini.clear();
        if(m.terminada()) {tiempoFinMini=0;registrarMinijuego();abrir(Pantalla.MINIJUEGO_FIN);}
    }
    private void teclaMision5(int codigo) {
        if(pantalla==Pantalla.MISION5_INTRO) {
            if(codigo==10||codigo==13||codigo==32) accion("m5sig");
            return;
        }
        if(pantalla!=Pantalla.MISION5||mision5==null) return;
        if(codigo==69||codigo==32) mision5.interactuar();                       // E / Espacio: calmar
        else if(codigo>=49&&codigo<=52) accion("m5v:"+(codigo-49));            // 1–4: verificar esa noticia
        else if(codigo==10||codigo==13) accion("m5v:"+seleccion5);              // Enter: verificar la seleccionada
        else if(modo5==Mision5.Modo.DUO&&codigo==38) seleccion5=Math.max(0,seleccion5-1);
        else if(modo5==Mision5.Modo.DUO&&codigo==40) seleccion5=Math.min(Math.max(0,mision5.noticias().size()-1),seleccion5+1);
    }
    /** Tecla soltada: solo la misión 6 necesita saberlo (movimiento y disparo mantenidos). */
    public void soltar(int codigo) {if(pantalla==Pantalla.MISION6&&seis!=null) seis.soltarTecla(codigo);}
    /** La ventana perdió el foco: la misión 6 se pausa para no seguir sin jugador. */
    public void perderFoco() {teclas.clear();if(pantalla==Pantalla.MISION6&&seis!=null) pausarMision6();}
    public void tecla(int codigo,char caracter) {
        if(pantalla==Pantalla.MISION6) {
            if(seis==null) {abrir(Pantalla.MENU);return;}
            // Esc o P pausan; el resto de teclas va directo al motor de la misión 6.
            if(codigo==27||(codigo==80&&!seis.eligiendoGuardado())) pausarMision6(); else seis.pulsarTecla(caracter,codigo);
            return;
        }
        if(codigo==27) {
            if(pantalla==Pantalla.ARBOLES) {cerrarArboles();return;}
            if(pantalla==Pantalla.MISION6_PAUSA) {accion("seguir6");return;}
            if(pantalla==Pantalla.CAPITULO||pantalla==Pantalla.EPILOGO) {abrir(Pantalla.HISTORIA);return;}
            if(pantalla==Pantalla.HISTORIA) {abrir(Pantalla.MENU);return;}
            if(enPantallaMision5()) {abrir(salidaDeJuego(Pantalla.MENU));return;}
            if(pantalla==Pantalla.MINIJUEGO||pantalla==Pantalla.MINIJUEGO_FIN) {abrir(salidaDeJuego(Pantalla.MINIJUEGOS));return;}
            if(pantalla==Pantalla.MINIJUEGOS) {abrir(Pantalla.MENU);return;}
            Pantalla destino=pantalla==Pantalla.PARQUE?Pantalla.MENU:
                pantalla==Pantalla.AYUDA?anterior:
                sesion==null?Pantalla.MENU:Pantalla.PARQUE;
            abrir(destino);return;
        }
        if(pantalla==Pantalla.ARBOLES&&laboratorio&&editandoBusqueda) {
            if(caracter>='0'&&caracter<='9'&&busqueda.length()<19) {
                busqueda=busqueda.substring(0,cursorBusqueda)+caracter+busqueda.substring(cursorBusqueda);cursorBusqueda++;return;
            }
            if(codigo==8) {if(cursorBusqueda>0) {busqueda=busqueda.substring(0,cursorBusqueda-1)+busqueda.substring(cursorBusqueda);cursorBusqueda--;}return;}
            if(codigo==127) {if(cursorBusqueda<busqueda.length()) busqueda=busqueda.substring(0,cursorBusqueda)+busqueda.substring(cursorBusqueda+1);return;}
            if(codigo==37) {cursorBusqueda=Math.max(0,cursorBusqueda-1);return;}
            if(codigo==39) {cursorBusqueda=Math.min(busqueda.length(),cursorBusqueda+1);return;}
            if(codigo==36) {cursorBusqueda=0;return;}
            if(codigo==35) {cursorBusqueda=busqueda.length();return;}
            if(codigo==10||codigo==13) {accion("buscar");return;}
        }
        if(pantalla==Pantalla.CAPITULO&&capitulo!=null&&(codigo==32||codigo==39)) {
            if(paginaCapitulo<capitulo.relato().size()-1) accion("hsig"); else if(codigo==32) accion("hjugar");
            return;
        }
        if(pantalla==Pantalla.EPILOGO&&(codigo==32||codigo==39)) {accion("esig");return;}
        if(enPantallaMision5()) {teclaMision5(codigo);return;}
        if(pantalla==Pantalla.MINIJUEGO) {toquesMini.add(codigo);return;}
        if(enPantallaMinijuego()) return;
        if(sesion==null) {if(codigo==72) accion("ayuda"); return;}
        if(codigo==72) accion("ayuda");
        if(codigo==67) accion("civitas");
        if(codigo==73||codigo==74) accion("evidencias");
        if(codigo==84) accion("arboles");
        if((codigo==69||codigo==32)&&pantalla==Pantalla.PARQUE) accion("interactuar");
    }
}
