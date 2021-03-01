package ast;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class MethodDecl extends AstNode {
    //private ArrayList<String> staticSignature;
    //private HashMap<String,ArrayList<String>> varsTypeInit;
    private HashMap<String,String> formalsType;
    

    @XmlElement(required = true)
    private AstTypeWrapper returnType;
    @XmlElement(required = true)
    private String name;

    @XmlElementWrapper(name="formals", required = true)
    @XmlElement(name="formal")
    private List<FormalArg> formals;

    @XmlElementWrapper(name="vardecls", required = true)
    @XmlElement(name="vardecl")
    private List<VarDecl> vardecls;

    @XmlElementWrapper(name="body", required=true)
    @XmlElements({
            @XmlElement(name="block", type=BlockStatement.class, required = false),
            @XmlElement(name="if", type=IfStatement.class, required = false),
            @XmlElement(name="while", type=WhileStatement.class, required = false),
            @XmlElement(name="sysout", type=SysoutStatement.class, required = false),
            @XmlElement(name="assign", type=AssignStatement.class, required = false),
            @XmlElement(name="assign-array", type=AssignArrayStatement.class, required = false)
    })
    private List<Statement> body;

    @XmlElement(required = true)
    private ExprWrapper ret;

    // for deserialization only!
    public MethodDecl() {
        //this.staticSignature = new ArrayList<String>();
        //this.varsTypeInit = new HashMap<String,ArrayList<String>>();
        this.formalsType = new HashMap<String,String>();
        
    }

    public MethodDecl(AstType returnType, String name,
                      List<FormalArg> formals, List<VarDecl> vardecls, List<Statement> body, Expr ret,
                      Integer lineNumber) {
        // lineNumber = null means it won't be marshaled to the XML
        super(lineNumber);
        this.returnType = new AstTypeWrapper(returnType);
        this.name = name;
        this.formals = formals;
        this.vardecls = vardecls;
        this.body = body;
        this.ret = new ExprWrapper(ret);
        //this.staticSignature = new ArrayList<String>();
        //this.varsTypeInit = new HashMap<String,ArrayList<String>>();
        this.formalsType = new HashMap<String,String>();
    }
    public HashMap<String,String> formalsType(){
        return this.formalsType;
    }
    /*
    public ArrayList<String> staticSignature(){
        return this.staticSignature;
    }*/

    /*
    public HashMap<String,ArrayList<String>> varsTypeInit(){
        return this.varsTypeInit;
    }*/

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }

    public AstType returnType() {
        return returnType.t;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FormalArg> formals() {
        return formals;
    }

    public List<VarDecl> vardecls() {
        return vardecls;
    }

    public List<Statement> body() {
        return body;
    }

    public Expr ret() {
        return ret.e;
    }


}
