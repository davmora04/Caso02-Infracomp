public class FiltroSobel {
    
    Imagen imagenIn;
    Imagen imagenOut;
    
    public FiltroSobel(Imagen imagenEntrada, Imagen imagenSalida){
        this.imagenIn = imagenEntrada;
        this.imagenOut = imagenSalida;
    }
    
    // Sobel Kernels
    static final int[][] SOBEL_X = {
        {-1, 0, 1},
        {-2, 0, 2},
        {-1, 0, 1}
    };
    static final int[][] SOBEL_Y = {
        {-1, -2, -1},
        { 0,  0,  0},
        { 1,  2,  1}
    };
    
    /**
     * Aplica el filtro de Sobel a la imagen
     */
    public void applySobel() {
        for(int i=1; i<imagenIn.alto -1; i++){
            for(int j=1; j<imagenIn.ancho-1; j++){
                
                int gradXRed=0,   gradXGreen=0,   gradXBlue=0;
                int gradYRed=0,   gradYGreen=0,   gradYBlue=0;
                
                for(int ki=-1; ki<=1; ki++){
                    for(int kj=-1; kj<=1; kj++){
                        int red   = (imagenIn.imagen[i+ki][j+kj][2] & 0xFF);
                        int green = (imagenIn.imagen[i+ki][j+kj][1] & 0xFF);
                        int blue  = (imagenIn.imagen[i+ki][j+kj][0] & 0xFF);
                        
                        gradXRed   += red   * SOBEL_X[ki+1][kj+1];
                        gradXGreen += green * SOBEL_X[ki+1][kj+1];
                        gradXBlue  += blue  * SOBEL_X[ki+1][kj+1];
                        
                        gradYRed   += red   * SOBEL_Y[ki+1][kj+1];
                        gradYGreen += green * SOBEL_Y[ki+1][kj+1];
                        gradYBlue  += blue  * SOBEL_Y[ki+1][kj+1];
                    }
                }
                
                int redVal   = (int) Math.sqrt(gradXRed*gradXRed + gradYRed*gradYRed);
                int greenVal = (int) Math.sqrt(gradXGreen*gradXGreen + gradYGreen*gradYGreen);
                int blueVal  = (int) Math.sqrt(gradXBlue*gradXBlue + gradYBlue*gradYBlue);
                
                // Clamping a [0..255]
                redVal   = Math.min(Math.max(redVal, 0), 255);
                greenVal = Math.min(Math.max(greenVal, 0), 255);
                blueVal  = Math.min(Math.max(blueVal, 0), 255);
                
                // Guardar en la imagenOut (B, G, R)
                imagenOut.imagen[i][j][0] = (byte)blueVal;
                imagenOut.imagen[i][j][1] = (byte)greenVal;
                imagenOut.imagen[i][j][2] = (byte)redVal;
            }
        }
    }
}
