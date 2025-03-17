import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GeneradorReferencias {
    
    /**
     * Genera un archivo de referencias en formato:
     *   TP=xxx
     *   NF=...
     *   NC=...
     *   NR=...
     *   NP=...
     *   ...
     *   Imagen[...],page,offset,R
     *   ...
     */
    public void generarArchivoReferencias(String nombreImagen, int pageSize, String archivoSalida) {
        // 1) Cargar la imagen
        Imagen img = new Imagen(nombreImagen);
        int alto = img.alto;
        int ancho = img.ancho;
        
        // 2) Calcular tamaños en bytes
        // imagen => alto * ancho * 3 bytes
        long sizeImagenBytes  = (long)alto * ancho * 3;
        // Suponiendo kernel 3x3 con int => 9 int => 36 bytes para SOBEL_X
        long sizeFiltroXBytes = 9 * 4;
        // Igual para SOBEL_Y
        long sizeFiltroYBytes = 9 * 4;
        // Matriz respuesta => alto * ancho * 3 bytes
        long sizeRtaBytes     = (long)alto * ancho * 3;
        
        // 3) Bases virtuales de cada matriz
        long baseImagen  = 0;
        long baseFiltroX = baseImagen + sizeImagenBytes;
        long baseFiltroY = baseFiltroX + sizeFiltroXBytes;
        long baseRta     = baseFiltroY + sizeFiltroYBytes;
        
        // 4) Calcular numPaginas totales
        long totalBytes = baseRta + sizeRtaBytes;
        long numPaginas = (long)Math.ceil((double)totalBytes / pageSize);
        
        // 5) Generar la lista de referencias
        long contadorReferencias = 0;
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoSalida))) {
            // Escribimos encabezado (pero ojo: NR se escribe al final cuando ya sabemos cuántas generamos)
            pw.println("TP=" + pageSize);
            pw.println("NF=" + alto);
            pw.println("NC=" + ancho);
            // Dejamos NR y NP pendientes para el final => usaremos un buffer o algo similar,
            // pero para simplicar, lo escribimos luego al final
            
            // Recorremos applySobel lógicamente
            for(int i = 1; i < alto - 1; i++){
                for(int j = 1; j < ancho - 1; j++){
                    
                    // (A) Lecturas: 9 vecinos de la imagen, cada vecino => 3 bytes (B,G,R)
                    for(int ki=-1; ki<=1; ki++){
                        for(int kj=-1; kj<=1; kj++){
                            // 3 lecturas => B, G, R
                            for(int comp=0; comp<3; comp++){
                                long dirVirtual = baseImagen + offsetImagen(alto, ancho, i+ki, j+kj, comp);
                                long pag = dirVirtual / pageSize;
                                long off = dirVirtual % pageSize;
                                pw.println("Imagen[" + (i+ki) + "][" + (j+kj) + "]."
                                        + componenteRGB(comp) + "," + pag + "," + off + ",R");
                                contadorReferencias++;
                            }
                        }
                    }
                    
                    // (B) 9 lecturas filtroX => 9 int => 36 bytes
                    //    Cada int puede contarse como 4 accesos consecutivos,
                    //    o como un único acceso de 4 bytes. Aquí haremos 4 accesos => (byte0,byte1,byte2,byte3)
                    for(int f=0; f<9; f++){
                        for(int b=0; b<4; b++){
                            long dirVirtual = baseFiltroX + f*4 + b;
                            long pag = dirVirtual / pageSize;
                            long off = dirVirtual % pageSize;
                            pw.println("SOBEL_X[" + f + "],"+ pag + "," + off + ",R");
                            contadorReferencias++;
                        }
                    }
                    
                    // (C) 9 lecturas filtroY => 9 int => 36 bytes
                    for(int f=0; f<9; f++){
                        for(int b=0; b<4; b++){
                            long dirVirtual = baseFiltroY + f*4 + b;
                            long pag = dirVirtual / pageSize;
                            long off = dirVirtual % pageSize;
                            pw.println("SOBEL_Y[" + f + "],"+ pag + "," + off + ",R");
                            contadorReferencias++;
                        }
                    }
                    
                    // (D) Escritura en respuesta => 3 bytes (B, G, R)
                    for(int comp=0; comp<3; comp++){
                        long dirVirtual = baseRta + offsetImagen(alto, ancho, i, j, comp);
                        long pag = dirVirtual / pageSize;
                        long off = dirVirtual % pageSize;
                        pw.println("Rta[" + i + "][" + j + "]." + componenteRGB(comp)
                                + "," + pag + "," + off + ",W");
                        contadorReferencias++;
                    }
                }
            }
            
            // Ahora sí escribimos NR y NP
            pw.println("NR=" + contadorReferencias);
            pw.println("NP=" + numPaginas);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Calcula el offset dentro de la matriz de la imagen (row-major).
     */
    private long offsetImagen(int alto, int ancho, int i, int j, int comp){
        // comp=0 => B, comp=1 => G, comp=2 => R
        // offset = (i*ancho + j)*3 + comp
        long pixelIndex = (long)i*ancho + j;
        return pixelIndex*3 + comp;
    }
    
    /**
     * Devuelve 'b','g','r' según comp
     */
    private String componenteRGB(int comp){
        switch(comp){
            case 0: return "b";
            case 1: return "g";
            case 2: return "r";
        }
        return "?";
    }
}
