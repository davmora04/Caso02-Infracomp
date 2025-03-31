import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GeneradorReferencias {

    /**
     * Genera un archivo de referencias en formato:
     * TP=xxx
     * NF=...
     * NC=...
     * NR=...
     * NP=...
     * (Lista de referencias)
     * 
     * Se asume que el archivo BMP se encuentra en "caso2-Anexos/".
     * Este método invoca FiltroSobel.applySobel() para procesar la imagen
     * y luego simula la generación de las referencias sin utilizar Logger.
     *
     * @param nombreImagen   Nombre del archivo BMP (ej: caso2-parrotspeq.bmp)
     * @param pageSize       Tamaño de página (en bytes), por ejemplo 512
     * @param archivoSalida  Nombre del archivo de salida (ej: refs512.txt)
     */
    public void generarArchivoReferencias(String nombreImagen, int pageSize, String archivoSalida) {
        // Construir la ruta del archivo BMP
        String rutaImagen = "Caso02-Infracomp\\caso2-Anexos\\" + nombreImagen;
        
        // 1) Cargar la imagen de entrada y crear la imagen de salida (para conservar cabecera)
        Imagen imgIn = new Imagen(rutaImagen);
        int alto = imgIn.alto;
        int ancho = imgIn.ancho;
        Imagen imgOut = new Imagen(rutaImagen);
        
        // 2) Invocar FiltroSobel para procesar la imagen (se mantiene sin modificar)
        FiltroSobel filtro = new FiltroSobel(imgIn, imgOut);
        filtro.applySobel();
        
        // 3) Calcular tamaños en bytes de las estructuras
        long sizeImagenBytes  = (long) alto * ancho * 3;   // Imagen de entrada
        long sizeFiltroXBytes = 9 * 4;                     // 9 enteros para SOBEL_X
        long sizeFiltroYBytes = 9 * 4;                     // 9 enteros para SOBEL_Y
        long sizeRtaBytes     = (long) alto * ancho * 3;    // Imagen de salida
        
        // 4) Definir las bases virtuales (organizadas de forma contigua)
        long baseImagen  = 0;
        long baseFiltroX = baseImagen + sizeImagenBytes;
        long baseFiltroY = baseFiltroX + sizeFiltroXBytes;
        long baseRta     = baseFiltroY + sizeFiltroYBytes;
        
        // 5) Calcular el número total de páginas virtuales
        long totalBytes = baseRta + sizeRtaBytes;
        long numPaginas = (long) Math.ceil((double) totalBytes / pageSize);
        
        // 6) Generar la lista de referencias (simulación de accesos a memoria)
        long contadorReferencias = 0;
        List<String> referenciasList = new ArrayList<>();
        
        // Arreglo de offsets para la ventana 3x3 (vecinos del píxel central)
        int[][] offsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            { 0, -1}, { 0, 0}, { 0, 1},
            { 1, -1}, { 1, 0}, { 1, 1}
        };
        
        // Recorrer cada píxel central (omitiendo los bordes)
        for (int i = 1; i < alto - 1; i++) {
            for (int j = 1; j < ancho - 1; j++) {
                // Para cada vecino en la ventana 3x3
                for (int[] offArr : offsets) {
                    int di = offArr[0];
                    int dj = offArr[1];
                    int ni = i + di;
                    int nj = j + dj;
                    
                    // Registrar accesos de lectura a la imagen de entrada (3 componentes)
                    for (int comp = 0; comp < 3; comp++) {
                        long dirVirtual = baseImagen + offsetImagen(alto, ancho, ni, nj, comp);
                        long pag = dirVirtual / pageSize;
                        long off = dirVirtual % pageSize;
                        // Se asume que componenteRGB devuelve una cadena, se usa charAt(0)
                        referenciasList.add("Imagen[" + ni + "][" + nj + "]." 
                            + componenteRGB(comp).charAt(0) + "," + pag + "," + off + ",R");
                        contadorReferencias++;
                    }
                    
                    // Convertir (di, dj) a índices del kernel: (di+1, dj+1)
                    int kernelRow = di + 1;  // valores de 0 a 2
                    int kernelCol = dj + 1;  // valores de 0 a 2
                    int index = kernelRow * 3 + kernelCol; // índice de 0 a 8
                    
                    // Para SOBEL_X: se registran 3 accesos idénticos
                    long dirVirtualX = baseFiltroX + index * 4;
                    long pagX = dirVirtualX / pageSize;
                    long offX = dirVirtualX % pageSize;
                    for (int b = 0; b < 3; b++) {
                        referenciasList.add("SOBEL_X[" + kernelRow + "][" + kernelCol + "]," 
                            + pagX + "," + offX + ",R");
                        contadorReferencias++;
                    }
                    
                    // Para SOBEL_Y: se registran 3 accesos idénticos
                    long dirVirtualY = baseFiltroY + index * 4;
                    long pagY = dirVirtualY / pageSize;
                    long offY = dirVirtualY % pageSize;
                    for (int b = 0; b < 3; b++) {
                        referenciasList.add("SOBEL_Y[" + kernelRow + "][" + kernelCol + "]," 
                            + pagY + "," + offY + ",R");
                        contadorReferencias++;
                    }
                }
                // Registrar accesos de escritura para la imagen de salida (Rta) para el píxel central (i, j)
                for (int comp = 0; comp < 3; comp++) {
                    long dirVirtual = baseRta + offsetImagen(alto, ancho, i, j, comp);
                    long pag = dirVirtual / pageSize;
                    long off = dirVirtual % pageSize;
                    referenciasList.add("Rta[" + i + "][" + j + "]." 
                        + componenteRGB(comp).charAt(0) + "," + pag + "," + off + ",W");
                    contadorReferencias++;
                }
            }
        }
        
        // 7) Escribir en el archivo de salida el encabezado y la lista de referencias
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
        
        // 8) Guardar la imagen de salida filtrada
        imgOut.escribirImagen("caso2-parrotspeq_sal.bmp");
    }
    
    /**
     * Método auxiliar para calcular el offset dentro de la matriz (row-major order).
     * Cada píxel ocupa 3 bytes (r, g, b).
     * Se asume:
     *   comp = 0 -> r, 1 -> g, 2 -> b.
     */
    private long offsetImagen(int alto, int ancho, int i, int j, int comp) {
        long pixelIndex = (long) i * ancho + j;
        return pixelIndex * 3 + comp;
    }
    
    /**
     * Devuelve el identificador del componente en minúscula ("r", "g" o "b").
     */
    private String componenteRGB(int comp) {
        switch (comp) {
            case 0: return "r";
            case 1: return "g";
            case 2: return "b";
        }
        return "?";
    }
}
