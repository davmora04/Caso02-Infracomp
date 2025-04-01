import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimuladorNRU {
    
    private PaginaInfo[] tablaPaginas;   // Info de cada página virtual
    private int[] marcos;                // marcos[i] = página que está en marco i, o -1 si vacío
    
    // Contadores
    private long hits = 0;
    private long misses = 0;
    private long totalReferencias = 0;
    
    // Parámetros
    private int pageSize;    // Tamaño de página (leído del archivo de referencias)
    private int numPaginas;  // Número de páginas virtuales
    
    // Para sincronizar el acceso a tablaPaginas y marcos
    private final Object lock = new Object();
    
    // Hilo B se mantiene corriendo hasta que terminemos
    private volatile boolean fin = false;
    
    // Lista de referencias
    private List<Referencia> referencias = new ArrayList<>();
    
    public void simular(String archivoReferencias, int numMarcos) throws InterruptedException {
        // 1) Leer archivo
        long expectedNR = leerArchivoReferencias(archivoReferencias);
        
        // Verificar si el número de referencias leídas coincide con el valor esperado
        if (expectedNR != 0 && expectedNR != referencias.size()) {
            System.out.println("Warning: Se esperaba NR=" + expectedNR + " pero se leyeron " + referencias.size() + " referencias.");
        }
        
        // Inicializar la tabla de páginas
        tablaPaginas = new PaginaInfo[numPaginas];
        for (int i = 0; i < tablaPaginas.length; i++) {
            tablaPaginas[i] = new PaginaInfo();
        }
        
        // Inicializar marcos
        marcos = new int[numMarcos];
        Arrays.fill(marcos, -1);
        
        // 2) Crear los hilos
        
        // Hilo A: procesa las referencias
        Thread hiloA = new Thread(() -> {
            try {
                procesarReferencias();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        // Hilo B: limpia bits R cada 1ms
        Thread hiloB = new Thread(() -> {
            while (!fin) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                limpiarBitsR();
            }
        });
        
        // 3) Iniciarlos
        hiloA.start();
        hiloB.start();
        
        // 4) Esperar a que termine hiloA
        hiloA.join();
        
        // 5) Indicar finalización para hiloB y esperar
        fin = true;
        hiloB.join();
        
        // 6) Reportar resultados
        reportarResultados();
    }
    
    /**
     * Lee el archivo de referencias (texto) y retorna el valor esperado de NR.
     * Formato esperado (ejemplo):
     *   TP=512
     *   NF=79
     *   NC=119
     *   ...
     *   NR=756756
     *   NP=111
     *   Imagen[0][0].r,0,0,R
     *   ...
     */
private long leerArchivoReferencias(String archivo) {
    long expectedNR = 0;
    try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
        String linea;
        while ((linea = br.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty()) {
                continue; // Saltar líneas vacías
            }
            if (linea.startsWith("TP=")) {
                // Tamaño de página
                String val = linea.substring(3).trim();
                pageSize = Integer.parseInt(val);
            } else if (linea.startsWith("NF=")) {
                // NF=, número de filas (opcional, se puede almacenar si es necesario)
            } else if (linea.startsWith("NC=")) {
                // NC=, número de columnas (opcional)
            } else if (linea.startsWith("NR=")) {
                String val = linea.substring(3).trim();
                expectedNR = Long.parseLong(val);
            } else if (linea.startsWith("NP=")) {
                String val = linea.substring(3).trim();
                numPaginas = Integer.parseInt(val);
            }
            // Procesar líneas de referencias
            else if (linea.contains(",")) {
                // Validar que la línea tenga exactamente 3 comas (4 partes)
                int commaCount = linea.length() - linea.replace(",", "").length();
                if (commaCount != 3) {
                    System.out.println("Formato inválido (número de campos incorrecto) en la línea: " + linea);
                    continue;
                }
                String[] partes = linea.split(",");
                if (partes.length != 4) {
                    System.out.println("Formato inválido (no se obtuvieron 4 partes) en la línea: " + linea);
                    continue;
                }
                // Validar que la primera parte no esté vacía
                if (partes[0].trim().isEmpty()) {
                    System.out.println("Referencia vacía en la línea: " + linea);
                    continue;
                }
                // Validar que las partes 2 y 3 sean enteros
                int page, off;
                try {
                    page = Integer.parseInt(partes[1].trim());
                    off = Integer.parseInt(partes[2].trim());
                } catch (NumberFormatException nfe) {
                    System.out.println("Error al parsear números en la línea: " + linea);
                    continue;
                }
                // Validar que la cuarta parte sea "R" o "W"
                String modo = partes[3].trim();
                if (!modo.equals("R") && !modo.equals("W")) {
                    System.out.println("Modo inválido (debe ser 'R' o 'W') en la línea: " + linea);
                    continue;
                }
                // Crear la referencia y agregarla a la lista
                Referencia ref = new Referencia(page, off, modo.equals("W"));
                referencias.add(ref);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return expectedNR;
}

    
    private void procesarReferencias() throws InterruptedException {
        long count = 0;
        for (Referencia r : referencias) {
            acceder(r);
            count++;
            
            if (count % 10000 == 0) {
                // Simulamos que este hilo corre cada ~1ms
                Thread.sleep(1);
            }
        }
    }
    
    private void acceder(Referencia r) {
        synchronized (lock) {
            totalReferencias++;
            // Marcar bit R=1
            PaginaInfo p = tablaPaginas[r.pageNumber];
            p.bitR = 1;
            if (r.isWrite) {
                p.bitM = 1;
            }
            
            // Chequear si la página ya está en RAM
            if (estaEnMarcos(r.pageNumber)) {
                hits++;
            } else {
                misses++;
                manejarFalla(r.pageNumber);
            }
        }
    }
    
    private boolean estaEnMarcos(int pageNumber) {
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i] == pageNumber) {
                return true;
            }
        }
        return false;
    }
    
    private void manejarFalla(int pageNumber) {
        // 1) Buscar marco libre
        for (int i = 0; i < marcos.length; i++) {
            if (marcos[i] == -1) {
                marcos[i] = pageNumber;
                return;
            }
        }
        // 2) Si no hay libre => reemplazar con NRU
        reemplazarNRU(pageNumber);
    }
    
    private void reemplazarNRU(int newPage) {
        int victimaIndex = -1;
        int mejorClase = 4; // mayor que 3 (clase 0..3)
        
        for (int i = 0; i < marcos.length; i++) {
            int pag = marcos[i];
            PaginaInfo info = tablaPaginas[pag];
            int clase = calcularClase(info.bitR, info.bitM);
            if (clase < mejorClase) {
                mejorClase = clase;
                victimaIndex = i;
            }
        }
        
        // Reemplazo
        marcos[victimaIndex] = newPage;
    }
    
    private int calcularClase(int r, int m) {
        // R=0,M=0 => 0, R=0,M=1 => 1, R=1,M=0 => 2, R=1,M=1 => 3
        if (r == 0 && m == 0) return 0;
        if (r == 0 && m == 1) return 1;
        if (r == 1 && m == 0) return 2;
        return 3;
    }
    
    /**
     * Cada 1ms el hilo B pone bitR=0 en todas las páginas (simulando la limpieza).
     */
    private void limpiarBitsR() {
        synchronized (lock) {
            for (PaginaInfo p : tablaPaginas) {
                p.bitR = 0;
            }
        }
    }
    
    private void reportarResultados() {
        System.out.println("=== RESULTADOS SIMULACION NRU ===");
        System.out.println("Total de referencias: " + totalReferencias);
        System.out.println("Hits: " + hits);
        System.out.println("Misses: " + misses);
        
        double porcHits = (100.0 * hits) / totalReferencias;
        System.out.printf("Porcentaje de hits: %.2f %%\n", porcHits);
        
        // Calcular tiempo en base a 50 ns para hits, 10 ms para misses
        // 10 ms => 10_000_000 ns
        long tiempoNs = hits * 50 + misses * 10_000_000;
        System.out.println("Tiempo total estimado (ns): " + tiempoNs);
        
        // Tiempos hipotéticos:
        long tiempoAllHit  = totalReferencias * 50;
        long tiempoAllMiss = totalReferencias * 10_000_000L;
        System.out.println("Tiempo si todo Hit (ns):  " + tiempoAllHit);
        System.out.println("Tiempo si todo Miss (ns): " + tiempoAllMiss);
        
        // Reportar parámetros usados
        System.out.println("Tamaño de página: " + pageSize + " bytes");
        System.out.println("Número de marcos asignados: " + marcos.length);
    }
}
