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
     */
    public void generarArchivoReferencias(String nombreImagen, int pageSize, String archivoSalida) {
        // Se concatena la ruta al nombre del archivo
        String rutaImagen = "caso2-Anexos/" + nombreImagen;

        // 1) Cargar la imagen
        Imagen img = new Imagen(rutaImagen);
        int alto = img.alto;
        int ancho = img.ancho;

        // 2) Calcular tamaños en bytes
        long sizeImagenBytes  = (long) alto * ancho * 3;   // 3 bytes por píxel (r, g, b)
        long sizeFiltroXBytes = 9 * 4;                     // 9 enteros (4 bytes cada uno)
        long sizeFiltroYBytes = 9 * 4;
        long sizeRtaBytes     = (long) alto * ancho * 3;

        // 3) Bases virtuales de cada matriz (en memoria virtual contigua)
        long baseImagen  = 0;
        long baseFiltroX = baseImagen + sizeImagenBytes;
        long baseFiltroY = baseFiltroX + sizeFiltroXBytes;
        long baseRta     = baseFiltroY + sizeFiltroYBytes;

        // 4) Calcular número total de páginas virtuales
        long totalBytes = baseRta + sizeRtaBytes;
        long numPaginas = (long) Math.ceil((double) totalBytes / pageSize);

        // 5) Generar la lista de referencias
        long contadorReferencias = 0;
        List<String> referenciasList = new ArrayList<>();

        // Definir los offsets para los 9 vecinos (en el orden: (-1,-1), (-1,0), (-1,1),
        // (0,-1), (0,0), (0,1), (1,-1), (1,0), (1,1))
        int[][] offsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            { 0, -1}, { 0, 0}, { 0, 1},
            { 1, -1}, { 1, 0}, { 1, 1}
        };

        // Procesar cada píxel central (omitiendo los bordes)
        for (int i = 1; i < alto - 1; i++) {
            for (int j = 1; j < ancho - 1; j++) {
                // Para cada vecino en el orden definido
                for (int[] offArr : offsets) {
                    int di = offArr[0];
                    int dj = offArr[1];
                    int ni = i + di;
                    int nj = j + dj;
                    // Para la matriz Imagen: usamos los índices directos (ni, nj)
                    for (int comp = 0; comp < 3; comp++) {
                        long dirVirtual = baseImagen + offsetImagen(alto, ancho, ni, nj, comp);
                        long pag = dirVirtual / pageSize;
                        long off = dirVirtual % pageSize;
                        // Ahora se imprime como "Imagen[ni][nj]." sin restar 1
                        referenciasList.add("Imagen[" + ni + "][" + nj + "]." 
                                + componenteRGB(comp) + "," + pag + "," + off + ",R");
                        contadorReferencias++;
                    }
                    
                    // Para el filtro SOBEL_X: se mapea el offset al índice del kernel.
                    // kernelRow = di + 1, kernelCol = dj + 1, (valores de 0 a 2)
                    int kernelRow = di + 1;
                    int kernelCol = dj + 1;
                    int index = kernelRow * 3 + kernelCol;  // índice de 0 a 8
                    long dirVirtualX = baseFiltroX + index * 4; // cada entero ocupa 4 bytes
                    long pagX = dirVirtualX / pageSize;
                    long offX = dirVirtualX % pageSize;
                    // Se realizan 3 accesos idénticos
                    for (int b = 0; b < 3; b++) {
                        referenciasList.add("SOBEL_X[" + kernelRow + "][" + kernelCol + "]," + pagX + "," + offX + ",R");
                        contadorReferencias++;
                    }
                    
                    // Para el filtro SOBEL_Y: similar
                    int kernelRowY = di + 1;
                    int kernelColY = dj + 1;
                    int indexY = kernelRowY * 3 + kernelColY;
                    long dirVirtualY = baseFiltroY + indexY * 4;
                    long pagY = dirVirtualY / pageSize;
                    long offY = dirVirtualY % pageSize;
                    for (int b = 0; b < 3; b++) {
                        referenciasList.add("SOBEL_Y[" + kernelRowY + "][" + kernelColY + "]," + pagY + "," + offY + ",R");
                        contadorReferencias++;
                    }
                }
                // Finalmente, se generan las referencias de escritura en Rta para el píxel central (i, j)
                for (int comp = 0; comp < 3; comp++) {
                    long dirVirtual = baseRta + offsetImagen(alto, ancho, i, j, comp);
                    long pag = dirVirtual / pageSize;
                    long off = dirVirtual % pageSize;
                    referenciasList.add("Rta[" + i + "][" + j + "]." + componenteRGB(comp) + "," + pag + "," + off + ",W");
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
     * Cada píxel ocupa 3 bytes. Se asume el siguiente orden de componentes:
     * comp = 0 -> r, 1 -> g, 2 -> b.
     */
    private long offsetImagen(int alto, int ancho, int i, int j, int comp) {
        long pixelIndex = (long) i * ancho + j;
        return pixelIndex * 3 + comp;
    }

    /**
     * Devuelve el identificador de componente RGB en el orden esperado: 'r', 'g' o 'b'.
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
