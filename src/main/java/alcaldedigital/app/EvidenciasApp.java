package alcaldedigital.app;
import alcaldedigital.cliente.*;
import alcaldedigital.cliente.interfaz.*;
import alcaldedigital.compartido.modelo.*;
import alcaldedigital.servidor.misiones.Mision5;
import alcaldedigital.servidor.minijuegos.*;
import alcaldedigital.servidor.sesion.SesionLocal;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Capturas deterministas desde el mismo render y controlador del juego. */
public final class EvidenciasApp {
    public static void main(String[] args) throws Exception {
        Path destino=Path.of(args.length>0?args[0]:"entregas/entrega1/evidencias");Files.createDirectories(destino);
        var juego=new JuegoCliente(SesionLocal::new);var render=new RenderJuego();
        captura(destino,"01-menu",juego,render);juego.accion("jugar");captura(destino,"02-roles",juego,render);
        juego.iniciar(Rol.CIUDADANO);captura(destino,"03-parque",juego,render);
        juego.accion("civitas");captura(destino,"04-civitas",juego,render);
        juego.accion("arboles");captura(destino,"12-radar-inicial",juego,render);
        juego.accion("camino");captura(destino,"13-camino-inicial",juego,render);
        juego.accion("laboratorio");juego.accion("radar");captura(destino,"05-avl-rotacion",juego,render);
        juego.accion("verificar");mover(juego,192,174);juego.accion("interactuar");
        captura(destino,"03b-parque-investigacion",juego,render);
        mover(juego,230,208);juego.accion("interactuar");juego.accion("evidencias");captura(destino,"06-evidencias",juego,render);
        juego.accion("volver");captura(destino,"14-aviso-fuentes",juego,render);
        juego.accion("arboles");juego.accion("jugador");captura(destino,"15-radar-verificado",juego,render);
        juego.accion("camino");captura(destino,"16-camino-desbloqueado",juego,render);
        juego.accion("laboratorio");captura(destino,"07-insercion-opciones",juego,render);
        juego.accion("contexto");
        juego.segundosAviso=0;captura(destino,"08-resultado",juego,render);
        juego.accion("arboles");captura(destino,"09-eliminacion-poda",juego,render);
        juego.accion("radar");captura(destino,"17-avl-correccion",juego,render);
        juego.pasoAVL=0;juego.accion("campoBusqueda");juego.tecla(49,'1');juego.accion("buscar");
        captura(destino,"18-busqueda-historica",juego,render);
        juego.accion("jugador");captura(destino,"19-radar-cierre",juego,render);
        juego.accion("ayuda");captura(destino,"10-ayuda",juego,render);
        juego.contraste=true;juego.accion("resultado");captura(destino,"11-alto-contraste",juego,render);
        juego.accion("arboles");juego.accion("camino");captura(destino,"20-camino-contraste",juego,render);
        transicion(destino);
        mision5(destino.toAbsolutePath().getParent().getParent().resolve("mision5").resolve("evidencias"));
        minijuegos(destino.toAbsolutePath().getParent().getParent().resolve("minijuegos").resolve("evidencias"));
        System.out.println("21 capturas de pantallas y 5 cuadros de transición reproducibles en "+destino);
    }
    /** Capturas de la misión 5 con un jugador automático que verifica y calma de forma constante. */
    public static void mision5(Path destino) throws Exception {
        Files.createDirectories(destino);
        var juego=new JuegoCliente(SesionLocal::new);var render=new RenderJuego();
        juego.accion("mision5");captura(destino,"m5-01-modo",juego,render);
        juego.accion("m5modo:SOLO");
        for(int p=0;p<JuegoCliente.PAGINAS_INTRO5;p++) {captura(destino,"m5-02-intro-"+(p+1),juego,render);juego.accion("m5sig");}
        captura(destino,"m5-03-cuenta-atras",juego,render);
        avanzar(juego,3.2);
        boolean tomada=false;
        for(int i=0;i<60*80&&juego.pantalla==JuegoCliente.Pantalla.MISION5;i++) {
            jugar(juego);
            if(!tomada&&juego.mision5.transcurrido()>18) {captura(destino,"m5-04-juego",juego,render);tomada=true;}
        }
        avanzar(juego,1.2);captura(destino,"m5-05-"+(juego.mision5.estado()==Mision5.Estado.VICTORIA?"victoria":"derrota"),juego,render);
        var perdida=new JuegoCliente(SesionLocal::new);
        perdida.accion("m5modo:DUO");perdida.accion("m5empezar");avanzar(perdida,3.2);
        while(perdida.pantalla==JuegoCliente.Pantalla.MISION5) perdida.actualizar(1/60.0);
        avanzar(perdida,1.2);captura(destino,"m5-06-derrota",perdida,render);
        var duo=new JuegoCliente(SesionLocal::new);duo.contraste=true;
        duo.accion("m5modo:DUO");duo.accion("m5empezar");avanzar(duo,3.2);
        for(int i=0;i<60*17;i++) jugar(duo);
        captura(destino,"m5-07-pareja-contraste",duo,render);
    }
    /** Capturas de los cuatro minijuegos: selector, cuenta atrás, partida en curso y resultado. */
    public static void minijuegos(Path destino) throws Exception {
        Files.createDirectories(destino);
        var juego=new JuegoCliente(SesionLocal::new);var render=new RenderJuego();
        juego.accion("menu");captura(destino,"mj-00-menu-principal",juego,render);
        juego.accion("minijuegos");captura(destino,"mj-01-selector",juego,render);
        String[] nombres={"cadena","fuente","duelo","bots"};
        double[] momento={22,30,14,24};
        Minijuego.Modo[] modos={Minijuego.Modo.DUO,Minijuego.Modo.SOLO_CIUDADANO,Minijuego.Modo.DUO,Minijuego.Modo.SOLO_PERIODISTA};
        for(int i=0;i<4;i++) {
            var tipo=Minijuego.Tipo.values()[i];
            var j=new JuegoCliente(SesionLocal::new);
            j.accion("mj:"+tipo.name()+":"+modos[i].name());
            avanzar(j,1.2);captura(destino,"mj-"+(i+2)+"a-"+nombres[i]+"-cuenta-atras",j,render);
            avanzar(j,2.0);
            boolean tomada=false;
            for(int k=0;k<60*120&&j.pantalla==JuegoCliente.Pantalla.MINIJUEGO;k++) {
                jugarMinijuego(j);
                if(!tomada&&j.minijuego.transcurrido()>momento[i]) {captura(destino,"mj-"+(i+2)+"b-"+nombres[i]+"-juego",j,render);tomada=true;}
            }
            avanzar(j,1);captura(destino,"mj-"+(i+2)+"c-"+nombres[i]+"-"+(j.minijuego.estado()==Minijuego.Estado.VICTORIA?"victoria":"derrota"),j,render);
        }
        var perdida=new JuegoCliente(SesionLocal::new);perdida.contraste=true;
        perdida.accion("mj:CADENA:DUO");avanzar(perdida,3.2);
        while(perdida.pantalla==JuegoCliente.Pantalla.MINIJUEGO) perdida.actualizar(1/60.0);
        avanzar(perdida,1);captura(destino,"mj-6-derrota-contraste",perdida,render);
    }
    /** Jugador automático sencillo para las capturas: solo usa la información visible en pantalla. */
    private static void jugarMinijuego(JuegoCliente j) {
        var m=j.minijuego;
        j.teclas.clear();
        if(m.estado()==Minijuego.Estado.EN_CURSO) for(var q:Minijuego.Jugador.values()) {
            if(!m.humano(q)) continue;
            boolean ciu=q==Minijuego.Jugador.CIUDADANO,duo=m.modo()==Minijuego.Modo.DUO;
            int arriba=ciu||!duo?87:38,abajo=ciu||!duo?83:40,izq=ciu||!duo?65:37,der=ciu||!duo?68:39,accion=ciu||!duo?69:10;
            var yo=m.avatarVista(q);
            float[] meta=metaDe(m,q);
            if(meta==null) continue;
            float dx=meta[0]-yo.x(),dy=meta[1]-yo.y();
            if(Math.abs(dx)>4) j.teclas.add(dx>0?der:izq);
            if(Math.abs(dy)>4) j.teclas.add(dy>0?abajo:arriba);
            boolean pulsar=meta[2]>0;
            if(pulsar&&m instanceof DueloPalomo) j.teclas.add(accion);
            else if(pulsar&&!yo.ocupado()&&((int)(m.transcurrido()*60))%20==0) j.teclas.add(accion);
        }
        j.actualizar(1/60.0);
    }
    /** {x, y, 1 si hay que pulsar/mantener la acción}. */
    private static float[] metaDe(Minijuego m,Minijuego.Jugador q) {
        var yo=m.avatarVista(q);
        switch(m) {
            case CortaLaCadena c -> {
                CortaLaCadena.NodoVista mejor=null;double d=1e9;
                for(var n:c.nodos()) {
                    boolean util=n.estado()==CortaLaCadena.EstadoNodo.CONTAGIADO||(n.estado()==CortaLaCadena.EstadoNodo.SANO&&n.distancia()==1);
                    double dn=Math.hypot(n.x()-yo.x(),n.y()-yo.y());
                    if(util&&dn<d) {d=dn;mejor=n;}
                }
                if(mejor==null) return null;
                return new float[]{mejor.x(),mejor.y(),d<16?1:0};
            }
            case RastreaLaFuente f -> {
                if(f.silenciada()>=0) {var p=m.palomo();return new float[]{p.x(),p.y()+20,Math.hypot(p.x()-yo.x(),p.y()+20-yo.y())<20?1:0};}
                if(f.buscado()<0) return null;
                var p=f.personas().get(f.buscado());
                return new float[]{p.x(),p.y()+8,Math.hypot(p.x()-yo.x(),p.y()-yo.y())<16?1:0};
            }
            case DueloPalomo d -> {
                var p=m.palomo();
                boolean cargar=d.carga(q)<(q==Minijuego.Jugador.CIUDADANO?55:80)&&Math.abs(p.x()-yo.x())<40;
                return new float[]{p.x()+(q==Minijuego.Jugador.CIUDADANO?-12:12),yo.y(),cargar?1:0};
            }
            case CazaDeBots b -> {
                CazaDeBots.CuentaVista mejor=null;double d=1e9;
                for(var c:b.cuentas()) {
                    if(c.reportada()||!b.tabla().contiene(c.huella().clave())) continue;
                    double dc=Math.hypot(c.x()-yo.x(),c.y()-yo.y());
                    if(dc<d) {d=dc;mejor=c;}
                }
                if(mejor==null) return null;
                return new float[]{mejor.x(),mejor.y(),d<(q==Minijuego.Jugador.CIUDADANO?14:30)?1:0};
            }
            default -> {return null;}
        }
    }
    private static void avanzar(JuegoCliente j,double segundos) {for(double t=0;t<segundos;t+=1/60.0) j.actualizar(1/60.0);}
    private static void jugar(JuegoCliente j) {
        var m=j.mision5;
        if(m.estado()==Mision5.Estado.EN_CURSO) {
            if(!m.verificando()&&!m.noticias().isEmpty()) j.accion("m5v:0");
            var yo=m.jugador();j.teclas.clear();
            if(!yo.hablando()) {
                Mision5.VecinoVista meta=null;
                for(var v:m.vecinos()) if(v.conducta()==Mision5.Conducta.ALTERADO&&(meta==null||v.panico()>meta.panico())) meta=v;
                if(meta!=null) {
                    float dx=meta.x()-yo.x(),dy=meta.y()-yo.y();
                    if(Math.hypot(dx,dy)>20) {if(Math.abs(dx)>3) j.teclas.add(dx>0?68:65);if(Math.abs(dy)>3) j.teclas.add(dy>0?83:87);}
                    else j.tecla(69,'e');
                }
            }
        }
        j.actualizar(1/60.0);j.teclas.clear();
    }
    static void transicion(Path destino) throws Exception {
        var j=new JuegoCliente(SesionLocal::new);j.iniciar(Rol.CIUDADANO);
        var r=new RenderJuego();var t=new TransicionPixel();
        t.presentar(r.dibujar(j),"parque",0,false,false);j.accion("ayuda");
        for(int i=0;i<5;i++) ImageIO.write(t.presentar(r.dibujar(j),"ayuda",i==0?0:.061,false,true),
            "png",destino.resolve("transicion-"+i+".png").toFile());
    }
    static void captura(Path p,String nombre,JuegoCliente j,RenderJuego r) throws Exception {ImageIO.write(r.dibujar(j),"png",p.resolve(nombre+".png").toFile());}
    static void mover(JuegoCliente j,int x,int y) {
        for(int i=0;i<1000&&Math.abs(j.sesion.vista().x()-x)>.5;i++) j.sesion.mover(Math.signum(x-j.sesion.vista().x())*2,0);
        for(int i=0;i<1000&&Math.abs(j.sesion.vista().y()-y)>.5;i++) j.sesion.mover(0,Math.signum(y-j.sesion.vista().y())*2);
        if(Math.abs(j.sesion.vista().x()-x)>2||Math.abs(j.sesion.vista().y()-y)>2) throw new IllegalStateException("Ruta bloqueada");
    }
}
