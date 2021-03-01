package ast;

import javax.xml.bind.annotation.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;


@XmlRootElement
public class Program extends AstNode {
    // INBAL ADDED
    private HashMap<String,HashMap<String,String>> ClassFields = new HashMap<String,HashMap<String,String>>();
    // outer hashmap key is className, inner hashmap is Class fields (key is field name, value is fieldtype)


    private HashMap<String,HashSet<String>> ClassAncestors = new HashMap<String,HashSet<String>>();
    private HashMap<String,SymbolTable> STclasses = new HashMap<String,SymbolTable>();
    private HashMap<String,VTFields> VTClassFields = new HashMap<String,VTFields>();
    //private HashMap<String,ArrayList<String>> ClassFieldsTypeInit = new HashMap<String,ArrayList<String>>(); INBAL REMOVED
    //private HashMap<String,HashMap<String,ArrayList<String>>> ClassFieldsTypeInit = new HashMap<String,HashMap<String,ArrayList<String>>>(); //INBAL ADDED
    //key is className, value is the class's fieldsTypeInitialized
    private HashMap<String,HashMap<String,ArrayList<String>>> ClassMethods = new HashMap<String,HashMap<String,ArrayList<String>>>();

    @XmlElement(required = true)
    private MainClass mainclass;

    @XmlElementWrapper(name="classdecls", required = true)
    @XmlElement(name="classdecl")
    private List<ClassDecl> classdecls;

    // for deserialization only!
    public Program() {
        super(); //INBAL: why do we call super()?
    }

    public Program(MainClass mainclass, List<ClassDecl> classdecls) {
        super();//INBAL: why do we call super()?
        this.mainclass = mainclass;
        this.classdecls = classdecls;
    }

    //INBAL ADDED
    public HashMap<String,String> getClassFields(String className){
        return this.ClassFields.get(className);
    }

    public void addClassFields(String className, HashMap<String,String> classFields){
        this.ClassFields.put(className,classFields);
    }

    //END

    public HashSet<String> getClassAncestors(String className){
        return this.ClassAncestors.get(className);
    }

    public void addClassAncestors(String className,HashSet<String> classAncestors){
        this.ClassAncestors.put(className,classAncestors);
    }

    public HashMap<String,ArrayList<String>> getClassMethods(String className){//INBAL RENAMED
        return this.ClassMethods.get(className);
    }

    public ArrayList<String> getClassMethodByName(String className,String methodName){
        if (!this.ClassMethods.get(className).containsKey(methodName)){
            return null;
        }
        return this.ClassMethods.get(className).get(methodName);
    }

    /*
    public HashMap<String,ArrayList<String>> classFieldsTypeInit(String className){//INBAL CHANGED
        return this.ClassFieldsTypeInit.get(className);
    }

    public void addClassFieldsTypeInit(String className,HashMap<String,ArrayList<String>> classFieldsTypeInit){//INBAL ADDED
        ClassFieldsTypeInit.put(className,classFieldsTypeInit);
    }*/

    public void addClassMethods(String className,HashMap<String,ArrayList<String>> classMethods){//INBAL ADDED
        ClassMethods.put(className,classMethods);
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }

    public MainClass mainClass() {
        return mainclass;
    }

    public List<ClassDecl> classDecls() {
        return classdecls;
    }

    public void addClass(String className, SymbolTable classST){
        STclasses.put(className,classST);
    }

    public SymbolTable getClassST(String className){
        return STclasses.get(className);
    }

    public void addClassVTFields(String className, VTFields classVT){
        VTClassFields.put(className,classVT);
    }

    public VTFields getClassVTFields(String className){
        return VTClassFields.get(className);
    }
}
