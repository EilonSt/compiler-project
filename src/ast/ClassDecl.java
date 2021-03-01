package ast;

import javax.xml.bind.annotation.*;
import java.util.List;

public class ClassDecl extends AstNode {
    @XmlElement(required = true)
    private String name;

    @XmlElement(required = false)
    private String superName;

    @XmlElementWrapper(name="fields", required=true)
    @XmlElement(name="field")
    private List<VarDecl> fields;

    @XmlElementWrapper(name="methoddecls", required=true)
    @XmlElement(name="methoddecl")
    private List<MethodDecl> methoddecls;

    VTFields VTF;
    //private HashSet<String> fieldsSet;
    //private HashMap<String,ArrayList<String>> fieldsTypeInitialized;
    //private HashMap<String,ArrayList<String>> methodsSet;

    // for deserialization only!
    public ClassDecl() {
        this.VTF = new VTFields();
        //this.fieldsSet = new HashSet<String>();
        //this.methodsSet = new HashMap<String,ArrayList<String>>();
        //this.fieldsTypeInitialized = new HashMap<String,ArrayList<String>>();
    }

    public ClassDecl(String name, String superName, List<VarDecl> fields, List<MethodDecl> methoddecls) {
        super();
        this.name = name;
        this.superName = superName;
        this.fields = fields;
        this.methoddecls = methoddecls;
        this.VTF = new VTFields();
        //this.fieldsSet = new HashSet<String>();
        //this.methodsSet = new HashMap<String,ArrayList<String>>();
        //this.fieldsTypeInitialized = new HashMap<String,ArrayList<String>>();
    }

    /*
    public HashSet<String> fieldsSet(){
        return this.fieldsSet;
    }*/

    /*
    public HashMap<String,ArrayList<String>> fieldsTypeInitialized(){
        return this.fieldsTypeInitialized;
    }*/
    
    /*
    public HashMap<String,ArrayList<String>> methodsSet(){
        return this.methodsSet;
    }
    */

    public void accept(Visitor v) {
        v.visit(this);
    }

    public String name() {
        return name;
    }

    public String superName() {
        return superName;
    }

    public List<VarDecl> fields() {
        return fields;
    }

    public List<MethodDecl> methoddecls() {
        return methoddecls;
    }

}
