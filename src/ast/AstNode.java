package ast;

import javax.xml.bind.annotation.XmlElement;

public abstract class AstNode {
    @XmlElement(required = false)
    public Integer lineNumber;
    private SymbolTable enclosingScope;


    public AstNode() {
        lineNumber = null;
        enclosingScope = new SymbolTable();
    }

    public AstNode(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public SymbolTable enclosingScope(){
        return enclosingScope;

    }

    abstract public void accept(Visitor v);
}
