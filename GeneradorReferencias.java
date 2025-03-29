import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GeneradorReferencias {
    
    /**
     * Genera un archivo de referencias en formato:
     *   TP=xxx
     *   NF=...
     *   NC=...
     *   NR=...
     *   NP=...
     *   (Lista de referencias)
     */
    public void generarArchivoReferencias(String nombreImagen, int pageSize, String archivoSalida) {
        // 1) Cargar la imagen
        Imagen img = new Imagen(nombreImagen);
        int alto = img.alto;
        int ancho = img.ancho;
        
        // 2) Calcular tamaños en bytes
        long sizeImagenBytes  = (long)alto * ancho * 3;
        long sizeFiltroXBytes = 9 * 4; // 9 enteros de 4 bytes cada uno (conceptualmente)
        long sizeFiltroYBytes = 9 * 4;
        long sizeRtaBytes     = (long)alto * ancho * 3;
        
        // 3) Bases virtuales de cada matriz
        long baseImagen  = 0;
        long baseFiltroX = baseImagen + sizeImagenBytes;
        long baseFiltroY = baseFiltroX + sizeFiltroXBytes;
        long baseRta     = baseFiltroY + sizeFiltroYBytes;
        
        // 4) Calcular número total de páginas virtuales
        long totalBytes = baseRta + sizeRtaBytes;
        long numPaginas = (long)Math.ceil((double)totalBytes / pageSize);
        
        // 5) Generar la lista de referencias acumulándolas en una lista
        long contadorReferencias = 0;
        List<String> referenciasList = new ArrayList<>();
        
        // Recorremos el proceso de applySobel lógicamente
        for (int i = 1; i < alto - 1; i++) {
            for (int j = 1; j < ancho - 1; j++) {
                
                // (A) Lecturas: 9 vecinos de la imagen, cada uno con 3 componentes (B, G, R)
                for (int ki = -1; ki <= 1; ki++) {
                    for (int kj = -1; kj <= 1; kj++) {
                        for (int comp = 0; comp < 3; comp++) {
                            long dirVirtual = baseImagen + offsetImagen(alto, ancho, i + ki, j + kj, comp);
                            long pag = dirVirtual / pageSize;
                            long off = dirVirtual % pageSize;
                            referenciasList.add("Imagen[" + (i + ki) + "][" + (j + kj) + "]." 
                                    + componenteRGB(comp) + "," + pag + "," + off + ",R");
                            contadorReferencias++;
                        }
                    }
                }
                
                // (B) 9 lecturas del filtro SOBEL_X (cada int se lee en 3 accesos)
                for (int f = 0; f < 9; f++) {
                    for (int b = 0; b < 3; b++) {
                        long dirVirtual = baseFiltroX + f * 3 + b;
                        long pag = dirVirtual / pageSize;
                        long off = dirVirtual % pageSize;
                        referenciasList.add("SOBEL_X[" + f + "]," + pag + "," + off + ",R");
                        contadorReferencias++;
                    }
                }
                
                // (C) 9 lecturas del filtro SOBEL_Y (cada int se lee en 3 accesos)
                for (int f = 0; f < 9; f++) {
                    for (int b = 0; b < 3; b++) {
                        long dirVirtual = baseFiltroY + f * 3 + b;
                        long pag = dirVirtual / pageSize;
                        long off = dirVirtual % pageSize;
                        referenciasList.add("SOBEL_Y[" + f + "]," + pag + "," + off + ",R");
                        contadorReferencias++;
                    }
                }
                
                // (D) Escritura en la matriz de respuesta: 3 bytes (B, G, R)
                for (int comp = 0; comp < 3; comp++) {
                    long dirVirtual = baseRta + offsetImagen(alto, ancho, i, j, comp);
                    long pag = dirVirtual / pageSize;
                    long off = dirVirtual % pageSize;
                    referenciasList.add("Rta[" + i + "][" + j + "]." + componenteRGB(comp)
                            + "," + pag + "," + off + ",W");
                    contadorReferencias++;
                }
            }
        }
        
        // 6) Escribir en el archivo: primero el encabezado en el orden TP, NF, NC, NR, NP y luego la lista de referencias
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoSalida))) {
            pw.println("TP=" + pageSize);
            pw.println("NF=" + alto);
            pw.println("NC=" + ancho);
            pw.println("NR=" + contadorReferencias);
            pw.println("NP=" + numPaginas);
            
            for (String ref : referenciasList) {
                pw.println(ref);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Calcula el offset dentro de la matriz de la imagen (row-major order).
     * comp = 0 -> B, 1 -> G, 2 -> R.
     */
    private long offsetImagen(int alto, int ancho, int i, int j, int comp) {
        long pixelIndex = (long)i * ancho + j;
        return pixelIndex * 3 + comp;
    }
    
    /**
     * Devuelve el identificador de componente RGB: 'b', 'g' o 'r'.
     */
    private String componenteRGB(int comp) {
        switch (comp) {
            case 0:
                return "b";
            case 1:
                return "g";
            case 2:
                return "r";
        }
        return "?";
    }
}
