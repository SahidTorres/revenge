package alcaldedigital.cliente.interfaz;

import alcaldedigital.cliente.*;
import alcaldedigital.cliente.recursos.Recursos;
import alcaldedigital.cliente.visualizadores.ConsultaEstructuras;
import alcaldedigital.compartido.modelo.*;
import alcaldedigital.compartido.estructuras.avl.ArbolAVLPublicaciones;
import alcaldedigital.compartido.estructuras.decisiones.ArbolDecisiones;
import alcaldedigital.servidor.misiones.Mision5;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/** Render compartido por Processing y las capturas de evidencia, sin ventanas ocultas. */
public final class RenderJuego {
    public record Boton(Rectangle area,String accion,String texto) { }
    final Recursos recursos=new Recursos();
    private final RenderMinijuegos minijuegos=new RenderMinijuegos(this);
    private final RenderHistoria historia=new RenderHistoria(this);
    final TemaPixel tema=new TemaPixel();
    int ratonX=-1,ratonY=-1;
    private String presionado="";
    private double finPulsacion;
    private boolean fondoMundo;
    public void apuntar(int x,int y) {ratonX=x;ratonY=y;}
    final List<Boton> botones=new ArrayList<>();
    private final BufferedImage lienzo=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g;
    JuegoCliente juego;
    Color tinta=new Color(0x18283D), papel=new Color(0xF7E7C6), tenue=new Color(0xA9BEBE), oro=new Color(0xE5AA45), teal=new Color(0x287F82);
    public List<Boton> botones() { return List.copyOf(botones); }
    public void avanzarFoco(JuegoCliente j,int paso) {
        if(botones.isEmpty()) return;
        j.foco=Math.floorMod(j.foco+paso,botones.size());
        j.editandoBusqueda=botones.get(j.foco).accion().equals("campoBusqueda");
        if(j.editandoBusqueda) j.cursorBusqueda=j.busqueda.length();
    }
    public void activarFoco(JuegoCliente j) {
        if(j.editandoBusqueda) {j.accion("buscar");return;}
        if(!botones.isEmpty()) j.accion(botones.get(Math.floorMod(j.foco,botones.size())).accion());
    }
    /** Pulsa el botón bajo el puntero; devuelve si había uno (para el sonido de clic). */
    public boolean pulsar(JuegoCliente j,int x,int y) {
        for(int i=0;i<botones.size();i++) if(botones.get(i).area().contains(x,y)) {
            presionado=botones.get(i).accion();finPulsacion=j.animacion+.12;
            j.foco=i;j.editandoBusqueda=false;j.accion(botones.get(i).accion());return true;
        }
        j.editandoBusqueda=false;return false;
    }
    public BufferedImage dibujar(JuegoCliente juego) {
        // Defensa: una pantalla de la misión 5 siempre tiene una misión que mostrar.
        if(juego.enPantallaMision5()&&juego.mision5==null) juego.mision5=new Mision5(juego.modo5,juego.semilla);
        if((juego.pantalla==JuegoCliente.Pantalla.MINIJUEGO||juego.pantalla==JuegoCliente.Pantalla.MINIJUEGO_FIN)&&juego.minijuego==null)
            juego.minijuego=alcaldedigital.servidor.minijuegos.Minijuego.crear(juego.tipoMini,juego.modoMini,juego.semilla);
        this.juego=juego; botones.clear(); g=lienzo.createGraphics();
        TemaPixel.nitido(g);
        tinta=juego.contraste?Color.BLACK:TemaPixel.NOCHE; papel=juego.contraste?Color.WHITE:TemaPixel.CREMA;
        oro=TemaPixel.ORO;tenue=new Color(juego.contraste?0xFFFFFF:0xACD9DF);teal=new Color(0x197E87);
        rect(0,0,1280,720,tinta);
        if(juego.sesion!=null&&juego.pantalla!=JuegoCliente.Pantalla.MENU&&juego.pantalla!=JuegoCliente.Pantalla.PARQUE&&!juego.enPantallaMision5()&&!RenderHistoria.propia(juego.pantalla)) {
            fondoMundo=true;mundo(0,0,1280,720);fondoMundo=false;
            rect(0,0,1280,720,new Color(2,18,32,juego.contraste?250:195));
        } else {
            for(int y=0;y<720;y+=8) rect(0,y,1280,2,new Color(255,255,255,4));
        }
        switch(juego.pantalla) {
            case MENU -> menu(); case ROLES -> roles(); case PARQUE -> parque();
            case CIVITAS -> civitas(); case EVIDENCIAS -> evidencias();
            case ARBOLES -> arboles(); case AYUDA -> ayuda(); case RESULTADO -> resultado();
            case MISION5_MODO -> mision5Modo(); case MISION5_INTRO -> mision5Intro();
            case MISION5 -> mision5(); case MISION5_FIN -> mision5Fin();
            case MINIJUEGOS -> minijuegos.menu(); case MINIJUEGO -> minijuegos.juego(); case MINIJUEGO_FIN -> minijuegos.fin();
            case HISTORIA -> historia.mapa(); case CAPITULO -> historia.capitulo(); case EPILOGO -> historia.epilogo();
            case MISION6 -> historia.mision6(); case MISION6_PAUSA -> historia.pausa6();
        }
        if(!botones.isEmpty()&&juego.pantalla!=JuegoCliente.Pantalla.MISION5&&juego.pantalla!=JuegoCliente.Pantalla.MINIJUEGO) {
            juego.foco=Math.floorMod(juego.foco,botones.size());
            Rectangle r=botones.get(juego.foco).area();g.setStroke(new BasicStroke(2));g.setColor(oro);
            g.drawRect(r.x-4,r.y-4,r.width+8,r.height+8);
            rect(r.x-7,r.y+r.height/2-3,6,6,oro);
        }
        g.setStroke(new BasicStroke(2));g.setColor(papel);g.drawRect(10,10,1260,700);
        for(int x:new int[]{10,1258}) for(int y:new int[]{10,698}) {g.setColor(oro);g.drawRect(x,y,12,12);}
        g.dispose(); return lienzo;
    }
    void rect(int x,int y,int w,int h,Color c) { g.setColor(c);g.fillRect(x,y,w,h); }
    void panel(int x,int y,int w,int h,Color c) {
        boolean claro=c.getRed()>180&&c.getGreen()>160;
        if(!claro&&c.getAlpha()==255) c=juego.contraste?Color.BLACK:TemaPixel.PANEL;
        TemaPixel.marco(g,x,y,w,h,c,claro?papel:juego.contraste?Color.WHITE:TemaPixel.CIAN,w>450&&h>200);
    }
    void texto(String t,int x,int y,int size,Color c,boolean bold) {
        if(size>=12&&size<17) size+=2;
        g.setFont(tema.fuente(size,bold));
        Font fuente=tema.fuente(size,bold);
        if(size>=28) {g.setColor(new Color(0,8,20,130));tema.escribir(g,t,x+2,y+3,fuente);}
        g.setColor(c);tema.escribir(g,t,x,y,fuente);
    }
    int parrafo(String t,int x,int y,int w,int size,Color c) {
        g.setFont(tema.fuente(size,false));g.setColor(c);int baseline=y;String linea="";
        for(String palabra:t.split(" ")) {
            if(tema.ancho(g,linea+palabra,tema.fuente(size,false))>w&&!linea.isEmpty()) {tema.escribir(g,linea.strip(),x,baseline,tema.fuente(size,false));baseline+=size+7;linea="";}
            linea+=palabra+" ";
        }
        if(!linea.isEmpty()) tema.escribir(g,linea.strip(),x,baseline,tema.fuente(size,false));
        return baseline+size+7;
    }
    void imagen(String recurso,int x,int y,int w,int h) {g.drawImage(recursos.imagen(recurso),x,y,w,h,null);}
    void boton(String t,String action,int x,int y,int w,boolean principal) {
        boolean hover=new Rectangle(x,y,w,42).contains(ratonX,ratonY);
        boolean down=action.equals(presionado)&&juego.animacion<finPulsacion;
        int dy=down?2:0;
        Color fondo=principal?oro:juego.contraste?Color.BLACK:hover?new Color(0x176582):TemaPixel.AZUL;
        String icono=switch(action) {
            case "compartir" -> "07_compartir";case "verificar" -> "08_verificar";
            case "reportar" -> "09_reportar";case "ignorar" -> "10_ignorar";
            case "contraste","movimiento" -> "24_accesibilidad";case "ayuda" -> "22_ayuda";
            case "evidencias" -> "04_libreta";default -> null;
        };
        boolean accionSocial=Set.of("compartir","verificar","reportar","ignorar").contains(action);
        if(accionSocial&&!juego.contraste) fondo=switch(action) {
            case "verificar" -> new Color(0x167964);case "reportar" -> new Color(0xAD413E);
            case "ignorar" -> oro;default -> TemaPixel.AZUL;
        };
        TemaPixel.marco(g,x,y+dy,w,42-dy,fondo,principal?papel:hover?oro:TemaPixel.CIAN,false);
        rect(x+7,y+5+dy,w-14,2,new Color(255,255,255,principal?95:35));
        g.setFont(tema.fuente(17,true));
        int espacio=icono==null?28:58;
        int size=18;while(size>17&&tema.ancho(g,t,tema.fuente(size,true))>w-espacio) size--;
        int tw=tema.ancho(g,t,tema.fuente(size,true));
        if(icono!=null) imagen("iconos/"+icono,x+10,y+9+dy,24,24);
        // Los controles muy estrechos conservan el área clicable y reducen solo el rótulo.
        Font f=tema.fuente(size,true);
        while(size>12&&tema.ancho(g,t,f)>w-espacio) f=tema.fuente(--size,true);
        tw=tema.ancho(g,t,f);
        boolean claro=accionSocial?action.equals("ignorar")&&!juego.contraste:principal;
        g.setColor(claro?TemaPixel.NOCHE:papel);
        tema.escribir(g,t,x+(icono==null?(w-tw)/2:40+(w-48-tw)/2),y+27+dy,f);
        botones.add(new Boton(new Rectangle(x,y,w,42),action,t));
    }
    void cabecera(String titulo,String detalle) {cabecera(titulo,detalle,true);}
    void cabecera(String titulo,String detalle,boolean volver) {
        rect(18,18,1244,96,new Color(tinta.getRed(),tinta.getGreen(),tinta.getBlue(),235));
        texto("ALCALDE DIGITAL",28,33,16,oro,true);texto(titulo,28,77,34,papel,true);
        texto(detalle,30,101,14,tenue,false);
        rect(28,114,1224,2,TemaPixel.CIAN);
        if(volver) boton("Volver [Esc]","volver",1080,35,170,false);
    }
    void pie(String t) {texto(t,30,696,13,tenue,false);}
    private void menu() {
        imagen("portada",462,0,1080,720);
        TemaPixel.marco(g,18,18,467,683,tinta,papel,true);
        imagen("iconos/20_reloj",47,44,24,24);
        texto("CIUDAD NOVA  /  DÍA 01",84,64,17,oro,true);
        texto("ALCALDE",44,150,72,papel,true);texto("DIGITAL",44,215,72,papel,true);
        rect(48,232,384,3,oro);
        texto("LA NOCHE DEL PALOMO",48,259,19,oro,true);
        parrafo("Un rumor, una memoria USB, un ejército de bots y un dron en el cielo. Seis capítulos para salvar las elecciones de Ciudad Nova.",48,296,390,17,papel);
        var camp=juego.campana;
        String historiaTexto=!camp.iniciada()?"Jugar la historia  →":camp.completada()?"Historia completada  ★":"Continuar la historia · cap. "+camp.actual().numero()+"  →";
        boton(historiaTexto,"historia",48,384,364,true);
        if(juego.sesion==null) boton("Caso extra: el helipuerto","jugar",48,438,364,false);
        else boton(juego.sesion.vista().terminado()?"Resultado del caso extra":"Continuar el caso extra",juego.sesion.vista().terminado()?"resultado":"volver",48,438,364,false);
        boton("Cómo jugar","ayuda",48,492,364,false);
        boton("Sonido: "+(juego.audio.actual().mudo()?"no":"sí"),"sonido",48,546,176,false);
        boton("Salir","salir",236,546,176,false);
        rect(48,606,364,2,TemaPixel.CIAN);
        texto("MISIÓN 5 → 4 MINIJUEGOS → MISIÓN 6",48,634,15,oro,true);
        texto("Entrega final  ·  Java + Processing",48,662,16,tenue,false);
        // Juego libre: cada parte también se puede jugar suelta.
        panel(867,232,378,360,new Color(24,40,61,225));
        texto("JUEGO LIBRE · 1 O 2 JUGADORES",888,262,14,oro,true);
        texto("Misión 5 · 70 segundos",888,292,13,tenue,false);
        boton(juego.mision5EnCurso()?"Continuar misión 5":"Misión 5 · La calle del pánico","mision5",875,302,362,false);
        texto("4 retos de estructuras de datos",888,370,13,tenue,false);
        boton(juego.minijuegoEnCurso()?"Continuar minijuego":"Minijuegos de la misión 5","minijuegos",875,380,362,false);
        boolean seis=camp.mision6Desbloqueada();
        texto(seis?"Misión 6 · helicópteros contra el dron":"Bloqueada: gana la misión 5",888,448,13,seis?tenue:ROJO,false);
        boton(juego.mision6EnCurso()?"Continuar misión 6":"Misión 6 · Cielos de campaña","mision6",875,458,362,false);
        if(!seis) imagen("iconos/21_candado",1200,467,24,24);
        if(juego.sesion!=null) boton("Laboratorio de estructuras","laboratorio",875,536,362,false);
        else if(juego.segundosAviso>0&&!juego.aviso.isBlank()) parrafo(juego.aviso,888,540,340,14,oro);
        else {texto("Caso extra: el primer capítulo, con",888,546,13,tenue,false);texto("el laboratorio de árboles AVL.",888,568,13,tenue,false);}
        panel(917,605,320,69,new Color(24,40,61,225));
        texto("Una ciudad. Muchas versiones.",938,634,17,papel,true);
        texto("Verifica · coopera · decide",938,657,14,oro,false);
    }
    private void roles() {
        cabecera("¿Qué voz tendrá tu historia?","Un caso local de la entrega 1. Los otros personajes te acompañan como NPC.");
        String[] nombres={"Ciudadano","Periodista","Influencer","Candidato"};
        String[] textos={"Escucha a los vecinos y encuentra las fuentes. Tu voz puede frenar una cadena.","Documenta la declaración completa. Investiga antes de convertirla en titular.","Transmite junto al candidato. Dale alcance al contexto, no solo al escándalo.","Responde por tu campaña. Reconocer lo que no sabes también es una decisión."};
        for(int i=0;i<4;i++) {
            int x=28+i*312;panel(x,146,288,322,new Color(0x22394D));
            imagen("portada",x+8,154,272,114);
            rect(x+8,154,272,114,new Color(0,25,45,155));
            imagen("actores/"+nombres[i].toLowerCase(Locale.ROOT)+"_00",x+96,165,96,96);
            rect(x+10,270,268,2,TemaPixel.CIAN);
            texto(nombres[i],x+22,290,24,papel,true);parrafo(textos[i],x+22,324,245,16,tenue);
            boton("Elegir "+nombres[i],"rol:"+Rol.values()[i],x+18,408,252,true);
        }
        boton("Contraste: "+(juego.contraste?"alto":"normal"),"contraste",28,503,284,false);
        boton("Movimiento: "+(juego.movimientoReducido?"reducido":"normal"),"movimiento",340,503,286,false);
        boton("Decisión: "+(juego.tiempoAmpliado?"60 segundos":"10 segundos"),"tiempo",654,503,284,false);
        boton("Cambiar semilla +","semilla",966,503,286,false);
        parrafo("WASD o flechas para caminar · E para interactuar · C abre Civitas · T muestra los árboles. El reloj se detiene al investigar; al agotarse, se aplica Ignorar.",30,592,1130,18,papel);
        pie("Tab y Enter recorren todos los botones · Semilla "+juego.semilla+" · La verdad se descubre jugando");
    }
    private void parque() {
        var s=juego.sesion.vista();
        texto("CIUDAD NOVA",28,30,14,oro,true);texto("Parque de las Palomas Influencers",28,65,26,papel,true);
        texto("DÍA 01  /  "+s.rol()+"  /  "+s.puntos()+" puntos",28,90,14,tenue,false);
        boton("Civitas [C]","civitas",637,35,157,true);boton("Árboles [T]","arboles",805,35,157,false);
        boton("Ayuda [H]","ayuda",974,35,128,false);boton("Menú","menu",1114,35,138,false);
        mundo(24,114,866,484);
        panel(912,114,344,484,new Color(0x22394D));
        texto("TU SIGUIENTE PASO",936,148,13,oro,true);
        parrafo(s.objetivo(),936,180,291,20,papel);
        texto("EVIDENCIAS  "+s.evidencias().size()+" / 2",936,265,14,tenue,true);
        boton("Archivo de evidencias [I]","evidencias",932,281,303,false);
        texto(s.verificando()?"Investigación sin cuenta atrás":"Decisión: "+(int)Math.ceil(juego.sesion.restante())+" s",936,357,16,oro,true);
        texto("Al agotarse: Ignorar",936,381,13,tenue,false);
        int[] vals={s.ciudad().informacionVerificada(),s.ciudad().confianza(),s.ciudad().desinformacion()};
        String[] labels={"Información verificada","Confianza ciudadana","Desinformación"};
        for(int i=0;i<3;i++) barra(labels[i],vals[i],936,413+i*49,292,i==2);
        panel(24,608,1232,72,new Color(0x294357));
        parrafo(avisoVisible()?juego.aviso+" [T] Consultar":s.mensaje(),42,631,1185,14,papel);
        pie("WASD / flechas: explorar    E / Espacio: interactuar    C: Civitas    I: pruebas    T: árboles    H: ayuda");
    }
    void barra(String nombre,int valor,int x,int y,int w,boolean peligro) {
        texto(nombre,x,y,13,papel,false);texto(""+valor,x+w-26,y,13,oro,true);
        rect(x,y+8,w,6,new Color(0x405461));rect(x,y+8,Math.max(1,w*valor/100),6,peligro?new Color(0xE97866):new Color(0x6ABCAA));
    }
    private void mundo(int vx,int vy,int vw,int vh) {
        var s=juego.sesion.vista();
        int camX=Math.max(0,Math.min(640-vw/2,Math.round(s.x())-vw/4));
        int camY=Math.max(0,Math.min(480-vh/2,Math.round(s.y())-vh/4));
        Graphics2D guard=g;g=(Graphics2D)g.create();g.clipRect(vx,vy,vw,vh);g.translate(vx-camX*2,vy-camY*2);g.scale(2,2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        String[] tiles={"cesped","camino","agua"};
        for(int row=0;row<MapaParque.FILAS;row++) for(int col=0;col<MapaParque.COLUMNAS;col++)
            imagen("tiles/"+tiles[juego.sesion.mapa().suelo(col,row)],col*16,row*16,16,16);
        rect(419,114,82,42,new Color(0x976646));rect(419,114,82,3,oro);
        for(int i=0;i<6;i++) { g.setColor(i%2==0?oro:teal);g.fillPolygon(new int[]{422+i*13,435+i*13,428+i*13},new int[]{109,109,119},3); }
        // El orden por coordenada de pies permite pasar detrás de árboles y delante de bancos.
        record Entidad(int y,Runnable dibujar) { }
        List<Entidad> orden=new ArrayList<>();
        for(var o:juego.sesion.mapa().objetos()) orden.add(new Entidad(o.y(),()->imagen("objetos/"+o.tipo(),o.x()-o.ancho()/2,o.y()-o.alto(),o.ancho(),o.alto())));
        orden.add(new Entidad(228,()->sprite("periodista",0,230,228)));
        orden.add(new Entidad(155,()->sprite("candidato",0,455,155)));
        orden.add(new Entidad(267,()->sprite("influencer",0,474,267)));
        orden.add(new Entidad(315,()->imagen("actores/noti_%02d".formatted(juego.movimientoReducido?0:(int)(juego.animacion*6)%4),276,287,32,32)));
        orden.add(new Entidad((int)s.y(),()->{
            g.setColor(new Color(20,35,40,70));g.fillOval((int)s.x()-10,(int)s.y()-4,20,7);
            if(juego.efecto>0&&juego.ultimaAccion.equals("rol")&&!juego.movimientoReducido)
                imagen("acciones/frame_%02d".formatted(s.rol().ordinal()*4+Math.min(3,(int)((.8-juego.efecto)*5))),(int)s.x()-24,(int)s.y()-48,48,48);
            else sprite(s.rol().name().toLowerCase(Locale.ROOT),juego.direccion*4+juego.cuadro,(int)s.x(),(int)s.y());
            if(juego.efecto>0&&!juego.movimientoReducido) imagen("efectos/"+(juego.ultimaAccion.equals("compartir")?"rumor":"verificacion")+"_%02d".formatted(Math.min(7,(int)((.8-juego.efecto)*10))), (int)s.x()-24,(int)s.y()-48,48,48);
        }));
        orden.sort(Comparator.comparingInt(Entidad::y));orden.forEach(e->e.dibujar().run());
        if(!fondoMundo) {
        marcador("1 · Aviso municipal",176,101,s.evidencias().size()>0);
        marcador("2 · Administradora",230,184,s.evidencias().size()>1);
        marcador("Campaña",455,103,false);
        if(juego.sesion.cerca("aviso")||juego.sesion.cerca("fuente")||juego.sesion.cerca("candidato")) {
            panel((int)s.x()-34,(int)s.y()-51,68,15,tinta);texto("[E] Hablar",(int)s.x()-29,(int)s.y()-40,9,papel,true);
        }
        }
        g.dispose();g=guard;
        if(fondoMundo) return;
        // Minimap basado en el mismo tilemap.
        panel(vx+12,vy+12,130,101,new Color(24,40,61,220));
        for(int row=0;row<30;row++) for(int col=0;col<40;col++) {
            int t=juego.sesion.mapa().suelo(col,row);rect(vx+17+col*3,vy+17+row*3,3,3,t==0?new Color(0x478F62):t==1?papel:teal);
        }
        rect(vx+17+(int)s.x()*3/16,vy+17+(int)s.y()*3/16,5,5,oro);
    }
    void sprite(String rol,int frame,int x,int y) {imagen("actores/"+rol+"_%02d".formatted(frame),x-16,y-32,32,32);}
    void marcador(String label,int x,int y,boolean listo) {
        g.setFont(tema.fuente(8,true));int w=g.getFontMetrics().stringWidth(label)+12;
        panel(x-w/2,y-10,w,14,tinta);texto((listo?"✓ ":"")+label,x-w/2+4,y,8,listo?new Color(0x8FD7B6):papel,true);
    }
    private void civitas() {
        var s=juego.sesion.vista();cabecera("Civitas / lo que está circulando","Popular no significa comprobado. La prioridad expresa urgencia, no veracidad.");
        panel(28,133,727,499,papel);
        TemaPixel.marco(g,49,151,49,49,teal,teal,false);
        imagen("actores/influencer_00",54,154,40,40);
        texto("KEVIN VIRAL",112,172,20,teal,true);
        texto("Hace un momento · Parque de las Palomas",112,196,14,tinta,false);
        imagen("portada",568,215,162,108);
        imagen("iconos/09_reportar",627,248,40,40);
        parrafo("¿Cerrarán el parque para construir un helipuerto de palomas?",54,243,494,28,tinta);
        parrafo("«Mi primo tiene una paloma que trabaja en la alcaldía». Compártelo antes de que lo borren.",54,350,640,18,tinta);
        panel(50,395,680,44,new Color(0xEAD7AF));texto("ESTADO: "+(s.evidencias().size()==2?"FUENTES CONTRASTADAS":"SIN COMPROBAR"),67,424,14,tinta,true);
        boton("Compartir","compartir",53,464,157,false);boton("Verificar","verificar",226,464,157,true);
        boton("Reportar","reportar",399,464,157,false);boton("Ignorar","ignorar",572,464,157,false);
        if(s.evidencias().size()==2) {
            boton("Publicar contexto","contexto",53,527,213,true);boton("Guardar pruebas","guardar",283,527,210,false);
            if(juego.sesion.buscarDecision("rectificar")!=null) boton("Rectificar","rectificar",510,527,219,true);
        } else texto("Publicar contexto requiere el aviso + el testimonio.",54,550,15,teal,true);
        panel(779,133,475,499,new Color(0x22394D));texto("RADAR DE PUBLICACIONES",803,169,15,oro,true);
        int y=207;
        for(Publicacion p:s.publicaciones()) {
            texto("#"+p.id()+"   PRIORIDAD "+p.riesgo(),803,y,13,oro,true);
            y=parrafo(p.texto(),803,y+28,414,17,papel)+12;
            texto(p.estado(),803,y,13,tenue,false);y+=47;
        }
        boton("Ver árbol de prioridad","arboles",803,552,425,false);
        parrafo(avisoVisible()?juego.aviso+" [T] Consultar":s.mensaje(),30,652,1210,14,oro);
        pie(s.verificando()?"Investigación activa · vuelve al parque para conseguir pruebas":"Quedan "+(int)Math.ceil(juego.sesion.restante())+" s · al agotarse se aplica Ignorar");
    }
    private void evidencias() {
        var s=juego.sesion.vista();cabecera("Archivo de evidencias","Lee la procedencia y contrasta. Una captura aislada puede perder su contexto.");
        for(int i=0;i<2;i++) {
            int x=28+i*624;panel(x,149,599,286,new Color(0x22394D));
            imagen("iconos/"+(i==0?"06_fuente":"04_libreta"),x+23,170,48,48);
            texto(i==0?"01 / Aviso municipal":"02 / Testimonio directo",x+87,202,21,papel,true);
            parrafo(s.evidencias().size()>i?s.evidencias().get(i):"Pendiente. Explora el parque y pulsa E cerca de esta fuente.",x+25,258,544,20,papel);
            texto(s.evidencias().size()>i?"Procedencia registrada · acceso público":"Fuente todavía no consultada",x+25,402,14,oro,false);
        }
        boton("Volver al parque","volver",28,472,286,true);boton("Decidir en Civitas","civitas",338,472,286,false);
        parrafo("Tu misión: leer el aviso, escuchar a la administradora y decidir qué hacer con el mensaje. La opción Publicar contexto aparece en el árbol cuando reúnes ambas fuentes.",30,577,1134,21,papel);
        pie("Las evidencias se conservan durante todo el caso. No se descartan por accidente.");
    }
    private void arboles() {
        boolean radar=juego.pestana==JuegoCliente.Pestana.RADAR;
        cabecera(juego.laboratorio?"Laboratorio de estructuras":"Lo que cambia con tus decisiones",
            juego.laboratorio?"Sustentación · una estructura y un estado a la vez · consulta sin alterar la partida.":"Estado actual de tu caso · consulta opcional · el reloj local queda pausado.");
        boton(juego.laboratorio?"AVL · Publicaciones":"Radar de publicaciones","radar",28,127,304,radar);
        boton(juego.laboratorio?"N-ario · Decisiones":"Camino de decisiones","camino",348,127,304,!radar);
        boton(juego.laboratorio?"Volver a vista de jugador":"Abrir Laboratorio",juego.laboratorio?"jugador":"laboratorio",900,127,352,false);
        if(juego.laboratorio) laboratorio(radar);else vistaJugador(radar);
    }
    private boolean avisoVisible() {return juego.avisos&&juego.segundosAviso>0&&!juego.aviso.isBlank();}
    private void vistaJugador(boolean radar) {
        var s=juego.sesion.vista();
        panel(28,190,766,483,new Color(0x22394D));panel(814,190,438,483,new Color(0x22394D));
        texto(radar?"Qué merece tu atención":"Tu camino en el rumor del parque",50,224,22,papel,true);
        if(radar) {
            int y=245;
            for(var p:s.publicaciones()) {
                panel(48,y,726,118,papel);
                imagen("portada",60,y+12,120,92);
                rect(64,y+60,34,34,TemaPixel.NOCHE);
                imagen("iconos/"+(p.id()==4?"12_contexto":p.id()==3?"20_reloj":"01_celular"),65,y+61,32,32);
                texto("#"+p.id()+" · "+p.autor(),195,y+29,18,tinta,true);
                rect(622,y+12,135,24,oro);
                texto("Prioridad "+p.riesgo(),631,y+30,16,TemaPixel.NOCHE,true);
                parrafo(p.texto(),195,y+57,558,17,tinta);
                texto(p.estado(),195,y+103,15,teal,false);y+=130;
            }
            texto("Más prioridad = más atención. No indica si algo es verdad.",50,656,14,tenue,false);
        } else {
            texto("Las acciones se eligen en Civitas. Aquí puedes consultar su estado.",50,251,14,tenue,false);
            int y=caminoJugador(s.decisiones(),0,287,s.terminado());
            if(!s.terminado()&&ConsultaEstructuras.buscar(s.decisiones(),"contexto")==null)
                parrafo("Pendiente: reúne el aviso y el testimonio para descubrir cómo aportar contexto.",51,Math.max(564,y+12),700,16,oro);
            texto(s.terminado()?"Caso cerrado: se conserva el camino que tomaste.":"Consultar una opción no la ejecuta ni cambia tus puntos.",50,656,14,tenue,false);
        }
        var evento=juego.consulta.ultimoCambio(radar);
        texto("QUÉ CAMBIÓ Y POR QUÉ",838,225,14,oro,true);
        if(evento!=null) {
            int y=parrafo(evento.causa(),838,259,388,20,papel);
            y=parrafo(radar?evento.radar():evento.camino(),838,y+14,388,18,papel);
            texto("Cambio registrado #"+evento.numero(),838,y+8,13,tenue,false);
        }
        texto("TU SIGUIENTE PASO",838,476,13,oro,true);
        parrafo(s.objetivo(),838,506,380,19,papel);
        boton(s.terminado()?"Consultar resultado":"Ir a Civitas",s.terminado()?"resultado":"civitas",837,572,392,true);
        texto("T / Esc: volver al punto de entrada",838,647,14,tenue,false);
        pie("Las dos pestañas consultan el estado actual. Los detalles técnicos y estados anteriores están en el Laboratorio.");
    }
    private int caminoJugador(ArbolDecisiones.Vista n,int nivel,int y,boolean terminado) {
        int x=52+nivel*32;
        if(nivel>0) {g.setColor(tenue);g.drawLine(x-15,y-24,x-15,y);g.drawLine(x-15,y,x-3,y);}
        panel(x,y-21,700-nivel*32,36,n.ejecutado()?teal:new Color(0x294357));
        texto((n.ejecutado()?"✓ ":"")+n.texto(),x+12,y+3,16,papel,true);
        texto(n.ejecutado()?"Elegida":nivel==0?"Tu caso":terminado?"Caso cerrado":"Disponible",635,y+3,13,papel,false);
        y+=43;
        for(var hijo:n.hijos()) y=caminoJugador(hijo,nivel+1,y,terminado);
        return y;
    }
    private void laboratorio(boolean radar) {
        int indice=radar?juego.indiceAVL():juego.indiceDecision();
        int cantidad=radar?juego.sesion.pasosAVL().size():juego.sesion.pasosDecisiones().size();
        boolean actual=(radar?juego.pasoAVL:juego.pasoDecision)<0;
        String orden=radar?new String[]{"inorden","preorden","postorden","inverso"}[juego.recorrido]:new String[]{"preorden","postorden","niveles"}[juego.recorridoNario];
        panel(28,190,788,316,new Color(0x22394D));panel(836,190,416,316,new Color(0x22394D));
        texto((actual?"ACTUAL":"HISTÓRICO")+" · Estado "+(indice+1)+" / "+cantidad,48,217,14,oro,true);
        var evento=juego.consulta.eventoDelPaso(radar,indice);
        int y=parrafo(evento==null?"Operación de la sesión":evento.titulo(),858,223,370,17,papel);
        if(evento!=null) texto("Día 1 · Parque · versión "+evento.version(),858,y+3,13,tenue,false);
        String operacion=radar?juego.sesion.pasosAVL().get(indice).operacion():juego.sesion.pasosDecisiones().get(indice).operacion();
        y=parrafo(operacion,858,y+36,368,16,oro);
        if(evento!=null) {
            texto("Al completar esta acción:",858,y+10,12,tenue,false);
            parrafo(radar?evento.radar():evento.camino(),858,y+32,368,14,papel);
        }
        texto("Cada pestaña conserva su propio estado.",858,477,12,tenue,false);
        List<String> ids;
        if(radar) {
            var recorrido=ConsultaEstructuras.publicaciones(juego.vistaAVL(),orden);
            long marcado=recorrido.isEmpty()?-1:recorrido.get(Math.floorMod(juego.visita,recorrido.size())).id();
            dibujarAVL(juego.vistaAVL(),422,263,182,0,marcado);
            ids=recorrido.stream().map(p->"#"+p.id()+" ("+p.riesgo()+")").toList();
            texto("Clave (riesgo, ID) · h = altura · FE = derecha − izquierda",48,489,13,tenue,false);
        } else {
            ids=ConsultaEstructuras.decisiones(juego.vistaDecision(),orden);
            dibujarNario(juego.vistaDecision(),44,802,258,0);
            texto("✓ ruta ejecutada · borde dorado: nodo del recorrido · selecciona un nodo para consultarlo",48,489,12,tenue,false);
        }
        boton("← Anterior",radar?"avlAnterior":"narAnterior",28,520,143,false);
        boton("Siguiente →",radar?"avlSiguiente":"narSiguiente",183,520,146,false);
        boton("Estado actual","actual",341,520,157,false);
        boton("Orden: "+orden,radar?"recorrido":"recorridoNario",510,520,228,true);
        boton("Siguiente nodo","visitar",750,520,191,false);
        boton(juego.automatico?"Detener animación":"Animar nodos","automatico",953,520,299,false);
        texto("Nodos del estado mostrado · "+orden+" · "+(ids.isEmpty()?"sin nodos":"visita "+(Math.floorMod(juego.visita,ids.size())+1)+" / "+ids.size()),30,587,14,oro,true);
        texto(String.join(" → ",ids),30,614,14,papel,false);
        if(radar) {
            String campo=juego.busqueda;
            if(juego.editandoBusqueda) campo=campo.substring(0,juego.cursorBusqueda)+"│"+campo.substring(juego.cursorBusqueda);
            boton("ID: "+(campo.isEmpty()?"escribir aquí":campo),"campoBusqueda",28,637,292,false);
            boton("Buscar ID","buscar",336,637,146,false);
            texto(juego.encontrado,504,665,14,oro,false);
        } else texto(juego.encontrado.isBlank()?"Selecciona un nodo: la consulta usa este estado, incluso si la opción ya fue podada.":juego.encontrado,30,663,16,oro,false);
        pie(juego.movimientoReducido?"Movimiento reducido: usa Siguiente nodo. T / Esc / Volver regresan al origen.":"Animar nodos recorre este estado; Anterior/Siguiente cambian operaciones. T / Esc / Volver regresan al origen.");
    }
    private void dibujarAVL(ArbolAVLPublicaciones.Vista n,int x,int y,int distancia,int nivel,long resaltado) {
        if(n==null) return;
        g.setStroke(new BasicStroke(2));g.setColor(new Color(0x69818B));
        if(n.izquierdo()!=null) g.drawLine(x,y,x-distancia,y+76);
        if(n.derecho()!=null) g.drawLine(x,y,x+distancia,y+76);
        dibujarAVL(n.izquierdo(),x-distancia,y+76,distancia/2,nivel+1,resaltado);
        dibujarAVL(n.derecho(),x+distancia,y+76,distancia/2,nivel+1,resaltado);
        TemaPixel.marco(g,x-26,y-24,52,48,n.valor().id()==resaltado?oro:teal,papel,false);
        texto(""+n.valor().riesgo(),x-13,y+6,20,n.valor().id()==resaltado?tinta:papel,true);
        texto("#"+n.valor().id()+" h"+n.altura()+" FE"+n.fe(),x-44,y+43,13,papel,false);
    }
    private void dibujarNario(ArbolDecisiones.Vista n,int left,int right,int y,int depth) {
        int x=(left+right)/2, count=n.hijos().size(),total=hojas(n),offset=0;
        if(count>0) for(int i=0;i<count;i++) {
            int peso=hojas(n.hijos().get(i));
            int l=left+(right-left)*offset/total,r=left+(right-left)*(offset+peso)/total;
            offset+=peso;
            g.setColor(new Color(0x69818B));g.drawLine(x,y,(l+r)/2,y+82);
            dibujarNario(n.hijos().get(i),l,r,y+82,depth+1);
        }
        panel(x-55,y-17,110,35,n.ejecutado()?teal:new Color(0x405461));
        texto((n.ejecutado()?"✓ ":"")+n.id(),x-49,y+5,13,papel,true);
        var ruta=ConsultaEstructuras.decisiones(juego.vistaDecision(),new String[]{"preorden","postorden","niveles"}[juego.recorridoNario]);
        if(!ruta.isEmpty()&&n.id().equals(ruta.get(Math.floorMod(juego.visita,ruta.size())))) {
            g.setColor(oro);g.setStroke(new BasicStroke(2));g.drawRect(x-57,y-19,114,39);
        }
        botones.add(new Boton(new Rectangle(x-55,y-17,110,35),"nodo:"+n.id(),n.id()));
    }
    private int hojas(ArbolDecisiones.Vista n) { return n.hijos().isEmpty()?1:n.hijos().stream().mapToInt(this::hojas).sum(); }
    private void resultado() {
        var s=juego.sesion.vista();cabecera("El parque después de tu decisión","Cierre del capítulo 1. La elección y los finales de siete días pertenecen a la entrega final.");
        panel(28,139,715,478,papel);texto(s.puntos()>=20?"CONTEXTO, BENDITO CONTEXTO":"CADA DECISIÓN DEJA HUELLA",54,179,22,tinta,true);
        parrafo(s.mensaje(),54,221,653,22,tinta);
        texto("TU RECORRIDO",54,339,13,teal,true);int y=368;
        List<String> h=s.historial();
        for(String linea:h.subList(Math.max(0,h.size()-6),h.size())) y=parrafo("• "+linea,54,y,650,14,tinta)+5;
        panel(766,139,487,478,new Color(0x22394D));texto(s.puntos()+" PUNTOS",793,180,29,oro,true);
        EstadoCiudad c=s.ciudad();String[] names={"Información verificada","Confianza","Convivencia","Bienestar digital","Participación","Desinformación","Conflictos"};
        int[] vals={c.informacionVerificada(),c.confianza(),c.convivencia(),c.bienestar(),c.participacion(),c.desinformacion(),c.conflictos()};
        for(int i=0;i<7;i++) {
            int delta=vals[i]-(i>4?20:50);
            barra(names[i]+"  ("+(delta>=0?"+":"")+delta+")",vals[i],793,224+i*51,430,i>4);
        }
        boton("Otra historia","jugar",28,639,226,true);boton("Revisar los árboles","arboles",273,639,252,false);
        boton("Evidencias","evidencias",545,639,198,false);boton("Laboratorio","laboratorio",766,639,230,false);boton("Menú principal","menu",1012,639,241,false);
        if(avisoVisible()) {panel(45,516,682,93,new Color(0xEAD7AF));parrafo(juego.aviso+" [T] Consultar",58,538,654,13,tinta);}
        pie("El historial conserva tus acciones incluso cuando una publicación sale del AVL.");
    }
    private void ayuda() {
        cabecera("Cómo jugar · La noche del Palomo","El modo historia une la misión 5, cuatro minijuegos y la misión 6. Esta ayuda pausa el juego.");
        String[] titulos={"01 · Tu objetivo","02 · Cómo te mueves","03 · Qué hacen tus acciones","04 · Los personajes","05 · Ganar y perder","06 · Accesibilidad"};
        String[] body={"Frena el plan del Palomo en seis capítulos: misión 5, Corta la cadena, Rastrea la fuente, Caza de bots, Duelo contra el Palomo y misión 6. Ganar uno abre el siguiente.",
            "Solo: WASD o flechas y E, Espacio o Enter. En pareja: J1 WASD + E, J2 flechas + Enter. Misión 6: F dispara, Q misil, Espacio impulso (J2: L, K, Mayús). Esc pausa.",
            "Verificar desmiente noticias. Calmar baja el pánico. Blindar corta un rumor, preguntar sube al padre, reportar caza bots y cargar un dato rompe chismes. Cada acción usa una estructura.",
            "Ciudadano y Periodista enfrentan al Palomo en la calle y en Civitas. En la misión 6, Juan (el candidato) y la influencer vuelan juntos. El caso extra conserva los cuatro roles.",
            "Pierdes si el pánico o la desinformación llegan a 100, si se acaba el tiempo o la vida. Reintenta cuanto quieras: la historia guarda tu mejor puntaje y cuenta los reintentos.",
            "Activa alto contraste o movimiento reducido. Toda información esencial es visual y textual, con formas además del color. El sonido se activa o silencia desde el menú."};
        String[] iconos={"06_fuente","19_mapa","07_compartir","01_celular","13_confianza","24_accesibilidad"};
        for(int i=0;i<6;i++) {
            int x=28+(i%2)*624,y=135+(i/2)*163;
            panel(x,y,596,153,new Color(0x22394D));
            TemaPixel.marco(g,x+14,y+19,88,113,TemaPixel.NOCHE,TemaPixel.CIAN,false);
            if(i==3) imagen("actores/ciudadano_00",x+26,y+37,64,64);
            else imagen("iconos/"+iconos[i],x+26,y+40,64,64);
            texto(titulos[i],x+118,y+29,20,oro,true);
            parrafo(body[i],x+118,y+56,461,16,papel);
        }
        boton("Contraste: "+(juego.contraste?"alto":"normal"),"contraste",28,639,288,false);
        boton("Animaciones: "+(juego.movimientoReducido?"reducidas":"activas"),"movimiento",338,639,302,false);
        boton(juego.avisos?"Avisos: visibles":"Avisos: solo al consultar","avisos",660,639,286,false);
        if(juego.sesion!=null) boton("Laboratorio","laboratorio",964,639,289,true);
        else boton("Volver","volver",964,639,289,true);
        pie("Caso extra «El helipuerto de palomas»: C abre Civitas, I las pruebas y T el laboratorio de árboles AVL.");
    }

    // ═════════════════════════ Misión 5 · La calle del pánico ═════════════════════════
    private final Map<String,BufferedImage> tintes=new HashMap<>();
    private float palomoAnteriorX=216;
    private boolean palomoMiraIzquierda;
    static final Color ROJO=new Color(0xE97866), ROJO_FUERTE=new Color(0xAD413E);

    /** Transeúntes: candidato/influencer con la ropa en otro tono; la piel conserva su color. */
    BufferedImage vecinoImagen(int variante,int cuadro) {
        String rol=variante%2==0?"candidato":"influencer";float giro=(variante/2)*.15f;
        String nombre="actores/"+rol+"_%02d".formatted(cuadro);
        if(giro==0) return recursos.imagen(nombre);
        return tintes.computeIfAbsent(nombre+"@"+variante,k->{
            BufferedImage base=recursos.imagen(nombre),nueva=new BufferedImage(base.getWidth(),base.getHeight(),BufferedImage.TYPE_INT_ARGB);
            float[] hsb=new float[3];
            for(int y=0;y<base.getHeight();y++) for(int x=0;x<base.getWidth();x++) {
                int argb=base.getRGB(x,y);int a=argb>>>24;
                if(a==0) {nueva.setRGB(x,y,argb);continue;}
                Color.RGBtoHSB((argb>>16)&255,(argb>>8)&255,argb&255,hsb);
                if(hsb[1]>=.28f&&hsb[2]>.22f&&hsb[0]>=.13f&&hsb[0]<=.96f)
                    argb=(a<<24)|(Color.HSBtoRGB((hsb[0]+giro)%1f,hsb[1],hsb[2])&0xFFFFFF);
                nueva.setRGB(x,y,argb);
            }
            return nueva;
        });
    }
    String ajustar(String t,int w,int size,boolean bold) {
        Font f=tema.fuente(size,bold);if(tema.ancho(g,t,f)<=w) return t;
        while(t.length()>1&&tema.ancho(g,t+"…",f)>w) t=t.substring(0,t.length()-1);
        return t.strip()+"…";
    }
    List<String> envolver(String t,int w,int size) {
        List<String> lineas=new ArrayList<>();String linea="";Font f=tema.fuente(size,false);
        for(String palabra:t.split(" ")) {
            if(!linea.isEmpty()&&tema.ancho(g,linea+" "+palabra,f)>w) {lineas.add(linea);linea=palabra;}
            else linea=linea.isEmpty()?palabra:linea+" "+palabra;
        }
        if(!linea.isEmpty()) lineas.add(linea);
        return lineas;
    }
    /** Globo de diálogo en coordenadas de pantalla. */
    void globo(String nombre,String texto,int cx,int cima,int w,Color relleno,Color borde,Color tintaTexto,int limiteSuperior,int limiteIzq,int limiteDer) {
        List<String> lineas=envolver(texto,w-24,14);
        int h=18+lineas.size()*20+(nombre==null?0:20);
        int x=Math.max(limiteIzq+4,Math.min(limiteDer-w-4,cx-w/2)),y=Math.max(limiteSuperior+4,cima-h);
        TemaPixel.marco(g,x,y,w,h,relleno,borde,false);
        int ty=y+(nombre==null?24:22);
        if(nombre!=null) {texto(nombre,x+12,ty,12,borde==papel?tinta:borde,true);ty+=22;}
        g.setColor(tintaTexto);
        for(String l:lineas) {tema.escribir(g,l,x+12,ty,tema.fuente(14,false));ty+=20;}
        int tx=Math.max(x+14,Math.min(x+w-14,cx));
        if(y+h<cima+2) {g.setColor(relleno);g.fillPolygon(new int[]{tx-6,tx+6,tx},new int[]{y+h-1,y+h-1,y+h+8},3);}
    }
    void sombra5(float x,float y,int w) {g.setColor(new Color(20,35,40,70));g.fillOval((int)x-w/2,(int)y-4,w,7);}

    /** Escena de la calle frente al Centro de Periodismo: un solo plano de 432×242 a escala 2. */
    private void escena5(int vx,int vy,int vw,int vh,int modo) {
        var m=juego.mision5;boolean reducido=juego.movimientoReducido;double t=juego.animacion;
        Graphics2D guard=g;g=(Graphics2D)g.create();g.clipRect(vx,vy,vw,vh);g.translate(vx,vy);g.scale(2,2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        for(int row=0;row<16;row++) for(int col=0;col<27;col++) imagen("tiles/"+(row>=14?"cesped":"camino"),col*16,row*16,16,16);
        // Calzada con línea discontinua y cebra; los vecinos caminan por toda la calle.
        rect(0,118,432,76,new Color(0x4C5964));rect(0,118,432,2,new Color(0xFFF0CE,false));rect(0,192,432,2,papel);
        for(int x=6;x<432;x+=40) rect(x,154,22,2,papel);
        for(int i=0;i<7;i++) rect(268+i*8,122,5,68,new Color(255,240,206,170));
        // Fachada del Centro de Periodismo.
        rect(20,0,392,64,new Color(0x8A5B3C));rect(20,0,392,6,new Color(0x5A3A28));rect(20,58,392,6,new Color(0x5A3A28));
        for(int i=0;i<4;i++) for(int lado=0;lado<2;lado++) {
            int wx=lado==0?36+i*40:260+i*36;
            rect(wx,18,24,30,new Color(0x3A2A20));rect(wx+2,20,20,26,new Color(0x54BDCF));rect(wx+2,20,20,6,new Color(0xA8E6EE));rect(wx+11,20,2,26,new Color(0x3A2A20));
        }
        rect(194,24,44,40,TemaPixel.ORO);rect(197,27,38,37,TemaPixel.AZUL);rect(214,27,3,37,TemaPixel.ORO);
        rect(118,6,196,16,TemaPixel.NOCHE);rect(120,8,192,12,TemaPixel.ORO);
        g.setFont(tema.fuente(8,true));g.setColor(TemaPixel.NOCHE);tema.escribir(g,"CENTRO DE PERIODISMO",216-tema.ancho(g,"CENTRO DE PERIODISMO",tema.fuente(8,true))/2,17,tema.fuente(8,true));
        // Entidades ordenadas por la coordenada de los pies.
        record Entidad(float y,Runnable dibujar) { }
        List<Entidad> orden=new ArrayList<>();
        for(int x:new int[]{24,104,192,256,332,408}) orden.add(new Entidad(242,()->imagen("objetos/"+(x%3==0?"pino":"arbol"),x-20,190,40,52)));
        for(int i=0;i<Mision5.LAMPARAS_X.length;i++) {
            int lx=Mision5.LAMPARAS_X[i];
            orden.add(new Entidad(88,()->{
                imagen("objetos/lampara",lx-9,46,18,42);
                if(m.lamparasApagadas()>0) rect(lx-9,46,18,42,new Color(3,21,34,170));
            }));
        }
        orden.add(new Entidad(88,()->imagen("objetos/banco",262,66,38,22)));
        orden.add(new Entidad(88,()->imagen("objetos/aviso",388,46,38,42)));
        orden.add(new Entidad(88,()->imagen("objetos/flores",140,66,26,22)));
        orden.add(new Entidad(88,()->imagen("objetos/flores",300,66,26,22)));
        var zona=m.zona();
        for(var v:m.vecinos()) orden.add(new Entidad(v.y(),()->vecino5(v,modo)));
        orden.add(new Entidad(Mision5.PERIODISTA_Y,()->{
            sombra5(Mision5.PERIODISTA_X,Mision5.PERIODISTA_Y,20);
            if(m.verificando()&&!reducido) imagen("acciones/frame_%02d".formatted(4+(int)(t*6)%4),(int)Mision5.PERIODISTA_X-24,(int)Mision5.PERIODISTA_Y-48,48,48);
            else sprite("periodista",0,(int)Mision5.PERIODISTA_X,(int)Mision5.PERIODISTA_Y);
            if(modo==0||modo==1) marcador("Periodista",(int)Mision5.PERIODISTA_X,(int)Mision5.PERIODISTA_Y-34,m.verificando());
        }));
        var jug=m.jugador();
        orden.add(new Entidad(modo==1?106:jug.y(),()->{
            float jx=modo==1?182:jug.x(),jy=modo==1?106:jug.y();int dir=modo==1?1:jug.direccion();
            sombra5(jx,jy,20);
            if(jug.hablando()&&!reducido) imagen("acciones/frame_%02d".formatted((int)(t*6)%4),(int)jx-24,(int)jy-48,48,48);
            else sprite("ciudadano",dir*4+(jug.caminando()&&!reducido?(int)(t*8)%4:0),(int)jx,(int)jy);
            if(modo==0) {
                g.setColor(oro);g.fillPolygon(new int[]{(int)jx-4,(int)jx+4,(int)jx},new int[]{(int)jy-42,(int)jy-42,(int)jy-36},3);
                if(jug.hablando()) {rect((int)jx-12,(int)jy+3,24,3,new Color(0x405461));rect((int)jx-12,(int)jy+3,(int)(24*jug.avanceHabla()),3,new Color(0x6ABCAA));}
                else if(m.vecinoCercano()>=0&&m.estado()==Mision5.Estado.EN_CURSO) {panel((int)jx-34,(int)jy-52,68,15,tinta);texto("[E] Calmar",(int)jx-29,(int)jy-41,9,papel,true);}
            }
        }));
        var palomo=m.palomo();
        boolean fugaVictoria=modo==2;
        float palomoX=palomo.x(),palomoY=palomo.y();
        if(fugaVictoria) {palomoX+=(float)(juego.tiempoFin5*60);palomoY-=(float)(juego.tiempoFin5*40);}
        else if(modo==1) {palomoX=216;palomoY=48;}
        if(Math.abs(palomoX-palomoAnteriorX)>.05) palomoMiraIzquierda=palomoX<palomoAnteriorX;
        palomoAnteriorX=palomoX;
        final float pxF=palomoX,pyF=palomoY;
        boolean palomoVisible=modo!=1||juego.pagina5>=2;
        if(palomoVisible) orden.add(new Entidad(999,()->{
            sombra5(pxF,Math.min(Mision5.Y_MAX,pyF+56),30);
            int frame=reducido?8:8+(int)(t*8)%4;
            BufferedImage im=recursos.imagen("actores/noti_%02d".formatted(frame));
            if(palomoMiraIzquierda) g.drawImage(im,(int)pxF+32,(int)pyF-32,(int)pxF-32,(int)pyF+32,0,0,32,32,null);
            else g.drawImage(im,(int)pxF-32,(int)pyF-32,64,64,null);
        }));
        // Suelo de la zona sabotada, siempre debajo de los personajes.
        if(zona!=null) {
            g.setColor(new Color(20,0,40,100));g.fillOval((int)(zona.x()-zona.radio()),(int)(zona.y()-zona.radio()*.6),(int)(zona.radio()*2),(int)(zona.radio()*1.2));
            g.setColor(new Color(0xAD413E));g.setStroke(new BasicStroke(1));g.drawOval((int)(zona.x()-zona.radio()),(int)(zona.y()-zona.radio()*.6),(int)(zona.radio()*2),(int)(zona.radio()*1.2));
            if(!reducido) imagen("efectos/rumor_%02d".formatted((int)(t*8)%8),(int)zona.x()-32,(int)zona.y()-48,64,64);
        }
        orden.sort(Comparator.comparingDouble(Entidad::y));orden.forEach(e->e.dibujar().run());
        if(!reducido) for(var e:m.efectos()) imagen("efectos/"+e.tipo()+"_%02d".formatted(Math.min(7,(int)(e.edad()*10))),(int)e.x()-24,(int)e.y()-24,48,48);
        if(modo==2||modo==3) {
            // Pista que deja el Palomo: cae con la fuga y se queda en la calle.
            if(modo==2) {
                double c=Math.min(1,juego.tiempoFin5/1.0);float ux=palomo.x()+(float)(c*(jug.x()+26-palomo.x())),uy=(float)(palomo.y()+10+c*c*(jug.y()-palomo.y()-10));
                sombra5(ux,jug.y(),14);imagen("iconos/05_usb",(int)ux-10,(int)uy-20+(c>=1&&!reducido?(int)(Math.sin(t*4)*2):0),20,20);
            }
        }
        g.dispose();g=guard;
        // Capa de globos en pantalla: texto legible a tamaño normal.
        int izq=vx,der=vx+vw,arriba=vy;
        if(modo==0||modo==1||modo==3) {
            if(!palomo.globo().isBlank()&&modo!=1) globo("EL PALOMO",palomo.globo(),vx+(int)(palomoX*2),vy+(int)((palomoY-30)*2),290,new Color(0x2A1B2E),new Color(0xF4BD4F),papel,arriba,izq,der);
            if(modo==3) globo("EL PALOMO","Ya lo dije: nadie verifica.",vx+(int)(palomoX*2),vy+(int)((palomoY+10)*2)+96,260,new Color(0x2A1B2E),new Color(0xF4BD4F),papel,arriba,izq,der);
            if(modo==0) {
                if(!jug.globo().isBlank()) globo("CIUDADANO",jug.globo(),vx+(int)(jug.x()*2),vy+(int)((jug.y()-56)*2),300,papel,teal,tinta,arriba,izq,der);
                for(var v:m.vecinos()) if(!v.globo().isBlank()) globo(null,v.globo(),vx+(int)(v.x()*2),vy+(int)((v.y()-50)*2),210,new Color(0xEAD7AF),teal,tinta,arriba,izq,der);
            }
        }
        if(modo==2&&juego.tiempoFin5<2.6) globo("EL PALOMO","Esto no se queda así.",vx+(int)(palomo.x()*2)+120,vy+(int)((palomo.y()+40)*2)+10,240,new Color(0x2A1B2E),new Color(0xF4BD4F),papel,arriba,izq,der);
    }
    private void vecino5(Mision5.VecinoVista v,int modo) {
        boolean reducido=juego.movimientoReducido;double t=juego.animacion;
        boolean alterado=v.conducta()==Mision5.Conducta.ALTERADO||v.conducta()==Mision5.Conducta.HABLANDO;
        boolean mueve=v.conducta()!=Mision5.Conducta.HABLANDO&&modo!=3&&modo!=2;
        int cuadro=reducido||!mueve?0:(int)(t*(alterado?10:6)+v.id())%4;
        float tiembla=alterado&&!reducido&&modo==0&&v.panico()>=60?(float)Math.sin(t*35+v.id())*.8f:0;
        sombra5(v.x(),v.y(),18);
        g.drawImage(vecinoImagen(v.variante(),v.direccion()*4+cuadro),(int)(v.x()+tiembla)-16,(int)v.y()-32,32,32,null);
        int x=(int)v.x(),y=(int)v.y();
        if(v.conducta()==Mision5.Conducta.CALMADO||v.conducta()==Mision5.Conducta.SALIENDO) {
            imagen("iconos/13_confianza",x-7,y-47,14,14);return;
        }
        // Medidor de pánico visible y con forma: «!» cuando es urgente y barra bajo los pies.
        Color c=v.panico()>=75?ROJO:v.panico()>=45?TemaPixel.ORO:new Color(0x6ABCAA);
        if(modo==1) return;
        rect(x-10,y+3,20,3,new Color(0x405461));rect(x-10,y+3,Math.max(1,20*v.panico()/100),3,c);
        if(v.panico()>=55) {
            rect(x-6,y-47,12,12,v.panico()>=75?ROJO_FUERTE:new Color(0xB07A1E));g.setColor(papel);g.drawRect(x-6,y-47,11,11);
            texto("!",x-2,y-38,9,papel,true);
        }
        if(v.cerca()&&modo==0) {g.setColor(oro);g.setStroke(new BasicStroke(1));g.drawOval(x-12,y-5,24,10);}
    }

    void meta5(String nombre,int valor,int meta,int x,int y,int w) {
        texto(nombre,x,y,13,papel,false);String s=valor+" / "+meta;
        texto(s,x+w-tema.ancho(g,s,tema.fuente(15,true)),y,13,valor>=meta?new Color(0x8FD7B6):oro,true);
        rect(x,y+8,w,6,new Color(0x405461));rect(x,y+8,Math.max(0,Math.min(w,w*valor/Math.max(1,meta))),6,new Color(0x6ABCAA));
    }
    void contador5(String nombre,int valor,int x,int y,int w) {
        texto(nombre,x,y,13,papel,false);
        texto(""+valor,x+w-tema.ancho(g,""+valor,tema.fuente(15,true)),y,13,valor>=70?ROJO:oro,true);
        rect(x,y+8,w,6,new Color(0x405461));rect(x,y+8,Math.max(1,w*valor/100),6,valor>=70?ROJO:valor>=40?TemaPixel.ORO:new Color(0x6ABCAA));
    }

    private void mision5() {
        var m=juego.mision5;boolean solo=m.modo()==Mision5.Modo.SOLO;
        texto("CIUDAD NOVA",28,30,14,oro,true);texto("La calle del pánico",28,65,26,papel,true);
        texto("MISIÓN 5  /  "+(solo?"UN JUGADOR":"DOS JUGADORES")+"  /  "+m.puntos()+" puntos",28,90,14,tenue,false);
        double restante=m.restante();boolean urgente=restante<=15&&m.estado()==Mision5.Estado.EN_CURSO;
        imagen("iconos/20_reloj",600,40,32,32);
        texto((int)Math.ceil(restante)+" s",642,70,34,urgente&&((int)(juego.animacion*3)%2==0||juego.movimientoReducido)?ROJO:papel,true);
        rect(600,82,420,8,new Color(0x405461));rect(600,82,(int)(420*restante/Mision5.DURACION),8,urgente?ROJO:oro);
        boton("Pausa [Esc]","menu",1060,35,192,false);
        escena5(24,114,866,484,0);
        // Periodista: radar de noticias ordenado por el AVL (mayor prioridad primero).
        panel(912,114,344,484,new Color(0x22394D));
        texto("PERIODISTA · RADAR DE NOTICIAS",932,146,13,oro,true);
        var noticias=m.noticias();
        boolean enCurso=m.estado()==Mision5.Estado.EN_CURSO;
        for(int i=0;i<noticias.size();i++) {
            var n=noticias.get(i);int cx=924,cw=320,cy=158+i*90,ch=84;
            Rectangle area=new Rectangle(cx,cy,cw,ch);
            boolean sel=i==juego.seleccion5,hover=area.contains(ratonX,ratonY),verif=n.fase()==Mision5.Fase.VERIFICANDO;
            TemaPixel.marco(g,cx,cy,cw,ch,verif?new Color(0xCFE8E2):papel,sel||hover?oro:teal,false);
            if(sel||hover) {g.setColor(oro);g.drawRect(cx-3,cy-3,cw+6,ch+6);}
            rect(cx+10,cy+9,20,20,tinta);texto(""+(i+1),cx+16,cy+25,13,papel,true);
            texto(ajustar(n.fuente(),148,15,true),cx+38,cy+25,13,teal,true);
            rect(cx+cw-112,cy+9,102,20,n.viral()?ROJO_FUERTE:oro);
            texto((n.viral()?"VIRAL ":"Prioridad ")+n.prioridad(),cx+cw-107,cy+25,12,n.viral()?papel:TemaPixel.NOCHE,true);
            parrafo(n.titular(),cx+12,cy+46,cw-24,14,tinta);
            rect(cx+12,cy+ch-10,cw-24,5,new Color(0xC9B896));
            if(verif) {rect(cx+12,cy+ch-10,(int)((cw-24)*n.verificacion()),5,teal);}
            else rect(cx+12,cy+ch-10,(int)((cw-24)*n.vida()),5,n.vida()>.5?teal:n.vida()>.25?new Color(0xE5AA45):ROJO_FUERTE);
            if(enCurso&&n.fase()==Mision5.Fase.ACTIVA) botones.add(new Boton(area,"m5v:"+i,n.titular()));
        }
        if(noticias.isEmpty()) parrafo("Sin publicaciones por ahora. Siguen llegando a Civitas…",936,196,290,16,tenue);
        if(m.enCola()>0) texto("+"+m.enCola()+" en cola · menor prioridad",932,530,12,tenue,false);
        var cierres=m.cierres();
        for(int j=0;j<cierres.size();j++) {
            var c=cierres.get(j);Color fondo=c.fase()==Mision5.Fase.DESMENTIDA?new Color(0x167964):c.fase()==Mision5.Fase.CONFIRMADA?new Color(0x7A6A2A):ROJO_FUERTE;
            panel(924,538+j*28,320,24,fondo);
            String marca=c.fase()==Mision5.Fase.DESMENTIDA?"✓ ":c.fase()==Mision5.Fase.CONFIRMADA?"i ":"✗ ";
            texto(ajustar(marca+c.texto(),296,13,false),934,555+j*28,12,papel,false);
        }
        if(cierres.isEmpty()) texto("Clic o teclas 1–4 para verificar",932,560,12,tenue,false);
        // Medidores de la misión.
        panel(24,608,1232,72,new Color(0x294357));
        meta5("Noticias desmentidas",m.desmentidas(),m.metaDesmentidas(),42,630,270);
        meta5("Vecinos calmados",m.calmados(),m.metaCalmados(),330,630,270);
        contador5("Pánico en la calle",m.panico(),618,630,270);
        contador5("Desinformación",m.desinformacion(),906,630,320);
        texto(ajustar(m.mensaje(),1190,13,false),42,668,13,oro,false);
        pie(solo?"WASD / flechas: caminar    E: calmar al vecino    1–4 o clic: verificar noticia    Enter: la más urgente    Esc: pausa"
            :"J1 Ciudadano: WASD + E    ·    J2 Periodista: ↑ ↓ + Enter, teclas 1–4 o clic    ·    Esc: pausa");
        if(m.estado()==Mision5.Estado.PREPARACION) {
            rect(24,114,866,484,new Color(2,18,32,170));
            String n=""+Math.max(1,m.cuentaAtras());
            texto(n,457-tema.ancho(g,n,tema.fuente(96,true))/2,390,96,oro,true);
            String a="¡Prepárense! Verifica noticias y calma vecinos.";
            texto(a,457-tema.ancho(g,a,tema.fuente(20,true))/2,450,20,papel,true);
        }
    }

    private void mision5Modo() {
        cabecera("Misión 5 · La calle del pánico","70 segundos · desmiente la mentira y calma la calle antes de que Civitas la devore.");
        String[] titulos={"Un jugador","Dos jugadores"};
        for(int i=0;i<2;i++) {
            var modo=Mision5.Modo.values()[i];var datos=new Mision5(modo,0);
            int x=28+i*624;panel(x,146,600,322,new Color(0x22394D));
            imagen("portada",x+8,154,584,114);rect(x+8,154,584,114,new Color(0,25,45,155));
            imagen("actores/ciudadano_00",x+196,162,96,96);imagen("actores/periodista_00",x+308,162,96,96);
            imagen("actores/noti_08",x+500,172,64,64);
            rect(x+10,270,580,2,TemaPixel.CIAN);
            texto(titulos[i],x+22,298,24,papel,true);
            parrafo(i==0?"Eres las dos voces: camina como Ciudadano con WASD y calma con E; verifica noticias con el mouse o las teclas 1–4."
                :"J1 es el Ciudadano (WASD + E) y J2 el Periodista (↑ ↓ + Enter, teclas 1–4 o mouse). Más vecinos, más noticias.",x+22,326,556,16,tenue);
            texto("Meta: desmentir "+datos.metaDesmentidas()+" noticias y calmar "+datos.metaCalmados()+" vecinos",x+22,396,15,oro,true);
            boton(i==0?"Jugar solo":"Jugar en pareja","m5modo:"+modo.name(),x+18,408,564,true);
        }
        boton("Contraste: "+(juego.contraste?"alto":"normal"),"contraste",28,503,284,false);
        boton("Movimiento: "+(juego.movimientoReducido?"reducido":"normal"),"movimiento",340,503,286,false);
        parrafo("Misión 5: sales del Centro de Periodismo tras el ataque informático y te cruzas con un ciudadano igual de asustado. El Palomo aparece para burlarse.",30,592,1130,18,papel);
        pie("Tab y Enter recorren los botones · Esc vuelve al menú");
    }

    private void mision5Intro() {
        var m=juego.mision5;boolean solo=m.modo()==Mision5.Modo.SOLO;int p=juego.pagina5;
        // Sin botón Volver: Enter avanza la historia y Esc regresa al menú.
        cabecera("Misión 5 · La calle del pánico","Una calle, dos voces y un Palomo con ganas de caos.",false);
        escena5(24,133,866,465,1);
        panel(912,133,344,465,new Color(0x22394D));
        String[] voces={"NARRADOR","NARRADOR","EL PALOMO","LA MISIÓN · 70 SEGUNDOS"};
        texto(voces[p],936,168,14,oro,true);rect(932,178,303,2,TemaPixel.CIAN);
        switch(p) {
            case 0 -> parrafo("Estás saliendo del Centro de Periodismo, justo después del ataque informático que viviste en la oficina. Al dar el primer paso hacia la calle, te encuentras cara a cara con un ciudadano que camina alterado, intentando asimilar el miedo y la tensión por lo que ocurre dentro de Civitas.",936,210,296,18,papel);
            case 1 -> parrafo("De la nada, se cruzan y se dan cuenta de que ambos recibieron la misma propuesta sospechosa de la paloma para interactuar con la red social «Civitas» y vieron las mismas publicaciones extrañas sobre noticias y rumores falsos.",936,210,296,18,papel);
            case 2 -> parrafo("«Vaya, vaya, los dos idiotas que cayeron en la red se juntaron a llorar. Veamos si pueden salvar la ciudad ahora juntos.»",936,210,296,20,papel);
            default -> {
                int y=204;
                y=parrafo("• Periodista: verifica y desmiente las noticias falsas que llegan a Civitas.",936,y,296,15,papel)+6;
                y=parrafo("• Ciudadano: acércate a los vecinos asustados y cálmalos con mensajes de civismo.",936,y,296,15,papel)+6;
                y=parrafo("• El Palomo interrumpe con burlas, chismes y sabotajes.",936,y,296,15,papel)+6;
                parrafo("META: desmentir "+m.metaDesmentidas()+" noticias y calmar "+m.metaCalmados()+" vecinos sin que el pánico o la desinformación lleguen a 100.",936,y+6,296,15,oro);
            }
        }
        texto((p+1)+" / "+JuegoCliente.PAGINAS_INTRO5,936,482,13,tenue,false);
        boton(p==JuegoCliente.PAGINAS_INTRO5-1?"¡Empezar!  →":"Continuar  →","m5sig",932,494,303,true);
        boton("Saltar historia","m5empezar",932,544,303,false);
        panel(24,608,1232,72,new Color(0x294357));
        parrafo(p<3?"Enter / Espacio: continuar    ·    Esc: volver al menú":solo?"Un jugador: WASD o flechas para caminar, E para calmar, clic o teclas 1–4 para verificar noticias, Enter para la más urgente."
            :"Dos jugadores: J1 Ciudadano con WASD + E · J2 Periodista con ↑ ↓ + Enter, teclas 1–4 o clic. El reloj empieza tras la cuenta atrás.",42,638,1185,15,papel);
    }

    private void mision5Fin() {
        var m=juego.mision5;boolean victoria=m.estado()==Mision5.Estado.VICTORIA;
        texto("MISIÓN 5",28,30,14,oro,true);
        texto(victoria?"La calle respira":"La calle cayó",28,65,26,victoria?papel:ROJO,true);
        texto((m.modo()==Mision5.Modo.SOLO?"UN JUGADOR":"DOS JUGADORES")+"  /  "+m.puntos()+" puntos",28,90,14,tenue,false);
        escena5(24,114,866,484,victoria?2:3);
        panel(912,114,344,484,new Color(0x22394D));
        texto(m.puntos()+" PUNTOS",936,160,29,victoria?oro:ROJO,true);
        meta5("Noticias desmentidas",m.desmentidas(),m.metaDesmentidas(),936,206,296);
        meta5("Vecinos calmados",m.calmados(),m.metaCalmados(),936,252,296);
        contador5("Pánico en la calle",m.panico(),936,298,296);
        contador5("Desinformación",m.desinformacion(),936,344,296);
        if(victoria) {
            panel(924,370,320,124,papel);
            imagen("iconos/05_usb",934,380,40,40);
            if(juego.enHistoria()) {
                texto("MEMORIA USB DEL PALOMO",986,406,14,teal,true);
                parrafo("Adentro está la «Operación Pánico Total»: un plan de cuatro pasos. Hay que detenerlo paso a paso.",934,436,298,13,tinta);
            } else {
                texto("PISTA CLAVE",986,406,14,teal,true);
                parrafo("Civitas ordena lo que ves por reacciones, no por verdad. Alguien dentro del sistema le da prioridad al pánico.",934,436,298,13,tinta);
            }
        } else parrafo(m.motivoTexto(),936,404,296,15,papel);
        if(juego.capitulo==alcaldedigital.cliente.campana.Campana.Capitulo.M5) {
            if(victoria) {boton("Continuar la historia  →","hsiguiente",932,502,303,true);boton("Repetir","m5reintentar",932,548,146,false);}
            else {boton("Reintentar","m5reintentar",932,502,303,true);boton("Relato","hcap:M5",932,548,146,false);}
            boton("Mapa","historia",1089,548,146,false);
        } else {
            boton(victoria?"Jugar de nuevo":"Reintentar","m5reintentar",932,502,303,true);
            boton("Cambiar modo","m5otromodo",932,548,146,false);boton("Menú","menu",1089,548,146,false);
        }
        panel(24,608,1232,72,new Color(0x294357));
        if(victoria) {
            texto("EL PALOMO: «Esto no se queda así.»",42,636,15,oro,true);
            texto("«Menos mal logramos frenar esta histeria colectiva antes de que empeorara en la red.»",42,664,15,papel,false);
        } else {
            texto("Fin de la misión: "+m.motivoTexto(),42,636,15,oro,true);
            texto("Pulsa Reintentar para volver a intentarlo con otra semilla.",42,664,15,papel,false);
        }
        if(!victoria) {
            rect(24,114,866,484,new Color(110,8,12,150));
            TemaPixel.marco(g,150,230,614,176,new Color(0x2A0A0E),ROJO,true);
            texto("La ciudad cayó en la mentira.",190,300,36,ROJO,true);
            texto("Perdiste.",190,360,56,ROJO,true);
        }
    }
}
