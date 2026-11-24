package alcaldedigital.cliente.recursos;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Recursos empaquetados: también funciona desde un JAR fuera del repositorio. */
public final class Recursos {
    private final Map<String,BufferedImage> cache=new HashMap<>();
    public BufferedImage imagen(String nombre) {
        return cache.computeIfAbsent(nombre,n->{
            // «../mision6/cielo» sale de la carpeta de arte: recursos compartidos con la misión 6.
            String ruta=n.startsWith("../")?"/cliente/"+n.substring(3)+".png":"/cliente/arte/"+n+".png";
            try(var in=Recursos.class.getResourceAsStream(ruta)) {
                if(in==null) throw new IllegalStateException("Falta recurso: "+n);
                BufferedImage im=ImageIO.read(in);
                if(im==null) throw new IllegalStateException("PNG inválido: "+n);
                return im;
            } catch(IOException e) {throw new UncheckedIOException(e);}
        });
    }
}
