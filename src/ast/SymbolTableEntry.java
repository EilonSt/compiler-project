package ast;

public class SymbolTableEntry{
    private String symbol;
    private String kind;
    private String Decl;
    private int lineNum;


    public SymbolTableEntry(String symbol, String kind, String Decl, int lineNum){
        this.kind = kind;
        this.symbol = symbol;
        this.Decl =Decl;
        this.lineNum = lineNum;
    }

    public String get_symbol_kind_Decl(){
        String s = this.symbol;
        s = s + " " + this.kind;
        s = s + " " + this.Decl.toString();
        return s;
    }
    public void setSymbol(String s){
        this.symbol = s;
    }
    public String getSymbol(){
        return symbol;
    }
    public String getKind(){
        return kind;
    }
    public String getDecl(){
        return Decl;
    }

    public int getLineNum(){
        return lineNum;
    }
}