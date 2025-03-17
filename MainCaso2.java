import java.util.Scanner;

public class MainCaso2 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        while(true){
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1) Generar archivo de referencias (Opcion 1)");
            System.out.println("2) Simular NRU (Opcion 2)");
            System.out.println("3) Salir");
            System.out.print("Seleccione una opcion: ");
            
            int opcion = sc.nextInt();
            sc.nextLine(); // Consumir salto de línea
            
            switch(opcion){
                case 1:
                    System.out.print("Ingrese tamaño de página (bytes): ");
                    int pageSize = sc.nextInt();
                    sc.nextLine();
                    
                    System.out.print("Ingrese nombre del archivo BMP (ej: parrotspeq.bmp): ");
                    String bmp = sc.nextLine();
                    
                    System.out.print("Ingrese nombre para el archivo de salida de referencias: ");
                    String outFile = sc.nextLine();
                    
                    GeneradorReferencias gr = new GeneradorReferencias();
                    gr.generarArchivoReferencias(bmp, pageSize, outFile);
                    
                    System.out.println("Archivo de referencias generado en: " + outFile);
                    break;
                    
                case 2:
                    System.out.print("Ingrese número de marcos: ");
                    int marcos = sc.nextInt();
                    sc.nextLine();
                    
                    System.out.print("Ingrese nombre del archivo de referencias: ");
                    String refsFile = sc.nextLine();
                    
                    SimuladorNRU sim = new SimuladorNRU();
                    try {
                        sim.simular(refsFile, marcos);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                    
                case 3:
                    System.out.println("Saliendo...");
                    sc.close();
                    System.exit(0);
                    
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }
}
