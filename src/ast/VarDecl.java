package ast;

public class VarDecl extends VariableIntroduction {
    private int cnt = 0;
    // for deserialization only!
    public VarDecl() {
    }

    public VarDecl(AstType type, String name, Integer lineNumber) {
        // lineNumber = null means it won't be marshaled to the XML
        super(type, name, lineNumber);
    }
    public VarDecl(AstType type, String name) {
        // lineNumber = null means it won't be marshaled to the XML
        super(type, name, 0);
        cnt++;
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }
}
