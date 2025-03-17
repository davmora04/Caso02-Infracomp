public class Referencia {
    public int pageNumber;
    public int offset;
    public boolean isWrite;
    
    public Referencia(int pageNumber, int offset, boolean isWrite) {
        this.pageNumber = pageNumber;
        this.offset = offset;
        this.isWrite = isWrite;
    }
}
