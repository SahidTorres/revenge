package alcaldedigital.cliente;

import processing.core.PApplet;
import processing.core.PImage;
import alcaldedigital.cliente.interfaz.RenderJuego;
import alcaldedigital.cliente.interfaz.TransicionPixel;
import alcaldedigital.compartido.modelo.Rol;
import alcaldedigital.cliente.audio.DirectorMusical;
import alcaldedigital.cliente.audio.MotorAudio;
import java.awt.image.BufferedImage;

/**
 * Ventana Processing. Entrada y presentación del capítulo jugable de la entrega 1.
 */
public final class ClienteSketch extends PApplet {
    public static final int ANCHO = 1280;
    public static final int ALTO = 720;
    private final JuegoCliente juego;
    public ClienteSketch(JuegoCliente juego) { this.juego=juego; }
    private final RenderJuego render = new RenderJuego();
    private PImage imagen;
    private final TransicionPixel transicion=new TransicionPixel();
    private String claveVisual() {return juego.pantalla+":"+(juego.pantalla==JuegoCliente.Pantalla.ARBOLES?juego.pestana+":"+juego.laboratorio:"");}
    private boolean entradaLista() {return transicion.lista(claveVisual());}
    private long anterior;
    private String tituloActual="";
    private final java.util.Set<Integer> pulsadas=new java.util.HashSet<>();
    private final DirectorMusical director=new DirectorMusical(MotorAudio.global());

    @Override
    public void settings() {
        size(ANCHO, ALTO, JAVA2D);
        noSmooth();
    }

    @Override
    public void setup() {
        surface.setTitle("Alcalde Digital — La noche del Palomo");
        frameRate(60);
        juego.anfitrion=this;
        MotorAudio.global().iniciar(juego.audio);
        imagen = createImage(ANCHO, ALTO, ARGB);
        anterior = System.nanoTime();
    }

    @Override
    public void draw() {
        long ahora = System.nanoTime();
        double dt=(ahora-anterior)/1_000_000_000.0; anterior=ahora;
        director.observar(juego);
        // La misión 6 se dibuja con Processing sobre su propio lienzo, sin la transición de Java2D.
        if(juego.pantalla==JuegoCliente.Pantalla.MISION6&&juego.seis!=null) {
            juego.actualizar(dt);
            if(juego.pantalla==JuegoCliente.Pantalla.MISION6&&juego.seis!=null) {image(juego.seis.dibujarEnJuego(),0,0);titulo();return;}
        }
        // El tiempo de apertura pertenece a la interfaz: no consume el plazo de decisión.
        if(entradaLista()) juego.actualizar(dt);
        else juego.animacion+=Math.max(0,Math.min(.1,dt));
        BufferedImage cuadro=render.dibujar(juego);
        boolean panel=juego.pantalla!=JuegoCliente.Pantalla.MENU&&juego.pantalla!=JuegoCliente.Pantalla.ROLES&&juego.pantalla!=JuegoCliente.Pantalla.PARQUE
            &&juego.pantalla!=JuegoCliente.Pantalla.MISION5_MODO&&juego.pantalla!=JuegoCliente.Pantalla.MISION5
            &&juego.pantalla!=JuegoCliente.Pantalla.MINIJUEGOS&&juego.pantalla!=JuegoCliente.Pantalla.MINIJUEGO;
        cuadro=transicion.presentar(cuadro,claveVisual(),dt,juego.movimientoReducido,panel);
        imagen.loadPixels();cuadro.getRGB(0,0,ANCHO,ALTO,imagen.pixels,0,ANCHO);imagen.updatePixels();
        image(imagen,0,0);
        titulo();
        if(juego.salir) exit();
        if(Boolean.getBoolean("ad.smoke")) {
            if(frameCount==30) juego.iniciar(Rol.CIUDADANO);
            if(frameCount==60) juego.accion("verificar");
            if(frameCount==90) juego.accion("arboles");
            if(frameCount==100) {
                if(juego.laboratorio) throw new IllegalStateException("T debe abrir la vista de jugador en un caso nuevo");
                juego.tecla(84,'t');
                if(juego.pantalla!=JuegoCliente.Pantalla.PARQUE) throw new IllegalStateException("T no volvió al origen");
            }
            if(frameCount==110) {juego.accion("civitas");juego.accion("arboles");juego.accion("laboratorio");}
            if(frameCount==120) {juego.accion("campoBusqueda");juego.tecla(49,'1');juego.accion("buscar");}
            if(frameCount==150) {
                if(!juego.encontrado.contains("#1")) throw new IllegalStateException("Búsqueda no resuelta");
                // PApplet.save puede diferir la escritura; guardar el frame sin salir antes de terminar.
                String destino=java.nio.file.Path.of(System.getProperty("ad.captura","entregas/entrega1/evidencias/processing-real.png")).toAbsolutePath().toString();
                if(!get().save(destino)) throw new IllegalStateException("No se pudo guardar la captura Processing");
                juego.tecla(84,'t');
                if(juego.pantalla!=JuegoCliente.Pantalla.CIVITAS) throw new IllegalStateException("Retorno incorrecto a Civitas");
                System.out.println("PROCESSING_SMOKE_OK: 150 cuadros, vista de jugador, Laboratorio, búsqueda y retorno a Civitas.");exit();
            }
        }
    }
    private void titulo() {
        String capitulo=juego.enHistoria()?" · Historia, capítulo "+juego.capitulo.numero():"";
        String titulo=juego.enPantallaMision5()?"Alcalde Digital — Misión 5 · La calle del pánico"+capitulo
            :juego.enPantallaMinijuego()?"Alcalde Digital — Minijuegos"+capitulo
            :juego.pantalla==JuegoCliente.Pantalla.MISION6||juego.pantalla==JuegoCliente.Pantalla.MISION6_PAUSA?"Alcalde Digital — Misión 6 · Cielos de campaña"+capitulo
            :"Alcalde Digital — La noche del Palomo";
        if(!titulo.equals(tituloActual)) {tituloActual=titulo;surface.setTitle(titulo);}
    }
    @Override public void mousePressed() {
        if(juego.pantalla==JuegoCliente.Pantalla.MISION6) return;
        if(!entradaLista()) return;
        if(render.pulsar(juego,mouseX,mouseY)) MotorAudio.global().interfaz("click");
    }
    @Override public void mouseMoved() {render.apuntar(mouseX,mouseY);}
    @Override public void mouseExited() {render.apuntar(-1,-1);}
    @Override public void keyPressed() {
        int codigo=keyCode;char caracter=key;
        if(key==ESC) key=0;
        // Misión 6: todas las teclas (también Tab y Enter) van a su motor; la repetición del teclado se ignora.
        if(juego.pantalla==JuegoCliente.Pantalla.MISION6) {if(pulsadas.add(codigo)) juego.tecla(codigo,caracter);return;}
        if(!entradaLista()) {if(codigo==ESC) juego.tecla(codigo,caracter);return;}
        boolean nueva=pulsadas.add(codigo);
        if(!nueva&&!juego.editandoBusqueda) return;
        // En la misión 5 y en los minijuegos, Enter es la acción del Periodista y Tab no mueve el foco.
        if(juego.pantalla==JuegoCliente.Pantalla.MISION5||juego.pantalla==JuegoCliente.Pantalla.MINIJUEGO) {juego.teclas.add(codigo);juego.tecla(codigo,caracter);return;}
        if(codigo==TAB) {render.avanzarFoco(juego,keyEvent.isShiftDown()?-1:1);return;}
        if(codigo==ENTER||codigo==RETURN) {
            MotorAudio.global().interfaz("confirmar");render.activarFoco(juego);return;
        }
        juego.teclas.add(codigo);juego.tecla(codigo,caracter);
    }
    @Override public void keyReleased() {juego.soltar(keyCode);juego.teclas.remove(keyCode);pulsadas.remove(keyCode);}
    @Override public void focusLost() {juego.perderFoco();pulsadas.clear();}
    @Override public void exit() {if(juego.seis!=null) juego.seis.cerrar();MotorAudio.global().close();super.exit();}

}
